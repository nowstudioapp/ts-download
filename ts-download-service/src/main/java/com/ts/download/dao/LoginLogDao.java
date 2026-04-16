package com.ts.download.dao;

import com.ts.download.domain.entity.LoginLog;
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
public class LoginLogDao {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private LoginLog mapRow(ResultSet rs) throws SQLException {
        LoginLog ll = new LoginLog();
        ll.setId(rs.getLong("id"));
        ll.setUserId(rs.getLong("user_id"));
        ll.setUsername(rs.getString("username"));
        ll.setIp(rs.getString("ip"));
        ll.setStatus(rs.getInt("status"));
        ll.setMessage(rs.getString("message"));
        ll.setCreateTime(rs.getTimestamp("create_time"));
        return ll;
    }

    public void insert(LoginLog ll) {
        String sql = "INSERT INTO login_log (id, user_id, username, ip, status, message, create_time) " +
                "VALUES (?, ?, ?, ?, ?, ?, now())";
        jdbcTemplate.update(sql, ll.getId(), ll.getUserId(), ll.getUsername(),
                ll.getIp(), ll.getStatus(), ll.getMessage());
    }

    public List<LoginLog> queryPage(String username, String startTime, String endTime,
                                     int offset, int limit) {
        StringBuilder sql = new StringBuilder("SELECT * FROM login_log WHERE 1=1");
        List<Object> params = new ArrayList<>();

        if (username != null && !username.isEmpty()) {
            sql.append(" AND username = ?");
            params.add(username);
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

    public Long countTotal(String username, String startTime, String endTime) {
        StringBuilder sql = new StringBuilder("SELECT count() FROM login_log WHERE 1=1");
        List<Object> params = new ArrayList<>();

        if (username != null && !username.isEmpty()) {
            sql.append(" AND username = ?");
            params.add(username);
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
        Long maxId = jdbcTemplate.queryForObject("SELECT max(id) FROM login_log", Long.class);
        return maxId == null ? 0L : maxId;
    }
}
