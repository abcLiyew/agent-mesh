package com.esdllm.agentmesh.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 工具健康检查配置
 */
@Data
@Component
@ConfigurationProperties(prefix = "tool.health-check")
public class HealthCheckProperties {

    /**
     * 是否启用健康检查
     */
    private Boolean enabled = true;

    /**
     * 检查间隔时间（毫秒）
     */
    private Long intervalMs = 1000*60*60*7L; // 默认 1 分钟

    /**
     * 超时时间（毫秒）
     */
    private Long timeoutMs = 5000L; // 默认 5 秒

    /**
     * 最大重试次数
     */
    private Integer maxRetries = 3;

    /**
     * 重试间隔（毫秒）
     */
    private Long retryIntervalMs = 2000L; // 默认 2 秒

    /**
     * 连续失败多少次后标记为异常
     */
    private Integer failureThreshold = 3;

    /**
     * 恢复健康所需的成功次数
     */
    private Integer recoveryThreshold = 2;
}
