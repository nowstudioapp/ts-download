package com.ts.download.service;

import com.ts.download.domain.dto.LogQueryReqDTO;
import com.ts.download.domain.entity.DownloadLog;
import com.ts.download.domain.vo.PageVO;

public interface DownloadLogService {
    void record(DownloadLog log);
    PageVO<DownloadLog> queryPage(LogQueryReqDTO reqDTO, String currentUsername, String currentRole);
}
