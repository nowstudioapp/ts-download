package com.ts.download.dao;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Repository
@Slf4j
public class ValidUserSyncDao {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private static final Set<String> TG_TASK_TYPES = new HashSet<>(
            Arrays.asList("sieveLive", "sieveAvatar", "tgEffective"));
    private static final Set<String> WS_TASK_TYPES = new HashSet<>(
            Arrays.asList("gender", "whatsappExist", "wsValid", "wsExist"));

    public static boolean isTgType(String taskType) {
        return TG_TASK_TYPES.contains(taskType);
    }

    public static boolean isWsType(String taskType) {
        return WS_TASK_TYPES.contains(taskType);
    }

    public static String getSourceTable(String taskType) {
        if (isTgType(taskType)) return "ts_tg_task_record";
        if (isWsType(taskType)) return "ts_ws_task_record";
        return null;
    }

    public static String getTargetTable(String taskType) {
        if (isTgType(taskType)) return "tg_valid_users";
        if (isWsType(taskType)) return "ws_valid_users";
        return null;
    }

    public List<Map<String, Object>> fetchYesterdaySuccessTasks() {
        LocalDate yesterday = LocalDate.now().minusDays(1);
        String start = yesterday.format(DateTimeFormatter.ISO_LOCAL_DATE) + " 00:00:00";
        String end = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE) + " 00:00:00";

        String sql = "SELECT task_id, task_type, country_code FROM migrate_log FINAL " +
                "WHERE status = 'success' AND migrate_time >= '" + start + "' AND migrate_time < '" + end + "'";
        log.info("查询昨日成功迁移任务, SQL: {}", sql);

        List<Map<String, Object>> tasks = jdbcTemplate.queryForList(sql);
        log.info("昨日成功迁移任务数: {}", tasks.size());
        return tasks;
    }

    public List<String[]> fetchDistinctPhones(String sourceTable, String taskId) {
        String sql = "SELECT DISTINCT phone, country_code FROM " + sourceTable + " WHERE task_id = ?";
        return jdbcTemplate.query(sql, new Object[]{taskId}, (rs, rowNum) ->
                new String[]{rs.getString("phone"), rs.getString("country_code")}
        );
    }

    public void batchInsertValidUsers(String targetTable, List<String[]> records) {
        if (records == null || records.isEmpty()) return;

        int chunkSize = 5000;
        for (int start = 0; start < records.size(); start += chunkSize) {
            int end = Math.min(start + chunkSize, records.size());
            List<String[]> chunk = records.subList(start, end);
            doInsertChunk(targetTable, chunk);
        }
    }

    private void doInsertChunk(String targetTable, List<String[]> chunk) {
        StringBuilder sql = new StringBuilder();
        sql.append("INSERT INTO ").append(targetTable).append(" (phone, country_code) VALUES ");

        List<Object> params = new ArrayList<>();
        for (int i = 0; i < chunk.size(); i++) {
            if (i > 0) sql.append(", ");
            sql.append("(?, ?)");
            params.add(chunk.get(i)[0]);
            params.add(chunk.get(i)[1]);
        }

        jdbcTemplate.update(sql.toString(), params.toArray());
    }
}
