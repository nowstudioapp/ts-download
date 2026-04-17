package com.ts.download.service.impl;

import com.ts.download.dao.ValidUserSyncDao;
import com.ts.download.service.ValidUserSyncService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

@Service
@Slf4j
public class ValidUserSyncServiceImpl implements ValidUserSyncService {

    private static final Pattern DIGITS_ONLY = Pattern.compile("\\d+");

    @Autowired
    private ValidUserSyncDao validUserSyncDao;

    @Override
    public void syncYesterdayValidUsers() {
        long startTime = System.currentTimeMillis();
        log.info("===== 开始同步昨日有效用户 =====");

        List<Map<String, Object>> tasks = validUserSyncDao.fetchYesterdaySuccessTasks();
        if (tasks.isEmpty()) {
            log.info("昨日无成功迁移任务，跳过");
            return;
        }

        int tgTaskCount = 0, wsTaskCount = 0;
        long tgInserted = 0, wsInserted = 0;
        int skipCount = 0;

        for (Map<String, Object> task : tasks) {
            String taskId = String.valueOf(task.get("task_id"));
            String taskType = String.valueOf(task.get("task_type"));
            String countryCode = String.valueOf(task.get("country_code"));

            String sourceTable = ValidUserSyncDao.getSourceTable(taskType);
            String targetTable = ValidUserSyncDao.getTargetTable(taskType);

            if (sourceTable == null || targetTable == null) {
                skipCount++;
                continue;
            }

            try {
                List<String[]> phones = validUserSyncDao.fetchDistinctPhones(sourceTable, taskId);

                List<String[]> validPhones = new ArrayList<>();
                for (String[] record : phones) {
                    String phone = record[0];
                    if (phone != null && DIGITS_ONLY.matcher(phone).matches()) {
                        validPhones.add(record);
                    }
                }

                if (validPhones.isEmpty()) {
                    log.info("任务 {} ({}_{}) 无有效手机号，跳过", taskId, taskType, countryCode);
                    continue;
                }

                validUserSyncDao.batchInsertValidUsers(targetTable, validPhones);

                if (ValidUserSyncDao.isTgType(taskType)) {
                    tgTaskCount++;
                    tgInserted += validPhones.size();
                } else {
                    wsTaskCount++;
                    wsInserted += validPhones.size();
                }

                log.info("任务 {} ({}_{}) -> {} 插入 {} 条",
                        taskId, taskType, countryCode, targetTable, validPhones.size());
            } catch (Exception e) {
                log.error("处理任务 {} ({}_{}) 失败: {}",
                        taskId, taskType, countryCode, e.getMessage(), e);
            }
        }

        long elapsed = System.currentTimeMillis() - startTime;
        log.info("===== 有效用户同步完成 =====");
        log.info("  总任务数: {}, 跳过(非TG/WS): {}", tasks.size(), skipCount);
        log.info("  TG: {} 个任务, 插入 {} 条 -> tg_valid_users", tgTaskCount, tgInserted);
        log.info("  WS: {} 个任务, 插入 {} 条 -> ws_valid_users", wsTaskCount, wsInserted);
        log.info("  总耗时: {}ms", elapsed);
    }
}
