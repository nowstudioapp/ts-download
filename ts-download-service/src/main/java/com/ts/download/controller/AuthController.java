package com.ts.download.controller;

import com.ts.download.domain.dto.LoginReqDTO;
import com.ts.download.domain.vo.R;
import com.ts.download.domain.vo.UserVO;
import com.ts.download.service.AuthService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;

@Api(tags = "认证管理")
@RestController
@RequestMapping("/api/auth")
@Slf4j
public class AuthController {

    @Autowired
    private AuthService authService;

    @PostMapping("/login")
    @ApiOperation("用户登录")
    public R<UserVO> login(@RequestBody LoginReqDTO reqDTO, HttpServletRequest request) {
        try {
            UserVO user = authService.login(reqDTO, request);
            return R.ok(user, "登录成功");
        } catch (Exception e) {
            log.warn("登录失败: {}", e.getMessage());
            return R.fail(e.getMessage());
        }
    }

    @PostMapping("/logout")
    @ApiOperation("退出登录")
    public R<Void> logout(HttpServletRequest request) {
        authService.logout(request);
        return R.ok(null, "退出成功");
    }

    @GetMapping("/info")
    @ApiOperation("获取当前用户信息")
    public R<UserVO> info(HttpServletRequest request) {
        UserVO user = authService.getCurrentUser(request);
        if (user == null) {
            return R.fail("未登录");
        }
        return R.ok(user, "获取成功");
    }
}
