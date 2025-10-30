package com.ts.download.service;

import com.ts.download.domain.dto.DownloadReqDTO;
import com.ts.download.domain.dto.MergeDownloadReqDTO;

/**
 * 下载服务接口
 * 
 * @author TS Team
 */
public interface DownloadService {

    /**
     * 生成下载URL
     * 
     * @param reqDTO 下载请求参数
     * @return 下载URL
     */
    String generateDownloadUrl(DownloadReqDTO reqDTO) throws Exception;

    /**
     * 查询文件信息
     * 
     * @param reqDTO 查询请求参数
     * @return 文件信息
     */
    Object queryFileInfo(DownloadReqDTO reqDTO) throws Exception;

    /**
     * 合并两个任务类型生成下载URL（根据phone匹配）
     * 
     * @param reqDTO 合并下载请求参数
     * @return 下载URL
     */
    String generateMergeDownloadUrl(MergeDownloadReqDTO reqDTO) throws Exception;
}
