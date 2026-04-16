package com.ts.download.controller;

import com.ts.download.annotation.RequireAdmin;
import com.ts.download.domain.dto.LogQueryReqDTO;
import com.ts.download.domain.entity.LoginLog;
import com.ts.download.domain.vo.PageVO;
import com.ts.download.domain.vo.R;
import com.ts.download.service.LoginLogService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@Api(tags = "登录日志")
@RestController
@RequestMapping("/api/loginLog")
@Slf4j
public class LoginLogController {

    @Autowired
    private LoginLogService loginLogService;

    @PostMapping("/list")
    @ApiOperation("查询登录日志")
    @RequireAdmin
    public R<PageVO<LoginLog>> list(@RequestBody LogQueryReqDTO reqDTO) {
        PageVO<LoginLog> result = loginLogService.queryPage(reqDTO);
        return R.ok(result, "查询成功");
    }
}
