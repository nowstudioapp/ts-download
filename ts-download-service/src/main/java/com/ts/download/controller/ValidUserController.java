package com.ts.download.controller;

import com.alibaba.fastjson2.JSON;
import com.ts.download.annotation.RequireLeader;
import com.ts.download.domain.entity.DownloadLog;
import com.ts.download.domain.vo.R;
import com.ts.download.domain.vo.UserVO;
import com.ts.download.service.DownloadLogService;
import com.ts.download.service.ValidUserService;
import com.ts.download.util.IpUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

@Api(tags = "有效用户下载")
@RestController
@RequestMapping("/api/validUser")
@Slf4j
public class ValidUserController {

    @Autowired
    private ValidUserService validUserService;

    @Autowired
    private DownloadLogService downloadLogService;

    @PostMapping("/download")
    @ApiOperation("下载有效用户手机号")
    @RequireLeader
    public R<String> download(@RequestBody Map<String, String> params, HttpServletRequest request) {
        String type = params.get("type");
        String countryCode = params.get("countryCode");

        if (type == null || countryCode == null) {
            return R.fail("参数不完整，需要 type 和 countryCode");
        }
        if (!"tg".equalsIgnoreCase(type) && !"ws".equalsIgnoreCase(type)) {
            return R.fail("type 参数无效，只支持 tg 或 ws");
        }

        UserVO user = (UserVO) request.getAttribute("currentUser");
        log.info("用户 {} 下载有效用户, type={}, countryCode={}", user.getUsername(), type, countryCode);

        try {
            String downloadUrl = validUserService.downloadValidUsers(type.toLowerCase(), countryCode);

            if (!"admin".equals(user.getRole()) && !"superAdmin".equals(user.getRole())) {
                DownloadLog dl = new DownloadLog();
                dl.setUserId(user.getId());
                dl.setUsername(user.getUsername());
                dl.setIp(IpUtil.getClientIp(request));
                dl.setCountryCode(countryCode);
                dl.setFirstTaskType("validUser");
                dl.setSecondTaskType(type);
                dl.setDownloadParams(JSON.toJSONString(params));
                dl.setFileUrl(downloadUrl);
                downloadLogService.record(dl);
            }

            return R.ok(downloadUrl, "导出成功");
        } catch (Exception e) {
            log.error("有效用户下载失败：{}", e.getMessage(), e);
            return R.fail("下载失败：" + e.getMessage());
        }
    }

    @PostMapping("/count")
    @ApiOperation("查询有效用户数量")
    @RequireLeader
    public R<Map<String, Object>> count(@RequestBody Map<String, String> params) {
        String type = params.get("type");
        String countryCode = params.get("countryCode");

        if (type == null || countryCode == null) {
            return R.fail("参数不完整，需要 type 和 countryCode");
        }
        if (!"tg".equalsIgnoreCase(type) && !"ws".equalsIgnoreCase(type)) {
            return R.fail("type 参数无效，只支持 tg 或 ws");
        }

        try {
            Long count = validUserService.countValidUsers(type.toLowerCase(), countryCode);
            Map<String, Object> result = new HashMap<>();
            result.put("type", type);
            result.put("countryCode", countryCode);
            result.put("count", count);
            return R.ok(result, "查询成功");
        } catch (Exception e) {
            log.error("查询有效用户数量失败：{}", e.getMessage(), e);
            return R.fail("查询失败：" + e.getMessage());
        }
    }
}
