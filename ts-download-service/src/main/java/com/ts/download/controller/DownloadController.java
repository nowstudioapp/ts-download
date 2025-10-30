package com.ts.download.controller;

import com.ts.download.domain.dto.DownloadReqDTO;
import com.ts.download.domain.dto.MergeDownloadReqDTO;
import com.ts.download.domain.vo.R;
import com.ts.download.service.DownloadService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 文件下载控制器
 * 
 * @author TS Team
 */
@Api(tags = "文件下载")
@RestController
@RequestMapping("/api/download")
@Slf4j
public class DownloadController {

    @Autowired
    private DownloadService downloadService;

    @PostMapping("/generateDownloadUrl")
    @ApiOperation("生成文件下载地址（上传到COS）")
    public R<String> generateDownloadUrl(@RequestBody DownloadReqDTO reqDTO) {
        log.info("=== 接收到下载请求 ===，downloadType={}, taskType={}, countryCode={}",
                reqDTO.getDownloadType(), reqDTO.getTaskType(), reqDTO.getCountryCode());
        
        try {
            String downloadUrl = downloadService.generateDownloadUrl(reqDTO);
            return R.ok(downloadUrl, "文件生成成功");
        } catch (Exception e) {
            log.error("生成下载地址异常：{}", e.getMessage(), e);
            return R.fail("生成下载地址失败：" + e.getMessage());
        }
    }

    @PostMapping("/query")
    @ApiOperation("查询文件信息")
    public R<Object> queryFileInfo(@RequestBody DownloadReqDTO reqDTO) {
        log.info("=== 接收到查询请求 ===，downloadType={}, taskType={}, countryCode={}",
                reqDTO.getDownloadType(), reqDTO.getTaskType(), reqDTO.getCountryCode());
        
        try {
            Object result = downloadService.queryFileInfo(reqDTO);
            return R.ok(result, "查询成功");
        } catch (Exception e) {
            log.error("查询文件信息异常：{}", e.getMessage(), e);
            return R.fail("查询失败：" + e.getMessage());
        }
    }

    @PostMapping("/mergeDownload")
    @ApiOperation("合并两个任务类型下载（根据phone匹配）")
    public R<String> mergeDownload(@RequestBody MergeDownloadReqDTO reqDTO) {
        log.info("=== 接收到合并下载请求 ===，firstTaskType={}, secondTaskType={}, countryCode={}, minAge={}, maxAge={}, sex={}, excludeSkin={}",
                reqDTO.getFirstTaskType(), reqDTO.getSecondTaskType(), reqDTO.getCountryCode(), 
                reqDTO.getMinAge(), reqDTO.getMaxAge(), reqDTO.getSex(), reqDTO.getExcludeSkin());
        
        try {
            String downloadUrl = downloadService.generateMergeDownloadUrl(reqDTO);
            return R.ok(downloadUrl, "合并文件生成成功");
        } catch (Exception e) {
            log.error("合并下载异常：{}", e.getMessage(), e);
            return R.fail("合并下载失败：" + e.getMessage());
        }
    }
}
