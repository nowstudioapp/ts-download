package com.ts.download.controller;

import com.alibaba.fastjson2.JSON;
import com.ts.download.domain.dto.MergeDownloadReqDTO;
import com.ts.download.domain.dto.QueryTaskReqDTO;
import com.ts.download.domain.entity.DownloadLog;
import com.ts.download.domain.vo.R;
import com.ts.download.domain.vo.UserVO;
import com.ts.download.service.DownloadLogService;
import com.ts.download.service.DownloadService;
import com.ts.download.util.IpUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;

@Api(tags = "文件下载")
@RestController
@RequestMapping("/api/download")
@Slf4j
public class DownloadController {

    @Autowired
    private DownloadService downloadService;

    @Autowired
    private DownloadLogService downloadLogService;

    @PostMapping("/mergeDownload")
    @ApiOperation("合并两个任务类型下载")
    public R<String> mergeDownload(@RequestBody MergeDownloadReqDTO reqDTO, HttpServletRequest request) {
        UserVO user = (UserVO) request.getAttribute("currentUser");
        log.info("用户 {} 发起合并下载请求，firstTaskType={}, secondTaskType={}, countryCode={}",
                user.getUsername(), reqDTO.getFirstTaskType(), reqDTO.getSecondTaskType(), reqDTO.getCountryCode());

        try {
            String downloadUrl = downloadService.generateMergeDownloadUrl(reqDTO);

            if (!"admin".equals(user.getRole()) && !"superAdmin".equals(user.getRole())) {
                DownloadLog dl = new DownloadLog();
                dl.setUserId(user.getId());
                dl.setUsername(user.getUsername());
                dl.setIp(IpUtil.getClientIp(request));
                dl.setCountryCode(reqDTO.getCountryCode());
                dl.setFirstTaskType(reqDTO.getFirstTaskType());
                dl.setSecondTaskType(reqDTO.getSecondTaskType() != null ? reqDTO.getSecondTaskType() : "");
                dl.setDownloadParams(JSON.toJSONString(reqDTO));
                dl.setFileUrl(downloadUrl);
                downloadLogService.record(dl);
            }

            return R.ok(downloadUrl, "合并文件生成成功");
        } catch (Exception e) {
            log.error("合并下载异常：{}", e.getMessage(), e);
            return R.fail("合并下载失败：" + e.getMessage());
        }
    }

    @PostMapping("/queryTaskCount")
    @ApiOperation("查询任务记录数量")
    public R<Object> queryTaskCount(@RequestBody QueryTaskReqDTO reqDTO) {
        log.info("查询任务记录数量，taskType={}, countryCode={}", reqDTO.getTaskType(), reqDTO.getCountryCode());

        try {
            Object result = downloadService.queryTaskCount(reqDTO);
            return R.ok(result, "查询成功");
        } catch (Exception e) {
            log.error("查询任务记录数量异常：{}", e.getMessage(), e);
            return R.fail("查询失败：" + e.getMessage());
        }
    }
}
