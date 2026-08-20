package com.esdllm.agentmesh.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

/**
 * 健康检查结果
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HealthCheckResult {

    /**
     * 工具 ID
     */
    private Long toolId;

    /**
     * 工具名称
     */
    private String toolName;

    /**
     * 是否健康
     */
    private Boolean healthy;

    /**
     * 健康状态：0=未知，1=健康，2=异常，3=禁用
     */
    private Integer healthStatus;

    /**
     * 响应时间（毫秒）
     */
    private Long responseTimeMs;

    /**
     * 状态码
     */
    private Integer statusCode;

    /**
     * 错误信息
     */
    private String errorMessage;

    /**
     * 检查时间
     */
    private Date checkTime;

    /**
     * 是否重试
     */
    private Boolean retried;

    /**
     * 重试次数
     */
    private Integer retryCount;
}
