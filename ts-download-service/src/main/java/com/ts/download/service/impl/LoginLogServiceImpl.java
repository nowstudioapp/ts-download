package com.ts.download.service.impl;

import com.ts.download.dao.LoginLogDao;
import com.ts.download.domain.dto.LogQueryReqDTO;
import com.ts.download.domain.entity.LoginLog;
import com.ts.download.domain.vo.PageVO;
import com.ts.download.service.LoginLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class LoginLogServiceImpl implements LoginLogService {

    @Autowired
    private LoginLogDao loginLogDao;

    @Override
    public PageVO<LoginLog> queryPage(LogQueryReqDTO reqDTO) {
        int offset = (reqDTO.getPage() - 1) * reqDTO.getPageSize();
        List<LoginLog> records = loginLogDao.queryPage(reqDTO.getUsername(),
                reqDTO.getStartTime(), reqDTO.getEndTime(), offset, reqDTO.getPageSize());
        Long total = loginLogDao.countTotal(reqDTO.getUsername(),
                reqDTO.getStartTime(), reqDTO.getEndTime());
        return PageVO.of(records, total, reqDTO.getPage(), reqDTO.getPageSize());
    }
}
