package com.ts.download.service.impl;

import com.ts.download.constant.TaskTypeConstants;
import com.ts.download.dao.ClickHouseTaskRecordDao;
import com.ts.download.domain.dto.DownloadReqDTO;
import com.ts.download.domain.dto.MergeDownloadReqDTO;
import com.ts.download.domain.entity.TsWsTaskRecord;
import com.ts.download.domain.vo.QueryResultVO;
import com.ts.download.excel.*;
import com.ts.download.service.DownloadService;
import com.ts.download.util.CosUtil;
import com.ts.download.util.DateUtils;
import com.ts.download.util.ExcelUtil;
import com.ts.download.util.MockHttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.streaming.SXSSFSheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.*;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * 下载服务实现类
 * 
 * @author TS Team
 */
@Slf4j
@Service
public class DownloadServiceImpl implements DownloadService {

    @Autowired
    private ClickHouseTaskRecordDao clickHouseTaskRecordDao;

    @Autowired
    private CosUtil cosUtil;

    @Override
    public String generateDownloadUrl(DownloadReqDTO reqDTO) throws Exception {
        long totalStartTime = System.currentTimeMillis();
        String downloadType = reqDTO.getDownloadType();
        String taskType = reqDTO.getTaskType();
        String countryCode = reqDTO.getCountryCode();
        Integer limit = reqDTO.getLimit();

        log.info("=== 开始生成下载URL ===，downloadType={}, taskType={}, countryCode={}, limit={}",
                downloadType, taskType, countryCode, limit);

        // 1. 从 ClickHouse 获取数据
        long dataFetchStart = System.currentTimeMillis();
        List<TsWsTaskRecord> taskRecordList = clickHouseTaskRecordDao.selectTaskRecordList(taskType, countryCode, limit);
        log.info("数据获取耗时：{}ms, 原始记录数：{}", System.currentTimeMillis() - dataFetchStart, 
                taskRecordList != null ? taskRecordList.size() : 0);

        if (taskRecordList == null || taskRecordList.isEmpty()) {
            log.warn("未找到任务记录，taskType={}, countryCode={}", taskType, countryCode);
            throw new RuntimeException("未找到任务记录");
        }

        // 2. 应用层去重：保留每个phone的第一条记录（已按phone和create_time DESC排序）
        Map<String, TsWsTaskRecord> uniqueRecords = new LinkedHashMap<>();
        for (TsWsTaskRecord record : taskRecordList) {
            String phone = record.getPhone();
            if (phone != null && !uniqueRecords.containsKey(phone)) {
                uniqueRecords.put(phone, record);
            }
        }
        taskRecordList = new ArrayList<>(uniqueRecords.values());
        log.info("去重后记录数：{}", taskRecordList.size());
        
        // 调试：打印前3条记录的phone和userName
        if (!taskRecordList.isEmpty()) {
            for (int i = 0; i < Math.min(3, taskRecordList.size()); i++) {
                TsWsTaskRecord r = taskRecordList.get(i);
                log.info("记录{}：phone={}, userName={}", i, r.getPhone(), r.getUserName());
            }
        }

        // 2. 处理数据：设置 lastOnlineTimeStr
        taskRecordList.forEach(vo -> {
            Date lastOnlineTime = vo.getLastOnlineTime();
            if (lastOnlineTime != null) {
                vo.setLastOnlineTimeStr(DateUtils.parseDateToStr(DateUtils.YYYY_MM_DD_HH_MM_SS, lastOnlineTime));
            } else {
                vo.setLastOnlineTimeStr(vo.getStatus());
            }
        });

        // 3. 生成文件并上传到 COS
        String downloadUrl;
        if ("txt".equals(downloadType)) {
            downloadUrl = generateTxtFile(taskRecordList, taskType, countryCode);
        } else {
            downloadUrl = generateExcelFile(taskRecordList, taskType, countryCode);
        }

        log.info("=== 下载URL生成完成 ===，总耗时：{}ms, URL={}", 
                System.currentTimeMillis() - totalStartTime, downloadUrl);

        return downloadUrl;
    }

