package com.ts.download.service.impl;

import com.ts.download.dao.DownloadLogDao;
import com.ts.download.domain.dto.LogQueryReqDTO;
import com.ts.download.domain.entity.DownloadLog;
import com.ts.download.domain.vo.PageVO;
import com.ts.download.service.DownloadLogService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
public class DownloadLogServiceImpl implements DownloadLogService {

    @Autowired
    private DownloadLogDao downloadLogDao;

    @Override
    public void record(DownloadLog dl) {
        try {
            dl.setId(downloadLogDao.getMaxId() + 1);
            downloadLogDao.insert(dl);
        } catch (Exception e) {
            log.error("记录下载日志失败", e);
        }
    }

    @Override
    public PageVO<DownloadLog> queryPage(LogQueryReqDTO reqDTO, String currentUsername, String currentRole) {
        String queryUsername = reqDTO.getUsername();
        if (!"admin".equals(currentRole)) {
            queryUsername = currentUsername;
        }

        int offset = (reqDTO.getPage() - 1) * reqDTO.getPageSize();
        List<DownloadLog> records = downloadLogDao.queryPage(queryUsername, reqDTO.getCountryCode(),
                reqDTO.getTaskType(), reqDTO.getStartTime(), reqDTO.getEndTime(),
                offset, reqDTO.getPageSize());
        Long total = downloadLogDao.countTotal(queryUsername, reqDTO.getCountryCode(),
                reqDTO.getTaskType(), reqDTO.getStartTime(), reqDTO.getEndTime());

        return PageVO.of(records, total, reqDTO.getPage(), reqDTO.getPageSize());
    }
}
