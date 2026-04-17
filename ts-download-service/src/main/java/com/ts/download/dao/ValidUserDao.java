package com.ts.download.dao;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

@Repository
@Slf4j
public class ValidUserDao {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private String getTableName(String type) {
        if ("ws".equalsIgnoreCase(type)) {
            return "ws_valid_users";
        }
        return "tg_valid_users";
    }

    public Long countByCountry(String type, String countryCode) {
        String tableName = getTableName(type);
        String sql = "SELECT COUNT(*) FROM " + tableName + " FINAL WHERE country_code = ?";
        log.info("统计有效用户数量, table={}, countryCode={}", tableName, countryCode);
        try {
            Long count = jdbcTemplate.queryForObject(sql, Long.class, countryCode);
            log.info("统计结果: {}", count);
            return count != null ? count : 0L;
        } catch (Exception e) {
            log.error("统计有效用户数量失败", e);
            return 0L;
        }
    }

    public void streamPhones(String type, String countryCode,
                             Consumer<List<String>> callback, int batchSize) {
        String tableName = getTableName(type);
        String sql = "SELECT phone FROM " + tableName + " FINAL WHERE country_code = ? ORDER BY phone";
        log.info("流式查询有效用户手机号, table={}, countryCode={}", tableName, countryCode);

        try {
            List<String> batch = new ArrayList<>();
            jdbcTemplate.query(sql, new Object[]{countryCode}, (rs) -> {
                batch.add(rs.getString("phone"));
                if (batch.size() >= batchSize) {
                    callback.accept(new ArrayList<>(batch));
                    batch.clear();
                }
            });
            if (!batch.isEmpty()) {
                callback.accept(batch);
            }
            log.info("流式查询完成");
        } catch (Exception e) {
            log.error("流式查询有效用户失败", e);
            throw new RuntimeException("查询有效用户失败: " + e.getMessage(), e);
        }
    }
}