    /**
     * 生成TXT文件
     */
    private String generateTxtFile(List<TsWsTaskRecord> taskRecordList, String taskType, String countryCode) throws Exception {
        long txtGenerateStart = System.currentTimeMillis();

        // 生成文件名
        long timestamp = System.currentTimeMillis();
        String randomId = UUID.randomUUID().toString().substring(0, 8);
        String fileName = timestamp + "_" + randomId + "_" + taskType + "_" + countryCode + ".txt";

        // 生成日期目录
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd");
        String dateDir = dateFormat.format(new Date());

        log.info("生成TXT文件：{}", fileName);

        // 创建临时文件
        File tempFile = new File(System.getProperty("java.io.tmpdir"), fileName);

        try {
            // 生成TXT内容并写入文件
            byte[] data = IOUtils.toByteArray(generateTextResource(taskRecordList));
            try (FileOutputStream fos = new FileOutputStream(tempFile);
                 BufferedOutputStream bos = new BufferedOutputStream(fos, 8192)) {
                bos.write(data);
            }

            log.info("TXT文件生成耗时：{}ms, 文件大小：{}KB",
                    System.currentTimeMillis() - txtGenerateStart, tempFile.length() / 1024);

            // 上传文件到COS
            long uploadStart = System.currentTimeMillis();
            String downloadUrl = cosUtil.uploadToS3(tempFile, "download/" + dateDir);
            log.info("TXT文件上传到COS耗时：{}ms", System.currentTimeMillis() - uploadStart);

            return downloadUrl;

        } finally {
            // 清理临时文件
            if (tempFile.exists()) {
                boolean deleted = tempFile.delete();
                log.info("临时TXT文件清理：{}", deleted ? "成功" : "失败");
            }
        }
    }

    /**
     * 生成Excel文件
     */
    private String generateExcelFile(List<TsWsTaskRecord> taskRecordList, String taskType, String countryCode) throws Exception {
        long excelExportStart = System.currentTimeMillis();

        // 生成文件名
        long timestamp = System.currentTimeMillis();
        String randomId = UUID.randomUUID().toString().substring(0, 8);
        String fileName = timestamp + "_" + randomId + "_" + taskType + "_" + countryCode + ".xlsx";

        // 生成日期目录
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd");
        String dateDir = dateFormat.format(new Date());

        log.info("生成Excel文件：{}", fileName);

        // 创建临时文件
        File tempFile = new File(System.getProperty("java.io.tmpdir"), fileName);

        try {
            // 创建临时响应对象，将输出重定向到文件
            FileOutputStream fileOutputStream = new FileOutputStream(tempFile);
            BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(fileOutputStream, 8192);
            MockHttpServletResponse mockResponse = new MockHttpServletResponse();
            mockResponse.setOutputStream(bufferedOutputStream);

            String sheetName = taskType + "_" + countryCode;

            // 根据任务类型使用对应的VO类生成Excel
            generateExcelByTaskType(taskRecordList, taskType, sheetName, fileName, mockResponse);

            // 关闭文件流
            bufferedOutputStream.close();
            fileOutputStream.close();

            log.info("Excel导出耗时：{}ms, 导出记录数：{}, 文件大小：{}KB",
                    System.currentTimeMillis() - excelExportStart, taskRecordList.size(), tempFile.length() / 1024);

            // 上传文件到COS
            long uploadStart = System.currentTimeMillis();
            String downloadUrl = cosUtil.uploadToS3(tempFile, "download/" + dateDir);
            log.info("Excel文件上传到COS耗时：{}ms", System.currentTimeMillis() - uploadStart);

            return downloadUrl;

        } finally {
            // 清理临时文件
            if (tempFile.exists()) {
                boolean deleted = tempFile.delete();
                log.info("临时Excel文件清理：{}", deleted ? "成功" : "失败");
            }
        }
    }

    @Override
    public Object queryFileInfo(DownloadReqDTO reqDTO) throws Exception {
        long queryStartTime = System.currentTimeMillis();
        String taskType = reqDTO.getTaskType();
        String countryCode = reqDTO.getCountryCode();

        log.info("=== 开始查询文件信息 ===，taskType={}, countryCode={}",
                taskType, countryCode);

        // 查询总记录数
        String tableName = clickHouseTaskRecordDao.getTableName(taskType, countryCode);
        Long totalCount = clickHouseTaskRecordDao.countRecords(tableName);
        
        if (totalCount == null || totalCount == 0) {
            log.warn("未找到任务记录，taskType={}, countryCode={}", taskType, countryCode);
            throw new RuntimeException("未找到任务记录");
        }

        // 构建查询结果
        QueryResultVO result = new QueryResultVO();
        result.setTaskType(taskType);
        result.setCountryCode(countryCode);
        result.setTotalCount(totalCount);
        result.setValidCount(totalCount);

        log.info("=== 查询文件信息完成 ===，耗时：{}ms, 总记录数：{}", 
                System.currentTimeMillis() - queryStartTime, result.getTotalCount());

        return result;
    }

