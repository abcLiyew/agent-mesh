package com.esdllm.agentmesh.model.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 对话统计数据
 */
@Data
public class ConversationStatistics {
    
    /**
     * 总对话次数
     */
    private Long totalConversations;
    
    /**
     * 成功对话次数
     */
    private Long successfulConversations;
    
    /**
     * 失败对话次数
     */
    private Long failedConversations;
    
    /**
     * 成功率（百分比）
     */
    private Double successRate;
    
    /**
     * 总成本（元）
     */
    private BigDecimal totalCost;
    
    /**
     * 平均响应时间（毫秒）
     */
    private Long averageResponseTime;
    
    /**
     * 平均对话轮次
     */
    private Double averageTurns;
    
    /**
     * 活跃会话数
     */
    private Long activeSessions;
}
