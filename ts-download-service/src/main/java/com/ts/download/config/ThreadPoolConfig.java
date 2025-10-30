package com.ts.download.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 线程池配置
 * 
 * @author TS Team
 */
@Configuration
public class ThreadPoolConfig {

    @Bean(name = "taskExecutor")
    public Executor threadPoolTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        
        // 核心线程数
        executor.setCorePoolSize(10);
        
        // 最大线程数
        executor.setMaxPoolSize(50);
        
        // 队列容量
        executor.setQueueCapacity(200);
        
        // 线程空闲时间
        executor.setKeepAliveSeconds(60);
        
        // 线程名称前缀
        executor.setThreadNamePrefix("ts-download-");
        
        // 拒绝策略：由调用线程处理
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        
        // 等待所有任务结束后再关闭线程池
        executor.setWaitForTasksToCompleteOnShutdown(true);
        
        executor.initialize();
        return executor;
    }
}
