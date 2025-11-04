package com.ts.download.task;

import com.ts.download.util.LocalFileUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 文件清理定时任务
 * 
 * @author TS Team
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "local.file.enabled", havingValue = "true")
public class FileCleanupTask {

    @Autowired
    private LocalFileUtil localFileUtil;

    /**
     * 清理过期文件
     * 每天凌晨2点执行，删除7天前的文件
     */
    @Scheduled(cron = "0 0 2 * * ?")
    public void cleanupExpiredFiles() {
        log.info("开始执行文件清理任务");
        try {
            localFileUtil.cleanupExpiredFiles(7);
            log.info("文件清理任务执行完成");
        } catch (Exception e) {
            log.error("文件清理任务执行失败", e);
        }
    }

    /**
     * 清理过期文件（手动触发，保留3天）
     * 每周日凌晨3点执行
     */
    @Scheduled(cron = "0 0 3 * * 0")
    public void weeklyCleanup() {
        log.info("开始执行周度文件清理任务");
        try {
            localFileUtil.cleanupExpiredFiles(3);
            log.info("周度文件清理任务执行完成");
        } catch (Exception e) {
            log.error("周度文件清理任务执行失败", e);
        }
    }
}
