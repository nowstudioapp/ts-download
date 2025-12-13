package com.ts.download.service.impl;

import com.ts.download.constant.TaskTypeConstants;
import com.ts.download.dao.ClickHouseTaskRecordDao;
import com.ts.download.domain.dto.DownloadReqDTO;
import com.ts.download.domain.dto.MergeDownloadReqDTO;
import com.ts.download.domain.dto.QueryTaskReqDTO;
import com.ts.download.domain.entity.TsWsTaskRecord;
import com.ts.download.domain.vo.QueryResultVO;
import com.ts.download.excel.*;
import com.ts.download.service.DownloadService;
import com.ts.download.util.CosUtil;
import com.ts.download.util.LocalFileUtil;
import com.ts.download.config.LocalFileProperties;
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

    @Autowired
    private LocalFileUtil localFileUtil;

    @Autowired
    private LocalFileProperties localFileProperties;

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

            // 保存文件（本地存储或COS）
            long uploadStart = System.currentTimeMillis();
            String downloadUrl;
            if (localFileProperties.isEnabled()) {
                downloadUrl = localFileUtil.saveToLocal(tempFile, "download/" + dateDir);
                log.info("TXT文件保存到本地耗时：{}ms", System.currentTimeMillis() - uploadStart);
            } else {
                downloadUrl = cosUtil.uploadToS3(tempFile, "download/" + dateDir);
                log.info("TXT文件上传到COS耗时：{}ms", System.currentTimeMillis() - uploadStart);
            }

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

            // 保存文件（本地存储或COS）
            long uploadStart = System.currentTimeMillis();
            String downloadUrl;
            if (localFileProperties.isEnabled()) {
                downloadUrl = localFileUtil.saveToLocal(tempFile, "download/" + dateDir);
                log.info("Excel文件保存到本地耗时：{}ms", System.currentTimeMillis() - uploadStart);
            } else {
                downloadUrl = cosUtil.uploadToS3(tempFile, "download/" + dateDir);
                log.info("Excel文件上传到COS耗时：{}ms", System.currentTimeMillis() - uploadStart);
            }

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
        Integer skip = reqDTO.getSkip();
        String downloadType = reqDTO.getDownloadType();

        log.info("=== 开始生成合并下载URL ===，firstTaskType={}, secondTaskType={}, countryCode={}, limit={}, skip={}, downloadType={}",
                firstTaskType, secondTaskType, countryCode, limit, skip, downloadType);

        // 转换性别参数（某些任务类型如sieveAvatar、tgEffective等，sex字段存储的是中文）
        Integer sexParam = convertSexParam(firstTaskType, reqDTO.getSex());
        log.info("性别参数转换：原始={}, 转换后={}", reqDTO.getSex(), sexParam);

        // TXT导出：使用流式处理，只查询phone字段，直接写入文件，避免内存溢出
        if ("txt".equalsIgnoreCase(downloadType)) {
            return generatePhoneTxtFileStream(reqDTO, firstTaskType, countryCode, sexParam);
        }

        // Excel导出：保持原有逻辑
        // 检查是否需要合并第二个任务类型
        boolean needMerge = secondTaskType != null && !secondTaskType.trim().isEmpty();
        log.info("是否需要合并第二个任务类型：{}", needMerge);

        // 准备查询参数
        int targetLimit = (limit != null && limit > 0) ? limit : 10000; // Excel默认10000条
        int skipCount = (skip != null && skip > 0) ? skip : 0; // 跳过数量

        // 1. 统计符合条件的总记录数（分批下载时跳过统计以提高性能）
        if (skipCount > 0) {
            log.info("分批下载请求（skip={}），跳过总数统计以提高性能", skipCount);
        } else {
            Long totalCount = clickHouseTaskRecordDao.countRecordsWithConditions(
                    firstTaskType, countryCode, reqDTO.getMinAge(), reqDTO.getMaxAge(), 
                    sexParam, reqDTO.getExcludeSkin(), reqDTO.getCheckUserNameEmpty());
            log.info("第一个任务类型符合条件的总记录数：{}", totalCount);

            if (totalCount == null || totalCount == 0) {
                log.warn("未找到第一个任务类型记录，taskType={}, countryCode={}", firstTaskType, countryCode);
                throw new RuntimeException("未找到符合条件的记录");
            }
        }

        // 2. 查询准备
        
        log.info("开始查询数据，目标数量：{}，跳过数量：{}", targetLimit, skipCount);
        
        // 查询第一个任务类型的数据（自动分批查询）
        List<TsWsTaskRecord> firstTaskRecords = queryDataWithBatch(
                firstTaskType, countryCode, reqDTO.getMinAge(), reqDTO.getMaxAge(), 
                sexParam, reqDTO.getExcludeSkin(), reqDTO.getCheckUserNameEmpty(), skipCount, targetLimit);
        
        log.info("第一个任务类型查询完成，总共查询到：{}条", firstTaskRecords.size());
        
        if (firstTaskRecords == null || firstTaskRecords.isEmpty()) {
            throw new RuntimeException("没有找到符合条件的记录");
        }
        
        // 处理数据：设置 lastOnlineTimeStr
        firstTaskRecords.forEach(vo -> {
            Date lastOnlineTime = vo.getLastOnlineTime();
            if (lastOnlineTime != null) {
                vo.setLastOnlineTimeStr(DateUtils.parseDateToStr(DateUtils.YYYY_MM_DD_HH_MM_SS, lastOnlineTime));
            } else {
                vo.setLastOnlineTimeStr(vo.getStatus());
            }
        });
        
        // 获取第一个和第二个任务类型对应的VO类
        Class<?> firstVoClass = getVoClassByTaskType(firstTaskType);
        Class<?> secondVoClass = needMerge ? getVoClassByTaskType(secondTaskType) : null;
        
        List<Map<String, Object>> mergedRecords = new ArrayList<>();
        
        if (needMerge) {
            // 需要合并：查询第二个任务类型（使用phone列表）
            List<String> phones = firstTaskRecords.stream()
                    .map(TsWsTaskRecord::getPhone)
                    .filter(phone -> phone != null && !phone.trim().isEmpty())
                    .collect(java.util.stream.Collectors.toList());
            
            log.info("准备查询第二个任务类型，phone数量：{}", phones.size());
            
            Map<String, TsWsTaskRecord> secondRecordMap = new LinkedHashMap<>();
            
            // 分批查询第二个任务类型（每批5000个phone，适合ClickHouse大批量处理）
            int batchSize = 5000;
            for (int i = 0; i < phones.size(); i += batchSize) {
                int end = Math.min(i + batchSize, phones.size());
                List<String> subPhones = phones.subList(i, end);
                
                List<TsWsTaskRecord> secondRecords = clickHouseTaskRecordDao.selectTaskRecordListByPhones(
                        secondTaskType, countryCode, subPhones);
                
                for (TsWsTaskRecord record : secondRecords) {
                    String phone = record.getPhone();
                    if (phone != null && !secondRecordMap.containsKey(phone)) {
                        secondRecordMap.put(phone, record);
                    }
                }
            }
            log.info("第二个任务类型匹配到：{}条", secondRecordMap.size());
            
            // 合并数据：只保留第二个任务类型也存在的记录
            for (TsWsTaskRecord firstRecord : firstTaskRecords) {
                String phone = firstRecord.getPhone();
                TsWsTaskRecord secondRecord = secondRecordMap.get(phone);
                
                if (secondRecord != null) {
                    // 创建合并记录
                    Map<String, Object> mergedRecord = new LinkedHashMap<>();
                    addRecordFieldsByVo(mergedRecord, firstRecord, firstVoClass, "");
                    addRecordFieldsByVo(mergedRecord, secondRecord, secondVoClass, "副-");
                    mergedRecords.add(mergedRecord);
                }
            }
            log.info("合并完成，最终记录数：{}", mergedRecords.size());
        } else {
            // 不需要合并：直接使用第一个任务类型的数据
            for (TsWsTaskRecord firstRecord : firstTaskRecords) {
                Map<String, Object> mergedRecord = new LinkedHashMap<>();
                addRecordFieldsByVo(mergedRecord, firstRecord, firstVoClass, "");
                mergedRecords.add(mergedRecord);
            }
            log.info("单任务类型处理完成，记录数：{}", mergedRecords.size());
        }

        if (mergedRecords.isEmpty()) {
            throw new RuntimeException("没有找到匹配的记录");
        }

        // 3. 生成Excel文件
        String fileName = System.currentTimeMillis() + "_" + UUID.randomUUID().toString().substring(0, 8) 
                + "_merge_" + firstTaskType + "_" + secondTaskType + "_" + countryCode + ".xlsx";
        String downloadUrl = generateMergeExcelFileFromMap(mergedRecords, firstTaskType, secondTaskType, countryCode, fileName);

        log.info("=== 合并下载URL生成完成 ===，总耗时：{}ms, URL={}", 
                System.currentTimeMillis() - totalStartTime, downloadUrl);

        return downloadUrl;
    }

    /**
     * 流式生成TXT文件（只含手机号），避免内存溢出
     */
    private String generatePhoneTxtFileStream(MergeDownloadReqDTO reqDTO, String taskType, 
                                               String countryCode, Integer sexParam) throws Exception {
        long startTime = System.currentTimeMillis();
        
        // 生成文件名
        String fileName = System.currentTimeMillis() + "_" + UUID.randomUUID().toString().substring(0, 8) 
                + "_phones_" + taskType + "_" + countryCode + ".txt";
        
        // 生成日期目录
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd");
        String dateDir = dateFormat.format(new Date());
        
        log.info("开始流式生成TXT文件：{}", fileName);
        
        // 创建临时文件
        File tempFile = new File(System.getProperty("java.io.tmpdir"), fileName);
        
        try (BufferedWriter writer = new BufferedWriter(
                new OutputStreamWriter(new FileOutputStream(tempFile), StandardCharsets.UTF_8), 65536)) {
            
            final long[] count = {0};
            
            // 流式查询，边查询边写入
            clickHouseTaskRecordDao.streamPhoneNumbers(
                    taskType, countryCode,
                    reqDTO.getMinAge(), reqDTO.getMaxAge(),
                    sexParam, reqDTO.getExcludeSkin(),
                    reqDTO.getCheckUserNameEmpty(),
                    (phones) -> {
                        try {
                            for (String phone : phones) {
                                writer.write(phone);
                                writer.newLine();
                                count[0]++;
                            }
                            // 每批写入后刷新
                            writer.flush();
                            log.info("已写入：{}条", count[0]);
                        } catch (IOException e) {
                            throw new RuntimeException("写入文件失败", e);
                        }
                    },
                    10000  // 每批10000条
            );
            
            log.info("TXT文件生成完成，总记录数：{}，耗时：{}ms", count[0], System.currentTimeMillis() - startTime);
            
            if (count[0] == 0) {
                throw new RuntimeException("没有找到符合条件的记录");
            }
            
        }
        
        // 保存文件
        long uploadStart = System.currentTimeMillis();
        String downloadUrl;
        try {
            if (localFileProperties.isEnabled()) {
                downloadUrl = localFileUtil.saveToLocal(tempFile, "download/" + dateDir);
                log.info("TXT文件保存到本地耗时：{}ms", System.currentTimeMillis() - uploadStart);
            } else {
                downloadUrl = cosUtil.uploadToS3(tempFile, "download/" + dateDir);
                log.info("TXT文件上传到COS耗时：{}ms", System.currentTimeMillis() - uploadStart);
            }
        } finally {
            // 清理临时文件
            if (tempFile.exists()) {
                boolean deleted = tempFile.delete();
                log.info("临时TXT文件清理：{}", deleted ? "成功" : "失败");
            }
        }
        
        log.info("=== TXT导出完成 ===，总耗时：{}ms, URL={}", System.currentTimeMillis() - startTime, downloadUrl);
        return downloadUrl;
    }

    /**
     * 分批查询数据（使用基于时间的分页，避免大OFFSET导致内存溢出）
     * @param taskType 任务类型
     * @param countryCode 国家代码
     * @param minAge 最小年龄
     * @param maxAge 最大年龄
     * @param sex 性别
     * @param excludeSkin 排除肤色
     * @param checkUserNameEmpty 检查用户名是否为空
     * @param skip 跳过数量（仅第一批有效，后续批次使用时间分页）
     * @param totalLimit 总共需要的数量
     * @return 查询结果列表
     */
    private List<TsWsTaskRecord> queryDataWithBatch(String taskType, String countryCode, 
                                                    Integer minAge, Integer maxAge, Integer sex, 
                                                    Integer excludeSkin, Integer checkUserNameEmpty,
                                                    Integer skip, Integer totalLimit) {
        final int BATCH_SIZE = 10000; // 每批1万条
        List<TsWsTaskRecord> allRecords = new ArrayList<>();
        int remainingLimit = totalLimit;
        int batchNumber = 1;
        String lastCreateTime = null; // 用于时间分页
        
        log.info("开始分批查询（基于时间分页），总目标：{}条，跳过：{}条，批次大小：{}条", totalLimit, skip, BATCH_SIZE);
        
        // 如果需要跳过数据，先执行一次跳过查询
        if (skip != null && skip > 0) {
            log.info("执行跳过查询，跳过：{}条", skip);
            long skipStart = System.currentTimeMillis();
            
            // 获取第skip条记录的时间点作为分页起始点
            int skipOffset = Math.max(0, skip - 1); // 避免负数
            List<TsWsTaskRecord> skipRecords = clickHouseTaskRecordDao.selectTaskRecordListWithConditionsAndSkip(
                    taskType, countryCode, minAge, maxAge, sex, excludeSkin, 
                    checkUserNameEmpty, skipOffset, 1); // 取第skip条记录的时间点
            
            long skipTime = System.currentTimeMillis() - skipStart;
            log.info("跳过查询完成，耗时：{}ms，skipOffset：{}", skipTime, skipOffset);
            
            if (skipRecords != null && !skipRecords.isEmpty()) {
                // 获取跳过位置的时间点
                TsWsTaskRecord lastSkipRecord = skipRecords.get(0);
                if (lastSkipRecord.getCreateTime() != null) {
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                    lastCreateTime = sdf.format(lastSkipRecord.getCreateTime());
                    log.info("获取到跳过位置的时间点：{}", lastCreateTime);
                }
            }
        }
        
        while (remainingLimit > 0) {
            int currentBatchSize = Math.min(BATCH_SIZE, remainingLimit);
            
            log.info("执行第{}批查询，时间点：{}，查询：{}条", batchNumber, lastCreateTime, currentBatchSize);
            long batchStart = System.currentTimeMillis();
            
            // 使用基于时间的分页查询
            List<TsWsTaskRecord> batchRecords = clickHouseTaskRecordDao.selectTaskRecordListWithConditionsByTime(
                    taskType, countryCode, minAge, maxAge, sex, excludeSkin, 
                    checkUserNameEmpty, lastCreateTime, currentBatchSize);
            
            long batchTime = System.currentTimeMillis() - batchStart;
            log.info("第{}批查询完成，耗时：{}ms，获得：{}条记录", batchNumber, batchTime, 
                    batchRecords != null ? batchRecords.size() : 0);
            
            if (batchRecords == null || batchRecords.isEmpty()) {
                log.info("第{}批查询结果为空，停止查询", batchNumber);
                break;
            }
            
            allRecords.addAll(batchRecords);
            
            // 更新时间点为最后一条记录的时间
            TsWsTaskRecord lastRecord = batchRecords.get(batchRecords.size() - 1);
            if (lastRecord.getCreateTime() != null) {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                lastCreateTime = sdf.format(lastRecord.getCreateTime());
            }
            
            // 如果这批数据少于预期，说明没有更多数据了
            if (batchRecords.size() < currentBatchSize) {
                log.info("第{}批数据不足（{}条 < {}条），已到数据末尾", batchNumber, batchRecords.size(), currentBatchSize);
                break;
            }
            
            // 准备下一批
            remainingLimit -= batchRecords.size();
            batchNumber++;
            
            // 防止无限循环
            if (batchNumber > 100) {
                log.warn("分批查询超过100批，强制停止");
                break;
            }
        }
        
        log.info("分批查询完成，共{}批，总计获得：{}条记录", batchNumber - 1, allRecords.size());
        return allRecords;
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

            // 保存文件（本地存储或COS）
            long uploadStart = System.currentTimeMillis();
            String downloadUrl;
            if (localFileProperties.isEnabled()) {
                // 生成日期目录
                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd");
                String dateDir = dateFormat.format(new Date());
                downloadUrl = localFileUtil.saveToLocal(tempFile, "download/" + dateDir);
                log.info("合并Excel文件保存到本地耗时：{}ms", System.currentTimeMillis() - uploadStart);
            } else {
                String cosPath = "download";
                downloadUrl = cosUtil.uploadToS3(tempFile, cosPath);
                log.info("COS上传耗时：{}ms", System.currentTimeMillis() - uploadStart);
            }

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

            // 保存文件（本地存储或COS）
            long uploadStart = System.currentTimeMillis();
            String downloadUrl;
            if (localFileProperties.isEnabled()) {
                // 生成日期目录
                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd");
                String dateDir = dateFormat.format(new Date());
                downloadUrl = localFileUtil.saveToLocal(tempFile, "download/" + dateDir);
                log.info("合并Excel文件保存到本地耗时：{}ms", System.currentTimeMillis() - uploadStart);
            } else {
                String cosPath = "download";
                downloadUrl = cosUtil.uploadToS3(tempFile, cosPath);
                log.info("COS上传耗时：{}ms", System.currentTimeMillis() - uploadStart);
            }

            return downloadUrl;
        } finally {
            if (tempFile.exists()) {
                boolean deleted = tempFile.delete();
                log.info("临时Excel文件清理：{}", deleted ? "成功" : "失败");
            }
        }
    }

    @Override
    public Object queryTaskCount(QueryTaskReqDTO reqDTO) throws Exception {
        long queryStartTime = System.currentTimeMillis();
        String taskType = reqDTO.getTaskType();
        String countryCode = reqDTO.getCountryCode();

        log.info("=== 开始查询任务记录数量 ===，taskType={}, countryCode={}, minAge={}, maxAge={}, sex={}, excludeSkin={}, checkUserNameEmpty={}",
                taskType, countryCode, reqDTO.getMinAge(), reqDTO.getMaxAge(), reqDTO.getSex(), reqDTO.getExcludeSkin(), reqDTO.getCheckUserNameEmpty());

        // 转换性别参数（某些任务类型如sieveAvatar、tgEffective等，sex字段存储的是中文）
        Integer sexParam = convertSexParam(taskType, reqDTO.getSex());
        log.info("性别参数转换：原始={}, 转换后={}", reqDTO.getSex(), sexParam);

        // 查询符合条件的记录数
        Long totalCount = clickHouseTaskRecordDao.countRecordsWithConditions(
                taskType, countryCode, reqDTO.getMinAge(), reqDTO.getMaxAge(), 
                sexParam, reqDTO.getExcludeSkin(), reqDTO.getCheckUserNameEmpty());
        
        if (totalCount == null || totalCount == 0) {
            log.warn("未找到符合条件的任务记录，taskType={}, countryCode={}", taskType, countryCode);
            throw new RuntimeException("未找到符合条件的任务记录");
        }

        // 构建查询结果
        QueryResultVO result = new QueryResultVO();
        result.setTaskType(taskType);
        result.setCountryCode(countryCode);
        result.setTotalCount(totalCount);
        result.setValidCount(totalCount);

        log.info("=== 查询任务记录数量完成 ===，耗时：{}ms, 符合条件记录数：{}", 
                System.currentTimeMillis() - queryStartTime, result.getTotalCount());

        return result;
    }
}
