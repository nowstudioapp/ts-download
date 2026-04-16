package com.ts.download.controller;

import com.ts.download.annotation.RequireAdmin;
import com.ts.download.domain.dto.LogQueryReqDTO;
import com.ts.download.domain.entity.DownloadLog;
import com.ts.download.domain.vo.PageVO;
import com.ts.download.domain.vo.R;
import com.ts.download.service.DownloadLogService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@Api(tags = "下载日志")
@RestController
@RequestMapping("/api/log")
@Slf4j
public class DownloadLogController {

    @Autowired
    private DownloadLogService downloadLogService;

    @PostMapping("/list")
    @ApiOperation("查询下载日志")
    @RequireAdmin
    public R<PageVO<DownloadLog>> list(@RequestBody LogQueryReqDTO reqDTO) {
        PageVO<DownloadLog> result = downloadLogService.queryPage(reqDTO, null, "admin");
        return R.ok(result, "查询成功");
    }
}
