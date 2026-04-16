package com.ts.download.service;

import com.ts.download.domain.dto.LoginReqDTO;
import com.ts.download.domain.vo.UserVO;

import javax.servlet.http.HttpServletRequest;

public interface AuthService {
    UserVO login(LoginReqDTO reqDTO, HttpServletRequest request);
    void logout(HttpServletRequest request);
    UserVO getCurrentUser(HttpServletRequest request);
}
