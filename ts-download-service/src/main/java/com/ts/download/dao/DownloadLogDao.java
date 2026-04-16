package com.ts.download.dao;

import com.ts.download.domain.entity.DownloadLog;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

@Repository
@Slf4j
public class DownloadLogDao {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private DownloadLog mapRow(ResultSet rs) throws SQLException {
        DownloadLog dl = new DownloadLog();
        dl.setId(rs.getLong("id"));
        dl.setUserId(rs.getLong("user_id"));
        dl.setUsername(rs.getString("username"));
        dl.setIp(rs.getString("ip"));
        dl.setCountryCode(rs.getString("country_code"));
        dl.setFirstTaskType(rs.getString("first_task_type"));
        dl.setSecondTaskType(rs.getString("second_task_type"));
        dl.setDownloadParams(rs.getString("download_params"));
        dl.setFileUrl(rs.getString("file_url"));
        dl.setCreateTime(rs.getTimestamp("create_time"));
        return dl;
    }

    public void insert(DownloadLog dl) {
        String sql = "INSERT INTO download_log (id, user_id, username, ip, country_code, first_task_type, second_task_type, download_params, file_url, create_time) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, now())";
        jdbcTemplate.update(sql, dl.getId(), dl.getUserId(), dl.getUsername(), dl.getIp(),
                dl.getCountryCode(), dl.getFirstTaskType(), dl.getSecondTaskType(),
                dl.getDownloadParams(), dl.getFileUrl());
    }

    public List<DownloadLog> queryPage(String username, String countryCode, String taskType,
                                       String startTime, String endTime,
                                       int offset, int limit) {
        StringBuilder sql = new StringBuilder("SELECT * FROM download_log WHERE 1=1");
        List<Object> params = new ArrayList<>();

        if (username != null && !username.isEmpty()) {
            sql.append(" AND username = ?");
            params.add(username);
        }
        if (countryCode != null && !countryCode.isEmpty()) {
            sql.append(" AND country_code = ?");
            params.add(countryCode);
        }
        if (taskType != null && !taskType.isEmpty()) {
            sql.append(" AND (first_task_type = ? OR second_task_type = ?)");
            params.add(taskType);
            params.add(taskType);
        }
        if (startTime != null && !startTime.isEmpty()) {
            sql.append(" AND create_time >= ?");
            params.add(startTime);
        }
        if (endTime != null && !endTime.isEmpty()) {
            sql.append(" AND create_time <= ?");
            params.add(endTime);
        }
        sql.append(" ORDER BY create_time DESC LIMIT ").append(offset).append(", ").append(limit);

        return jdbcTemplate.query(sql.toString(), (rs, rowNum) -> mapRow(rs), params.toArray());
    }

    public Long countTotal(String username, String countryCode, String taskType,
                           String startTime, String endTime) {
        StringBuilder sql = new StringBuilder("SELECT count() FROM download_log WHERE 1=1");
        List<Object> params = new ArrayList<>();

        if (username != null && !username.isEmpty()) {
            sql.append(" AND username = ?");
            params.add(username);
        }
        if (countryCode != null && !countryCode.isEmpty()) {
            sql.append(" AND country_code = ?");
            params.add(countryCode);
        }
        if (taskType != null && !taskType.isEmpty()) {
            sql.append(" AND (first_task_type = ? OR second_task_type = ?)");
            params.add(taskType);
            params.add(taskType);
        }
        if (startTime != null && !startTime.isEmpty()) {
            sql.append(" AND create_time >= ?");
            params.add(startTime);
        }
        if (endTime != null && !endTime.isEmpty()) {
            sql.append(" AND create_time <= ?");
            params.add(endTime);
        }
        return jdbcTemplate.queryForObject(sql.toString(), Long.class, params.toArray());
    }

    public Long getMaxId() {
        Long maxId = jdbcTemplate.queryForObject("SELECT max(id) FROM download_log", Long.class);
        return maxId == null ? 0L : maxId;
    }
}
