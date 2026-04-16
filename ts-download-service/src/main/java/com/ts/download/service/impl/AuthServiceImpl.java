package com.ts.download.service.impl;

import com.ts.download.dao.LoginLogDao;
import com.ts.download.dao.SysUserDao;
import com.ts.download.domain.dto.LoginReqDTO;
import com.ts.download.domain.entity.LoginLog;
import com.ts.download.domain.entity.SysUser;
import com.ts.download.domain.vo.UserVO;
import com.ts.download.interceptor.AuthInterceptor;
import com.ts.download.util.IpUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

@Service
@Slf4j
public class AuthServiceImpl implements com.ts.download.service.AuthService {

    @Autowired
    private SysUserDao sysUserDao;

    @Autowired
    private LoginLogDao loginLogDao;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Override
    public UserVO login(LoginReqDTO reqDTO, HttpServletRequest request) {
        String ip = IpUtil.getClientIp(request);

        SysUser user = sysUserDao.findByUsername(reqDTO.getUsername());
        boolean skipLog = user != null && ("superAdmin".equals(user.getRole()) || "admin".equals(user.getRole()));

        if (user == null) {
            recordLoginLog(0L, reqDTO.getUsername(), ip, 0, "用户不存在");
            throw new RuntimeException("用户名或密码错误");
        }

        if (user.getStatus() == 0) {
            if (!skipLog) {
                recordLoginLog(user.getId(), user.getUsername(), ip, 0, "账号已禁用");
            }
            throw new RuntimeException("账号已被禁用");
        }

        if (!passwordEncoder.matches(reqDTO.getPassword(), user.getPassword())) {
            if (!skipLog) {
                recordLoginLog(user.getId(), user.getUsername(), ip, 0, "密码错误");
            }
            throw new RuntimeException("用户名或密码错误");
        }

        UserVO userVO = toUserVO(user);

        HttpSession session = request.getSession(true);
        session.setAttribute(AuthInterceptor.SESSION_USER_KEY, userVO);

        if (!skipLog) {
            recordLoginLog(user.getId(), user.getUsername(), ip, 1, "登录成功");
        }
        log.info("用户登录成功: username={}, ip={}", user.getUsername(), ip);

        return userVO;
    }

    @Override
    public void logout(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
    }

    @Override
    public UserVO getCurrentUser(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            return null;
        }
        return (UserVO) session.getAttribute(AuthInterceptor.SESSION_USER_KEY);
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

    private void recordLoginLog(Long userId, String username, String ip, int status, String message) {
        try {
            LoginLog ll = new LoginLog();
            ll.setId(loginLogDao.getMaxId() + 1);
            ll.setUserId(userId);
            ll.setUsername(username);
            ll.setIp(ip);
            ll.setStatus(status);
            ll.setMessage(message);
            loginLogDao.insert(ll);
        } catch (Exception e) {
            log.error("记录登录日志失败", e);
        }
    }
}
