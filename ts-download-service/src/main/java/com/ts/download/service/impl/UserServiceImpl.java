package com.ts.download.service.impl;

import com.ts.download.dao.SysUserDao;
import com.ts.download.domain.dto.UserAddReqDTO;
import com.ts.download.domain.dto.UserUpdateReqDTO;
import com.ts.download.domain.entity.SysUser;
import com.ts.download.domain.vo.UserVO;
import com.ts.download.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class UserServiceImpl implements UserService {

    @Autowired
    private SysUserDao sysUserDao;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    private static final String USER_CACHE_PREFIX = "ts:user:";

    @Override
    public List<UserVO> listAll() {
        return sysUserDao.findAll().stream().map(this::toUserVO).collect(Collectors.toList());
    }

    @Override
    public void addUser(UserAddReqDTO reqDTO) {
        SysUser existing = sysUserDao.findByUsername(reqDTO.getUsername());
        if (existing != null) {
            throw new RuntimeException("用户名已存在");
        }

        SysUser user = new SysUser();
        user.setId(sysUserDao.getMaxId() + 1);
        user.setUsername(reqDTO.getUsername());
        user.setPassword(passwordEncoder.encode(reqDTO.getPassword()));
        user.setNickname(reqDTO.getNickname() != null ? reqDTO.getNickname() : reqDTO.getUsername());
        user.setRole(reqDTO.getRole() != null ? reqDTO.getRole() : "user");
        user.setStatus(1);
        user.setDeleted(0);
        user.setVersion(1L);

        sysUserDao.insert(user);
        log.info("新增用户: {}", user.getUsername());
    }

    @Override
    public void updateUser(UserUpdateReqDTO reqDTO) {
        SysUser user = sysUserDao.findById(reqDTO.getId());
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }

        if (reqDTO.getNickname() != null) {
            user.setNickname(reqDTO.getNickname());
        }
        if (reqDTO.getPassword() != null && !reqDTO.getPassword().isEmpty()) {
            user.setPassword(passwordEncoder.encode(reqDTO.getPassword()));
        }
        if (reqDTO.getRole() != null) {
            user.setRole(reqDTO.getRole());
        }
        if (reqDTO.getStatus() != null) {
            user.setStatus(reqDTO.getStatus());
        }
        user.setVersion(user.getVersion() + 1);

        sysUserDao.insertOrUpdate(user);
        clearUserCache(user.getId());
        log.info("更新用户: id={}, username={}", user.getId(), user.getUsername());
    }

    @Override
    public void deleteUser(Long id) {
        SysUser user = sysUserDao.findById(id);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }
        if ("admin".equals(user.getRole())) {
            throw new RuntimeException("不能删除管理员账号");
        }

        user.setDeleted(1);
        user.setVersion(user.getVersion() + 1);
        sysUserDao.insertOrUpdate(user);
        clearUserCache(id);
        log.info("删除用户: id={}, username={}", id, user.getUsername());
    }

    @Override
    public void changePassword(Long userId, String oldPassword, String newPassword) {
        SysUser user = sysUserDao.findById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }
        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            throw new RuntimeException("旧密码错误");
        }
        user.setPassword(passwordEncoder.encode(newPassword));
        user.setVersion(user.getVersion() + 1);
        sysUserDao.insertOrUpdate(user);
        clearUserCache(userId);
        log.info("用户修改密码: id={}, username={}", userId, user.getUsername());
    }

    @Override
    public void resetPassword(Long userId, String newPassword) {
        SysUser user = sysUserDao.findById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }
        user.setPassword(passwordEncoder.encode(newPassword));
        user.setVersion(user.getVersion() + 1);
        sysUserDao.insertOrUpdate(user);
        clearUserCache(userId);
        log.info("管理员重置密码: id={}, username={}", userId, user.getUsername());
    }

    private UserVO toUserVO(SysUser user) {
        UserVO vo = new UserVO();
        vo.setId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setNickname(user.getNickname());
        vo.setRole(user.getRole());
        vo.setStatus(user.getStatus());
        vo.setCreateTime(user.getCreateTime());
        return vo;
    }

    private void clearUserCache(Long userId) {
        try {
            redisTemplate.delete(USER_CACHE_PREFIX + userId);
        } catch (Exception e) {
            log.warn("清除用户缓存失败: {}", e.getMessage());
        }
    }
}
