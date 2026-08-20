package com.esdllm.agentmesh.config;


import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * 异步任务配置
 */
@Configuration
@EnableAsync
public class AsyncConfig {
    
    /**
     * 文档处理线程池
     */
    @Bean("documentProcessingExecutor")
    public Executor documentProcessingExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);
        executor.setMaxPoolSize(10);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("doc-processing-");
        executor.setRejectedExecutionHandler((r, exec) -> {
            // 拒绝策略：记录日志或抛出异常
            throw new RuntimeException("文档处理队列已满，请稍后重试");
        });
        executor.initialize();
        return executor;
    }
}
