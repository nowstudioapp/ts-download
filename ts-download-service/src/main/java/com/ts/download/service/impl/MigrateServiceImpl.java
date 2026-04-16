package com.ts.download.service.impl;

import com.ts.download.dao.MigrateDao;
import com.ts.download.service.MigrateService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Pattern;

@Service
@Slf4j
@ConditionalOnProperty(name = "migrate.enabled", havingValue = "true")
public class MigrateServiceImpl implements MigrateService {

    @Autowired
    private MigrateDao migrateDao;

    @Value("${migrate.batch-size:50000}")
    private int batchSize;

    private static final Set<String> WS_TASK_TYPES = new HashSet<>(Arrays.asList(
            "gender", "whatsappExist", "wsValid", "wsExist"
    ));
    private static final Set<String> TG_TASK_TYPES = new HashSet<>(Arrays.asList(
            "sieveLive", "sieveAvatar", "tgEffective"
    ));
    private static final Set<String> OLD_WS_TASK_TYPES = new HashSet<>(Arrays.asList(
            "gender", "whatsappExist", "wsValid"
    ));
    private static final Set<String> OLD_TG_TASK_TYPES = new HashSet<>(Arrays.asList(
            "sieveLive", "sieveAvatar", "tgEffective"
    ));

    private static final Pattern DIGITS_ONLY = Pattern.compile("\\d+");

    private String getOldTableName(String taskType, String countryCode) {
        if (OLD_WS_TASK_TYPES.contains(taskType)) {
            return "ts_ws_task_record_" + countryCode;
        } else if (OLD_TG_TASK_TYPES.contains(taskType)) {
            return "ts_tg_task_record_" + countryCode;
        } else {
            return "ts_other_task_record_" + countryCode;
        }
    }

    private String getNewTableName(String taskType) {
        if (WS_TASK_TYPES.contains(taskType)) {
            return "ts_ws_task_record";
        } else if (TG_TASK_TYPES.contains(taskType)) {
            return "ts_tg_task_record";
        } else {
            return "ts_other_task_record";
        }
    }

    @Override
    public void executeIncrementalMigrate() {
        log.info("========== 增量迁移开始 ==========");
        long startTime = System.currentTimeMillis();

        try {
            Set<String> migratedSet = migrateDao.loadMigratedTaskIds();
            log.info("已迁移任务数: {}", migratedSet.size());

            // 1. 查询最近6小时已完成的任务
            List<Map<String, Object>> recentTasks = migrateDao.fetchRecentCompletedTasks(6);
            log.info("最近6小时已完成任务数: {}", recentTasks.size());

            // 过滤出未迁移的
            List<Map<String, Object>> pendingTasks = new ArrayList<>();
            for (Map<String, Object> task : recentTasks) {
                String taskId = String.valueOf(task.get("task_id"));
                if (!migratedSet.contains(taskId)) {
                    pendingTasks.add(task);
                }
            }

            // 2. 加载之前失败的任务，加入重试
            List<Map<String, Object>> failedTasks = migrateDao.loadFailedTasks();
            for (Map<String, Object> ft : failedTasks) {
                String taskId = String.valueOf(ft.get("task_id"));
                if (!migratedSet.contains(taskId)) {
                    boolean alreadyInPending = pendingTasks.stream()
                            .anyMatch(t -> taskId.equals(String.valueOf(t.get("task_id"))));
                    if (!alreadyInPending) {
                        pendingTasks.add(ft);
                        log.info("加入失败重试: taskId={}", taskId);
                    }
                }
            }

            log.info("待迁移任务数: {} (新增: {}, 重试: {})",
                    pendingTasks.size(),
                    pendingTasks.size() - failedTasks.size(),
                    failedTasks.size());

            if (pendingTasks.isEmpty()) {
                log.info("没有需要迁移的任务");
                return;
            }

            int success = 0, fail = 0;
            long totalRows = 0;

            for (int i = 0; i < pendingTasks.size(); i++) {
                Map<String, Object> task = pendingTasks.get(i);
                String taskId = String.valueOf(task.get("task_id"));
                String taskType = String.valueOf(task.get("task_type"));
                String countryCode = String.valueOf(task.get("country_code"));
                String newTable = getNewTableName(taskType);

                log.info("[{}/{}] 迁移任务: {} | 类型: {} | 国家: {} | 目标: {}",
                        i + 1, pendingTasks.size(), taskId, taskType, countryCode, newTable);

                try {
                    long rowCount = migrateOneTask(taskId, taskType, countryCode, newTable);
                    migrateDao.writeMigrateLog(taskId, taskType, countryCode, newTable, rowCount, "success", "");
                    success++;
                    totalRows += rowCount;
                    log.info("  -> 成功 | {} 行", rowCount);
                } catch (Exception e) {
                    String errMsg = e.getMessage();
                    if (errMsg != null && errMsg.length() > 500) {
                        errMsg = errMsg.substring(0, 500);
                    }
                    migrateDao.writeMigrateLog(taskId, taskType, countryCode, newTable, 0, "failed", errMsg);
                    fail++;
                    log.error("  -> 失败: {}", errMsg);
                }
            }

            long elapsed = System.currentTimeMillis() - startTime;
            log.info("========== 增量迁移完成 ==========");
            log.info("  成功: {} | 失败: {} | 总行数: {} | 耗时: {}ms", success, fail, totalRows, elapsed);

        } catch (Exception e) {
            log.error("增量迁移异常", e);
        }
    }

    private long migrateOneTask(String taskId, String taskType, String countryCode, String newTable) {
        String oldTable = getOldTableName(taskType, countryCode);

        long totalRows = migrateDao.countOldRecords(oldTable, taskId);
        if (totalRows == 0) {
            log.info("  任务 {} 在旧表 {} 中无数据，跳过", taskId, oldTable);
            return 0;
        }

        log.info("  旧表: {} -> 新表: {}, 共 {} 行, 一次性读取...", oldTable, newTable, totalRows);

        long readStart = System.currentTimeMillis();
        List<Map<String, Object>> rows = migrateDao.fetchAllOldRecords(oldTable, taskId);
        log.info("  读取完成, {} 行, 耗时 {}ms", rows.size(), System.currentTimeMillis() - readStart);

        List<Map<String, Object>> validBatch = new ArrayList<>(rows.size());
        int skipped = 0;
        for (Map<String, Object> row : rows) {
            String phone = String.valueOf(row.getOrDefault("phone", ""));
            if (!DIGITS_ONLY.matcher(phone).matches()) {
                skipped++;
                continue;
            }
            cleanUintField(row, "age");
            cleanUintField(row, "active_day");
            cleanUintField(row, "business_number");
            validBatch.add(row);
        }
        rows = null;

        if (skipped > 0) {
            log.info("  跳过 {} 条无效 phone 记录", skipped);
        }

        if (!validBatch.isEmpty()) {
            long writeStart = System.currentTimeMillis();
            migrateDao.batchInsertNewCk(newTable, validBatch);
            log.info("  写入完成, {} 行, 耗时 {}ms", validBatch.size(), System.currentTimeMillis() - writeStart);
        }

        return validBatch.size();
    }

    private void cleanUintField(Map<String, Object> row, String field) {
        Object val = row.get(field);
        int intVal = 0;
        if (val != null) {
            try {
                intVal = Integer.parseInt(String.valueOf(val));
            } catch (NumberFormatException ignored) {
            }
            if (intVal < 0 || intVal > 2147483647) {
                intVal = 0;
            }
        }
        row.put(field, intVal);
    }
}
