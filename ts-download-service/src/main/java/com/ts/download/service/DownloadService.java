package com.ts.download.service;

import com.ts.download.domain.dto.MergeDownloadReqDTO;
import com.ts.download.domain.dto.QueryTaskReqDTO;

public interface DownloadService {

    String generateMergeDownloadUrl(MergeDownloadReqDTO reqDTO) throws Exception;

    Object queryTaskCount(QueryTaskReqDTO reqDTO) throws Exception;
}
