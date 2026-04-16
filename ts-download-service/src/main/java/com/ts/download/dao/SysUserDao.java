package com.ts.download.dao;

import com.ts.download.domain.entity.SysUser;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

@Repository
@Slf4j
public class SysUserDao {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private SysUser mapRow(ResultSet rs) throws SQLException {
        SysUser user = new SysUser();
        user.setId(rs.getLong("id"));
        user.setUsername(rs.getString("username"));
        user.setPassword(rs.getString("password"));
        user.setNickname(rs.getString("nickname"));
        user.setRole(rs.getString("role"));
        user.setStatus(rs.getInt("status"));
        user.setDeleted(rs.getInt("deleted"));
        user.setCreateTime(rs.getTimestamp("create_time"));
        user.setUpdateTime(rs.getTimestamp("update_time"));
        user.setVersion(rs.getLong("version"));
        return user;
    }

    public SysUser findByUsername(String username) {
        String sql = "SELECT * FROM sys_user FINAL WHERE deleted = 0 AND username = ?";
        List<SysUser> list = jdbcTemplate.query(sql, (rs, rowNum) -> mapRow(rs), username);
        return list.isEmpty() ? null : list.get(0);
    }

    public SysUser findById(Long id) {
        String sql = "SELECT * FROM sys_user FINAL WHERE deleted = 0 AND id = ?";
        List<SysUser> list = jdbcTemplate.query(sql, (rs, rowNum) -> mapRow(rs), id);
        return list.isEmpty() ? null : list.get(0);
    }

    public List<SysUser> findAll() {
        String sql = "SELECT * FROM sys_user FINAL WHERE deleted = 0 AND role != 'superAdmin' ORDER BY create_time DESC";
        return jdbcTemplate.query(sql, (rs, rowNum) -> mapRow(rs));
    }

    public void insert(SysUser user) {
        String sql = "INSERT INTO sys_user (id, username, password, nickname, role, status, deleted, create_time, update_time, version) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, now(), now(), ?)";
        jdbcTemplate.update(sql, user.getId(), user.getUsername(), user.getPassword(),
                user.getNickname(), user.getRole(), user.getStatus(), user.getDeleted(), user.getVersion());
    }

    /**
     * 覆盖写更新：查出旧行，修改字段后 INSERT 新行（version+1）
     */
    public void insertOrUpdate(SysUser user) {
        String sql = "INSERT INTO sys_user (id, username, password, nickname, role, status, deleted, create_time, update_time, version) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, now(), ?)";
        jdbcTemplate.update(sql, user.getId(), user.getUsername(), user.getPassword(),
                user.getNickname(), user.getRole(), user.getStatus(), user.getDeleted(),
                user.getCreateTime(), user.getVersion());
    }

    public Long getMaxId() {
        String sql = "SELECT max(id) FROM sys_user WHERE role != 'superAdmin'";
        Long maxId = jdbcTemplate.queryForObject(sql, Long.class);
        return maxId == null ? 0L : maxId;
    }
}
