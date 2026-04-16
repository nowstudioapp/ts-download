package com.ts.download.service;

import com.ts.download.domain.dto.LogQueryReqDTO;
import com.ts.download.domain.entity.LoginLog;
import com.ts.download.domain.vo.PageVO;

public interface LoginLogService {
    PageVO<LoginLog> queryPage(LogQueryReqDTO reqDTO);
}
