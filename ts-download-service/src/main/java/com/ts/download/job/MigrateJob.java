package com.ts.download.job;

import com.ts.download.service.MigrateService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicBoolean;

@Component
@Slf4j
@ConditionalOnProperty(name = "migrate.enabled", havingValue = "true")
public class MigrateJob {

    @Autowired
    private MigrateService migrateService;

    private final AtomicBoolean running = new AtomicBoolean(false);

    @Scheduled(cron = "0 0 * * * ?")
    public void scheduledMigrate() {
        if (!running.compareAndSet(false, true)) {
            log.warn("上一轮迁移尚未完成，跳过本次执行");
            return;
        }
        try {
            migrateService.executeIncrementalMigrate();
        } finally {
            running.set(false);
        }
    }
}
