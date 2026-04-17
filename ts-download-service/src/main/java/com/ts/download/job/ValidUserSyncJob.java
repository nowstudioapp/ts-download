package com.ts.download.job;

import com.ts.download.service.ValidUserSyncService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicBoolean;

@Component
@Slf4j
public class ValidUserSyncJob {

    @Autowired
    private ValidUserSyncService validUserSyncService;

    private final AtomicBoolean running = new AtomicBoolean(false);

    @Scheduled(cron = "0 0 2 * * ?")
    public void syncValidUsers() {
        if (!running.compareAndSet(false, true)) {
            log.warn("上一轮有效用户同步尚未完成，跳过本次执行");
            return;
        }
        try {
            validUserSyncService.syncYesterdayValidUsers();
        } catch (Exception e) {
            log.error("有效用户同步任务异常", e);
        } finally {
            running.set(false);
        }
    }
}