    /**
     * 估算文件大小
     */
    private long calculateEstimatedFileSize(List<TsWsTaskRecord> taskRecordList, String taskType) {
        if (taskRecordList == null || taskRecordList.isEmpty()) {
            return 0L;
        }

        int recordCount = taskRecordList.size();
        int avgBytesPerRecord;

        // 根据任务类型估算每条记录的平均字节数
        if ("wsValid".equals(taskType)) {
            // phone + lastOnlineTime + activeDay ≈ 50字节
            avgBytesPerRecord = 50;
        } else if ("gender".equals(taskType)) {
            // 包含更多字段 ≈ 200字节
            avgBytesPerRecord = 200;
        } else if ("whatsappExist".equals(taskType) || "wsExist".equals(taskType)) {
            // phone + status ≈ 30字节
            avgBytesPerRecord = 30;
        } else if ("sieveLive".equals(taskType) || "sieveAvatar".equals(taskType)) {
            // TG相关字段较多 ≈ 150字节
            avgBytesPerRecord = 150;
        } else if (taskType.endsWith("Carrier")) {
            // 运营商相关字段 ≈ 100字节
            avgBytesPerRecord = 100;
        } else {
            // 默认只有phone ≈ 20字节
            avgBytesPerRecord = 20;
        }

        // 转换为KB并添加一些缓冲
        return (long) Math.ceil((recordCount * avgBytesPerRecord * 1.2) / 1024.0);
    }

