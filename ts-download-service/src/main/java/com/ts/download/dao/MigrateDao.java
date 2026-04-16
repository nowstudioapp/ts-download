package com.ts.download.dao;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.util.*;

@Repository
@Slf4j
@ConditionalOnProperty(name = "migrate.enabled", havingValue = "true")
public class MigrateDao {

    @Autowired
    @Qualifier("oldMysqlJdbcTemplate")
    private JdbcTemplate mysqlJdbc;

    @Autowired
    @Qualifier("oldClickHouseJdbcTemplate")
    private JdbcTemplate oldCkJdbc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private static final List<String> COLUMNS = Arrays.asList(
            "id", "phone", "sex", "age", "uid", "user_name",
            "last_online_time", "active_day", "business_number", "member",
            "multiple_avatars", "pic", "status", "skin", "hair_color",
            "ethnicity", "first_name", "last_name", "task_type", "country_code",
            "task_id", "create_time"
    );

    /**
     * 从旧 MySQL 查询最近 N 小时内已完成的任务
     */
    public List<Map<String, Object>> fetchRecentCompletedTasks(int hoursAgo) {
        String sql = "SELECT task_id, task_type, country_code " +
                "FROM ts_task " +
                "WHERE task_status = 1 " +
                "AND update_time >= DATE_SUB(NOW(), INTERVAL ? HOUR) " +
                "ORDER BY update_time ASC";
        return mysqlJdbc.queryForList(sql, hoursAgo);
    }

    /**
     * 从新 CK 的 migrate_log 加载已成功迁移的 task_id 集合
     */
    public Set<String> loadMigratedTaskIds() {
        String sql = "SELECT task_id FROM migrate_log FINAL WHERE status = 'success'";
        List<String> ids = jdbcTemplate.queryForList(sql, String.class);
        return new HashSet<>(ids);
    }

    /**
     * 从新 CK 的 migrate_log 加载失败的任务（用于重试）
     */
    public List<Map<String, Object>> loadFailedTasks() {
        String sql = "SELECT task_id, task_type, country_code FROM migrate_log FINAL WHERE status = 'failed'";
        return jdbcTemplate.queryForList(sql);
    }

    /**
     * 从旧 CK 统计某个任务在旧表中的记录数
     */
    public long countOldRecords(String oldTableName, String taskId) {
        String sql = "SELECT count() FROM " + oldTableName + " WHERE task_id = ?";
        Long count = oldCkJdbc.queryForObject(sql, Long.class, taskId);
        return count != null ? count : 0;
    }

    /**
     * 从旧 CK 一次性读取某个任务的全部数据（单任务最大百万行，无需分批）
     */
    public List<Map<String, Object>> fetchAllOldRecords(String oldTableName, String taskId) {
        String cols = String.join(", ", COLUMNS);
        String sql = "SELECT " + cols + " FROM " + oldTableName + " WHERE task_id = ?";
        return oldCkJdbc.queryForList(sql, taskId);
    }

    /**
     * 批量写入新 CK 表（分小批次提交，减少单次 SQL 大小）
     */
    public void batchInsertNewCk(String newTableName, List<Map<String, Object>> batch) {
        if (batch == null || batch.isEmpty()) return;

        int chunkSize = 5000;
        for (int start = 0; start < batch.size(); start += chunkSize) {
            int end = Math.min(start + chunkSize, batch.size());
            List<Map<String, Object>> chunk = batch.subList(start, end);
            doInsertChunk(newTableName, chunk);
        }
    }

    private void doInsertChunk(String newTableName, List<Map<String, Object>> chunk) {
        String cols = String.join(", ", COLUMNS);
        StringBuilder sb = new StringBuilder(chunk.size() * 200);
        sb.append("INSERT INTO ").append(newTableName).append(" (").append(cols).append(") VALUES ");

        Object[] params = new Object[chunk.size() * COLUMNS.size()];
        int idx = 0;
        for (int i = 0; i < chunk.size(); i++) {
            if (i > 0) sb.append(", ");
            sb.append("(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");
            Map<String, Object> row = chunk.get(i);
            for (String col : COLUMNS) {
                Object val = row.get(col);
                if (val == null) {
                    if ("age".equals(col) || "active_day".equals(col) || "business_number".equals(col)) {
                        params[idx++] = 0;
                    } else if ("last_online_time".equals(col) || "create_time".equals(col)) {
                        params[idx++] = new Timestamp(0);
                    } else {
                        params[idx++] = "";
                    }
                } else {
                    params[idx++] = val;
                }
            }
        }

        jdbcTemplate.update(sb.toString(), params);
    }

    /**
     * 写入迁移日志
     */
    public void writeMigrateLog(String taskId, String taskType, String countryCode,
                                String targetTable, long rowCount, String status, String errorMsg) {
        String sql = "INSERT INTO migrate_log (task_id, task_type, country_code, target_table, row_count, status, error_msg, migrate_time) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, now())";
        jdbcTemplate.update(sql, taskId, taskType, countryCode, targetTable, rowCount, status,
                errorMsg != null ? errorMsg : "");
    }
}
