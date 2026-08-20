package com.esdllm.agentmesh.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 系统运行状态信息
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SystemStatus {
    
    /**
     * 系统运行时间 (秒)
     */
    private Long uptimeSeconds;
    
    /**
     * API 调用次数 (今日)
     */
    private Long todayApiCalls;
    
    /**
     * 平均响应时间 (毫秒)
     */
    private Double averageResponseTimeMs;
    
    /**
     * 错误率 (百分比)
     */
    private Double errorRate;
    
    /**
     * 数据库连接状态
     */
    private String databaseStatus;
    
    /**
     * 缓存状态
     */
    private String cacheStatus;
}