    /**
     * 生成文本资源（异步写入）
     */
    private InputStream generateTextResource(List<TsWsTaskRecord> dataList) throws IOException {
        PipedInputStream in = new PipedInputStream();
        PipedOutputStream out = new PipedOutputStream(in);
        CompletableFuture.runAsync(() -> {
            try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(out, StandardCharsets.UTF_8))) {
                for (TsWsTaskRecord taskRecord : dataList) {
                    writer.write(taskRecord.getPhone());
                    writer.newLine();
                }
            } catch (IOException e) {
                throw new RuntimeException("写入失败", e);
            }
        });
        return in;
    }

    /**
     * 根据任务类型使用对应的VO类生成Excel
     */
    private void generateExcelByTaskType(List<TsWsTaskRecord> taskRecordList, String taskType, 
                                         String sheetName, String fileName, MockHttpServletResponse response) {
        switch (taskType) {
            case "gender":
                exportWithVO(taskRecordList, TsWsTaskRecordWSGender.class, sheetName, fileName, response);
                break;
            case "whatsappExist":
            case "wsExist":
                exportWithVO(taskRecordList, TsWsTaskRecordWSExists.class, sheetName, fileName, response);
                break;
            case "rcsValid":
                exportWithVO(taskRecordList, TsWsTaskRecordRcs.class, sheetName, fileName, response);
                break;
            case "sieveLive":
            case "sieveAvatar":
            case "tgEffective":
                exportWithVO(taskRecordList, TsWsTaskRecordTG.class, sheetName, fileName, response);
                break;
            case "line":
            case "line_gender":
                exportWithVO(taskRecordList, TsLineTaskRecord.class, sheetName, fileName, response);
                break;
            case "viber":
            case "viber_active":
                exportWithVO(taskRecordList, TsWsTaskRecordViber.class, sheetName, fileName, response);
                break;
            case "usaCarrier":
                exportWithVO(taskRecordList, TsWsTaskRecordUsaCarrier.class, sheetName, fileName, response);
                break;
            case "vnCarrier":
                exportWithVO(taskRecordList, TsWsTaskRecordVnCarrier.class, sheetName, fileName, response);
                break;
            case "deCarrier":
                exportWithVO(taskRecordList, TsWsTaskRecordDeCarrier.class, sheetName, fileName, response);
                break;
            case "ruCarrier":
                exportWithVO(taskRecordList, TsWsTaskRecordRuCarrier.class, sheetName, fileName, response);
                break;
            case "frCarrier":
                exportWithVO(taskRecordList, TsWsTaskRecordFrCarrier.class, sheetName, fileName, response);
                break;
            case "pkCarrier":
                exportWithVO(taskRecordList, TsWsTaskRecordPkCarrier.class, sheetName, fileName, response);
                break;
            case "brCarrier":
                exportWithVO(taskRecordList, TsWsTaskRecordBrCarrier.class, sheetName, fileName, response);
                break;
            case "jpCarrier":
                exportWithVO(taskRecordList, TsWsTaskRecordJpCarrier.class, sheetName, fileName, response);
                break;
            case "gbCarrier":
                exportWithVO(taskRecordList, TsWsTaskRecordGbCarrier.class, sheetName, fileName, response);
                break;
            case "bdCarrier":
                exportWithVO(taskRecordList, TsWsTaskRecordBdCarrier.class, sheetName, fileName, response);
                break;
            case "trCarrier":
                exportWithVO(taskRecordList, TsWsTaskRecordTrCarrier.class, sheetName, fileName, response);
                break;
            case "idCarrier":
                exportWithVO(taskRecordList, TsWsTaskRecordIdCarrier.class, sheetName, fileName, response);
                break;
            case "maCarrier":
                exportWithVO(taskRecordList, TsWsTaskRecordMaCarrier.class, sheetName, fileName, response);
                break;
            case "mxCarrier":
                exportWithVO(taskRecordList, TsWsTaskRecordMxCarrier.class, sheetName, fileName, response);
                break;
            case "nlCarrier":
                exportWithVO(taskRecordList, TsWsTaskRecordNlCarrier.class, sheetName, fileName, response);
                break;
            case "ptCarrier":
                exportWithVO(taskRecordList, TsWsTaskRecordPtCarrier.class, sheetName, fileName, response);
                break;
            case "esCarrier":
                exportWithVO(taskRecordList, TsWsTaskRecordEsCarrier.class, sheetName, fileName, response);
                break;
            case "uaCarrier":
                exportWithVO(taskRecordList, TsWsTaskRecordUaCarrier.class, sheetName, fileName, response);
                break;
            case "uzCarrier":
                exportWithVO(taskRecordList, TsWsTaskRecordUzCarrier.class, sheetName, fileName, response);
                break;
            case "kzCarrier":
                exportWithVO(taskRecordList, TsWsTaskRecordKzCarrier.class, sheetName, fileName, response);
                break;
            case "globalOperators":
                exportWithVO(taskRecordList, TsWsTaskRecordGlobalOperators.class, sheetName, fileName, response);
                break;
            default:
                // 默认使用TsWsTaskRecordExists（只有phone字段）
                exportWithVO(taskRecordList, TsWsTaskRecordExists.class, sheetName, fileName, response);
                break;
        }
    }

    /**
     * 使用指定的VO类导出Excel
     */
    private <V> void exportWithVO(List<TsWsTaskRecord> sourceList, Class<V> voClass, 
                                   String sheetName, String fileName, MockHttpServletResponse response) {
        try {
            // 转换数据：TsWsTaskRecord -> VO
            List<V> voList = new ArrayList<>();
            for (TsWsTaskRecord source : sourceList) {
                V vo = voClass.getDeclaredConstructor().newInstance();
                BeanUtils.copyProperties(source, vo);
                voList.add(vo);
            }
            
            // 使用VO类导出Excel
            ExcelUtil<V> util = new ExcelUtil<>(voClass);
            util.exportExcel(response, voList, sheetName, fileName);
            
        } catch (Exception e) {
            log.error("导出Excel失败，VO类：{}", voClass.getName(), e);
            throw new RuntimeException("导出Excel失败", e);
        }
    }

    @Override
    public String generateMergeDownloadUrl(MergeDownloadReqDTO reqDTO) throws Exception {
        long totalStartTime = System.currentTimeMillis();
        String firstTaskType = reqDTO.getFirstTaskType() != null ? reqDTO.getFirstTaskType() : "gender";
        String secondTaskType = reqDTO.getSecondTaskType();
        String countryCode = reqDTO.getCountryCode();
        Integer limit = reqDTO.getLimit();

        log.info("=== 开始生成合并下载URL ===，firstTaskType={}, secondTaskType={}, countryCode={}, limit={}",
                firstTaskType, secondTaskType, countryCode, limit);

        // 转换性别参数（某些任务类型如sieveAvatar、tgEffective等，sex字段存储的是中文）
        Integer sexParam = convertSexParam(firstTaskType, reqDTO.getSex());
        log.info("性别参数转换：原始={}, 转换后={}", reqDTO.getSex(), sexParam);

        // 检查是否需要合并第二个任务类型
        boolean needMerge = secondTaskType != null && !secondTaskType.trim().isEmpty();
        log.info("是否需要合并第二个任务类型：{}", needMerge);

        // 1. 先统计符合条件的总记录数
        Long totalCount = clickHouseTaskRecordDao.countRecordsWithConditions(
                firstTaskType, countryCode, reqDTO.getMinAge(), reqDTO.getMaxAge(), 
                sexParam, reqDTO.getExcludeSkin(), reqDTO.getCheckUserNameEmpty());
        log.info("第一个任务类型符合条件的总记录数：{}", totalCount);

        if (totalCount == null || totalCount == 0) {
            log.warn("未找到第一个任务类型记录，taskType={}, countryCode={}", firstTaskType, countryCode);
            throw new RuntimeException("未找到符合条件的记录");
        }

        // 2. 分批查询（如果需要合并则立即匹配第二个任务类型）
        int targetLimit = (limit != null && limit > 0) ? limit : 10000; // 目标导出数量
        int batchSize = 5000; // 每批查询5000条
        String lastCreateTime = null; // 上一批最后一个create_time，用于分页
        List<Map<String, Object>> mergedRecords = new ArrayList<>();
        
        // 获取第一个和第二个任务类型对应的VO类
        Class<?> firstVoClass = getVoClassByTaskType(firstTaskType);
        Class<?> secondVoClass = getVoClassByTaskType(secondTaskType);
        
        log.info("开始分批查询并匹配，目标数量：{}，批次大小：{}，总记录数：{}", targetLimit, batchSize, totalCount);
        
        int batchCount = 0;
        int maxBatches = (int) Math.ceil((double) totalCount / batchSize) + 1; // 最大批次数，避免死循环
        
        while (mergedRecords.size() < targetLimit && batchCount < maxBatches) {
            batchCount++;
            long batchStart = System.currentTimeMillis();
            
            // 查询第一个任务类型的一批数据
            List<TsWsTaskRecord> firstBatchRecords = clickHouseTaskRecordDao.selectTaskRecordListWithConditionsByTime(
                    firstTaskType, countryCode, reqDTO.getMinAge(), reqDTO.getMaxAge(), 
                    sexParam, reqDTO.getExcludeSkin(), reqDTO.getCheckUserNameEmpty(), lastCreateTime, batchSize);
            
            log.info("第{}批第一个任务类型查询完成，耗时：{}ms, 查询到：{}条", 
                    batchCount, System.currentTimeMillis() - batchStart, firstBatchRecords.size());
            
            if (firstBatchRecords == null || firstBatchRecords.isEmpty()) {
                log.info("第{}批查询结果为空，停止查询", batchCount);
                break;
            }
            
            // 去重：保留每个phone的第一条记录
            Map<String, TsWsTaskRecord> firstBatchMap = new LinkedHashMap<>();
            for (TsWsTaskRecord record : firstBatchRecords) {
                String phone = record.getPhone();
                if (phone != null && !firstBatchMap.containsKey(phone)) {
                    firstBatchMap.put(phone, record);
                }
            }
            log.info("第{}批去重后：{}条", batchCount, firstBatchMap.size());
            
            int batchMergedCount = 0;
            boolean reachedTarget = false;
            
            if (needMerge) {
                // 需要合并：查询第二个任务类型（使用这批phone）
                List<String> batchPhones = new ArrayList<>(firstBatchMap.keySet());
                Map<String, TsWsTaskRecord> secondBatchMap = new LinkedHashMap<>();
                
                // 分批查询第二个任务类型（每批1000个phone）
                int secondBatchSize = 1000;
                for (int i = 0; i < batchPhones.size(); i += secondBatchSize) {
                    int end = Math.min(i + secondBatchSize, batchPhones.size());
                    List<String> subPhones = batchPhones.subList(i, end);
                    
                    List<TsWsTaskRecord> secondRecords = clickHouseTaskRecordDao.selectTaskRecordListByPhones(
                            secondTaskType, countryCode, subPhones);
                    
                    for (TsWsTaskRecord record : secondRecords) {
                        String phone = record.getPhone();
                        if (phone != null && !secondBatchMap.containsKey(phone)) {
                            secondBatchMap.put(phone, record);
                        }
                    }
                }
                log.info("第{}批第二个任务类型匹配到：{}条", batchCount, secondBatchMap.size());
                
                // 合并数据：只保留第二个任务类型也存在的记录
                for (Map.Entry<String, TsWsTaskRecord> entry : firstBatchMap.entrySet()) {
                    String phone = entry.getKey();
                    TsWsTaskRecord firstRecord = entry.getValue();
                    TsWsTaskRecord secondRecord = secondBatchMap.get(phone);
                    
                    if (secondRecord != null) {
                        // 创建合并记录
                        Map<String, Object> mergedRecord = new LinkedHashMap<>();
                        addRecordFieldsByVo(mergedRecord, firstRecord, firstVoClass, "");
                        addRecordFieldsByVo(mergedRecord, secondRecord, secondVoClass, "副-");
                        mergedRecords.add(mergedRecord);
                        batchMergedCount++;
                        
                        if (mergedRecords.size() >= targetLimit) {
                            reachedTarget = true;
                            break;
                        }
                    }
                }
            } else {
                // 不需要合并：直接添加第一个任务类型的数据
                for (Map.Entry<String, TsWsTaskRecord> entry : firstBatchMap.entrySet()) {
                    TsWsTaskRecord firstRecord = entry.getValue();
                    
                    // 创建记录
                    Map<String, Object> mergedRecord = new LinkedHashMap<>();
                    addRecordFieldsByVo(mergedRecord, firstRecord, firstVoClass, "");
                    mergedRecords.add(mergedRecord);
                    batchMergedCount++;
                    
                    if (mergedRecords.size() >= targetLimit) {
                        reachedTarget = true;
                        break;
                    }
                }
            }
            log.info("第{}批处理成功：{}条，累计：{}/{}", batchCount, batchMergedCount, mergedRecords.size(), targetLimit);
            
            // 如果达到目标数量，停止查询
            if (reachedTarget) {
                log.info("已达到目标数量{}，停止查询", targetLimit);
                break;
            }
            
            // 记录本批最后一个create_time，用于下一批查询
            if (!firstBatchRecords.isEmpty()) {
                Date lastTime = firstBatchRecords.get(firstBatchRecords.size() - 1).getCreateTime();
                if (lastTime != null) {
                    lastCreateTime = DateUtils.parseDateToStr(DateUtils.YYYY_MM_DD_HH_MM_SS, lastTime);
                }
            }
        }
        
        log.info("分批查询完成，共{}批，最终合并记录数：{}", batchCount, mergedRecords.size());

        if (mergedRecords.isEmpty()) {
            throw new RuntimeException("没有找到phone匹配的记录");
        }

        // 3. 生成Excel文件并上传
        String fileName = System.currentTimeMillis() + "_" + UUID.randomUUID().toString().substring(0, 8) 
                + "_merge_" + firstTaskType + "_" + secondTaskType + "_" + countryCode + ".xlsx";
        String downloadUrl = generateMergeExcelFileFromMap(mergedRecords, firstTaskType, secondTaskType, countryCode, fileName);

        log.info("=== 合并下载URL生成完成 ===，总耗时：{}ms, URL={}", 
                System.currentTimeMillis() - totalStartTime, downloadUrl);

        return downloadUrl;
    }

    /**
     * 转换性别参数（某些任务类型的sex字段存储的是中文）
     * @param taskType 任务类型
     * @param sex 前端传来的性别参数（0=女, 1=男）
     * @return 转换后的性别参数（对于TG类任务返回null，让DAO层处理；其他任务返回原值）
     */
    private Integer convertSexParam(String taskType, Integer sex) {
        // TG类任务（sieveAvatar、tgEffective等）的sex字段存储的是中文"男"、"女"
        // 这些任务类型需要在DAO层进行特殊处理
        if (taskType != null && (taskType.equals("sieveAvatar") || taskType.equals("tgEffective") || taskType.equals("sieveLive"))) {
            // 返回原值，让DAO层根据任务类型判断是否需要转换为中文
            return sex;
        }
        return sex;
    }

    /**
     * 根据任务类型获取对应的VO类
     */
    private Class<?> getVoClassByTaskType(String taskType) {
        switch (taskType) {
            case "gender":
                return TsWsTaskRecordWSGender.class;
            case "whatsappExist":
            case "wsExist":
                return TsWsTaskRecordWSExists.class;
            case "rcsValid":
                return TsWsTaskRecordRcs.class;
            case "sieveLive":
            case "sieveAvatar":
            case "tgEffective":
                return TsWsTaskRecordTG.class;
            case "line":
            case "line_gender":
                return TsLineTaskRecord.class;
            case "viber":
            case "viber_active":
                return TsWsTaskRecordViber.class;
            case "usaCarrier":
                return TsWsTaskRecordUsaCarrier.class;
            case "vnCarrier":
                return TsWsTaskRecordVnCarrier.class;
            case "deCarrier":
                return TsWsTaskRecordDeCarrier.class;
            case "ruCarrier":
                return TsWsTaskRecordRuCarrier.class;
            case "frCarrier":
                return TsWsTaskRecordFrCarrier.class;
            case "pkCarrier":
                return TsWsTaskRecordPkCarrier.class;
            case "brCarrier":
                return TsWsTaskRecordBrCarrier.class;
            case "jpCarrier":
                return TsWsTaskRecordJpCarrier.class;
            case "gbCarrier":
                return TsWsTaskRecordGbCarrier.class;
            case "bdCarrier":
                return TsWsTaskRecordBdCarrier.class;
            case "trCarrier":
                return TsWsTaskRecordTrCarrier.class;
            case "idCarrier":
                return TsWsTaskRecordIdCarrier.class;
            case "maCarrier":
                return TsWsTaskRecordMaCarrier.class;
            case "mxCarrier":
                return TsWsTaskRecordMxCarrier.class;
            case "nlCarrier":
                return TsWsTaskRecordNlCarrier.class;
            case "ptCarrier":
                return TsWsTaskRecordPtCarrier.class;
            case "esCarrier":
                return TsWsTaskRecordEsCarrier.class;
            case "uaCarrier":
                return TsWsTaskRecordUaCarrier.class;
            case "uzCarrier":
                return TsWsTaskRecordUzCarrier.class;
            case "kzCarrier":
                return TsWsTaskRecordKzCarrier.class;
            case "globalOperators":
                return TsWsTaskRecordGlobalOperators.class;
            default:
                return TsWsTaskRecordExists.class;
        }
    }

    /**
     * 根据VO类的@Excel注解添加字段到Map中
     */
    private void addRecordFieldsByVo(Map<String, Object> map, TsWsTaskRecord record, Class<?> voClass, String prefix) {
        try {
            // 获取VO类中所有带@Excel注解的字段
            Field[] fields = voClass.getDeclaredFields();
            for (Field field : fields) {
                com.ts.download.annotation.Excel excelAnnotation = field.getAnnotation(com.ts.download.annotation.Excel.class);
                if (excelAnnotation != null) {
                    field.setAccessible(true);
                    String fieldName = field.getName();
                    String excelName = excelAnnotation.name();
                    String readConverterExp = excelAnnotation.readConverterExp();
                    
                    // 从TsWsTaskRecord中获取对应字段的值
                    Object value = getFieldValueFromRecord(record, fieldName);
                    
                    if (value != null) {
                        // 如果有数据转换表达式，进行转换
                        if (readConverterExp != null && !readConverterExp.isEmpty()) {
                            value = convertByExp(value.toString(), readConverterExp);
                        }
                        
                        // 使用Excel注解中的name作为列名
                        map.put(prefix + excelName, value);
                    }
                }
            }
        } catch (Exception e) {
            log.error("添加字段失败", e);
        }
    }
    
    /**
     * 数据转换（如：0=男,1=女,2=未知）
     */
    private String convertByExp(String value, String converterExp) {
        try {
            String[] convertSource = converterExp.split(",");
            for (String item : convertSource) {
                String[] itemArray = item.split("=");
                if (itemArray.length == 2 && itemArray[0].trim().equals(value.trim())) {
                    return itemArray[1];
                }
            }
        } catch (Exception e) {
            log.error("数据转换失败", e);
        }
        return value;
    }

    /**
     * 从TsWsTaskRecord中获取指定字段的值
     */
    private Object getFieldValueFromRecord(TsWsTaskRecord record, String fieldName) {
        try {
            Field field = TsWsTaskRecord.class.getDeclaredField(fieldName);
            field.setAccessible(true);
            Object value = field.get(record);
            
            // 如果是Date类型，转换为字符串
            if (value instanceof Date) {
                return DateUtils.parseDateToStr(DateUtils.YYYY_MM_DD_HH_MM_SS, (Date) value);
            }
            
            return value;
        } catch (NoSuchFieldException e) {
            // 字段不存在，返回null
            return null;
        } catch (Exception e) {
            log.error("获取字段值失败: {}", fieldName, e);
            return null;
        }
    }

    /**
     * 将记录的所有字段添加到Map中（旧方法，保留）
     */
    private void addRecordFields(Map<String, Object> map, TsWsTaskRecord record, String prefix) {
        if (record.getPhone() != null) {
            map.put(prefix + "phone", record.getPhone());
        }
        if (record.getUid() != null) {
            map.put(prefix + "uid", record.getUid());
        }
        if (record.getUserName() != null) {
            map.put(prefix + "userName", record.getUserName());
        }
        if (record.getPic() != null) {
            map.put(prefix + "pic", record.getPic());
        }
        if (record.getMember() != null) {
            map.put(prefix + "member", record.getMember());
        }
        if (record.getLastOnlineTime() != null) {
            map.put(prefix + "lastOnlineTime", DateUtils.parseDateToStr(DateUtils.YYYY_MM_DD_HH_MM_SS, record.getLastOnlineTime()));
        }
        if (record.getActiveDay() != null) {
            map.put(prefix + "activeDay", record.getActiveDay());
        }
        if (record.getStatus() != null) {
            map.put(prefix + "status", record.getStatus());
        }
        if (record.getSex() != null) {
            map.put(prefix + "sex", record.getSex());
        }
        if (record.getAge() != null) {
            map.put(prefix + "age", record.getAge());
        }
        if (record.getBusinessNumber() != null) {
            map.put(prefix + "businessNumber", record.getBusinessNumber());
        }
        if (record.getEthnicity() != null) {
            map.put(prefix + "ethnicity", record.getEthnicity());
        }
        if (record.getFirstName() != null) {
            map.put(prefix + "firstName", record.getFirstName());
        }
        if (record.getLastName() != null) {
            map.put(prefix + "lastName", record.getLastName());
        }
        if (record.getCountry() != null) {
            map.put(prefix + "country", record.getCountry());
        }
        if (record.getRegion() != null) {
            map.put(prefix + "region", record.getRegion());
        }
        if (record.getHairColor() != null) {
            map.put(prefix + "hairColor", record.getHairColor());
        }
        if (record.getSkin() != null) {
            map.put(prefix + "skin", record.getSkin());
        }
        if (record.getMultipleAvatars() != null) {
            map.put(prefix + "multipleAvatars", record.getMultipleAvatars());
        }
    }

    /**
     * 合并两条记录的数据
     */
    private void mergeRecords(TsWsTaskRecord first, TsWsTaskRecord second) {
        // 如果第一个记录的字段为空，用第二个记录的字段填充
        if (first.getUid() == null && second.getUid() != null) {
            first.setUid(second.getUid());
        }
        if (first.getUserName() == null && second.getUserName() != null) {
            first.setUserName(second.getUserName());
        }
        if (first.getPic() == null && second.getPic() != null) {
            first.setPic(second.getPic());
        }
        if (first.getStatus() == null && second.getStatus() != null) {
            first.setStatus(second.getStatus());
        }
        if (first.getMember() == null && second.getMember() != null) {
            first.setMember(second.getMember());
        }
        if (first.getLastOnlineTime() == null && second.getLastOnlineTime() != null) {
            first.setLastOnlineTime(second.getLastOnlineTime());
        }
        if (first.getActiveDay() == null && second.getActiveDay() != null) {
            first.setActiveDay(second.getActiveDay());
        }
    }

    /**
     * 从Map生成合并的Excel文件
     */
    private String generateMergeExcelFileFromMap(List<Map<String, Object>> dataList, String firstTaskType, 
                                                  String secondTaskType, String countryCode, String fileName) throws Exception {
        long excelExportStart = System.currentTimeMillis();
        File tempFile = new File(System.getProperty("java.io.tmpdir"), fileName);
        log.info("生成合并Excel文件：{}", fileName);

        try (FileOutputStream fileOutputStream = new FileOutputStream(tempFile);
             SXSSFWorkbook workbook = new SXSSFWorkbook(1000)) {
            
            SXSSFSheet sheet = (SXSSFSheet) workbook.createSheet("merge_" + firstTaskType + "_" + secondTaskType);
            sheet.trackAllColumnsForAutoSizing();

            // 创建表头样式
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);

            // 创建数据样式
            CellStyle dataStyle = workbook.createCellStyle();
            dataStyle.setAlignment(HorizontalAlignment.LEFT);

            // 获取列名（从第一条记录）
            if (dataList.isEmpty()) {
                throw new RuntimeException("没有数据可导出");
            }
            
            List<String> columnNames = new ArrayList<>(dataList.get(0).keySet());
            
            // 创建表头行
            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < columnNames.size(); i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(columnNames.get(i));
                cell.setCellStyle(headerStyle);
            }

            // 填充数据
            for (int i = 0; i < dataList.size(); i++) {
                Row row = sheet.createRow(i + 1);
                Map<String, Object> dataMap = dataList.get(i);
                
                for (int j = 0; j < columnNames.size(); j++) {
                    Cell cell = row.createCell(j);
                    Object value = dataMap.get(columnNames.get(j));
                    
                    if (value == null) {
                        cell.setCellValue("");
                    } else if (value instanceof Number) {
                        cell.setCellValue(((Number) value).doubleValue());
                    } else {
                        cell.setCellValue(value.toString());
                    }
                    cell.setCellStyle(dataStyle);
                }
            }

            // 自动调整列宽
            for (int i = 0; i < columnNames.size(); i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(fileOutputStream);
            
            log.info("合并Excel导出耗时：{}ms, 导出记录数：{}, 文件大小：{}KB",
                    System.currentTimeMillis() - excelExportStart, dataList.size(), tempFile.length() / 1024);

            // 上传文件到COS
            long uploadStart = System.currentTimeMillis();
            String cosPath = "download";
            String downloadUrl = cosUtil.uploadToS3(tempFile, cosPath);
            log.info("COS上传耗时：{}ms", System.currentTimeMillis() - uploadStart);

            return downloadUrl;
        } finally {
            if (tempFile.exists()) {
                boolean deleted = tempFile.delete();
                log.info("临时Excel文件清理：{}", deleted ? "成功" : "失败");
            }
        }
    }

    /**
     * 生成合并的Excel文件（旧方法，保留以防需要）
     */
    private String generateMergeExcelFile(List<TsWsTaskRecord> taskRecordList, String firstTaskType, 
                                          String secondTaskType, String countryCode, String fileName) throws Exception {
        long excelExportStart = System.currentTimeMillis();
        File tempFile = new File(System.getProperty("java.io.tmpdir"), fileName);
        log.info("生成合并Excel文件：{}", fileName);

        try {
            FileOutputStream fileOutputStream = new FileOutputStream(tempFile);
            BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(fileOutputStream, 8192);
            MockHttpServletResponse mockResponse = new MockHttpServletResponse();
            mockResponse.setOutputStream(bufferedOutputStream);

            String sheetName = "merge_" + firstTaskType + "_" + secondTaskType + "_" + countryCode;

            // 使用第一个任务类型的VO类导出（因为以第一个任务类型为主）
            generateExcelByTaskType(taskRecordList, firstTaskType, sheetName, fileName, mockResponse);

            bufferedOutputStream.close();
            fileOutputStream.close();

            log.info("合并Excel导出耗时：{}ms, 导出记录数：{}, 文件大小：{}KB",
                    System.currentTimeMillis() - excelExportStart, taskRecordList.size(), tempFile.length() / 1024);

            // 上传文件到COS
            long uploadStart = System.currentTimeMillis();
            String cosPath = "download";
            String downloadUrl = cosUtil.uploadToS3(tempFile, cosPath);
            log.info("COS上传耗时：{}ms", System.currentTimeMillis() - uploadStart);

            return downloadUrl;
        } finally {
            if (tempFile.exists()) {
                boolean deleted = tempFile.delete();
                log.info("临时Excel文件清理：{}", deleted ? "成功" : "失败");
            }
        }
    }
}
