package com.esdllm.agentmesh.model.dto;


import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 成本告警通知 DTO
 */
@Data
@Builder
public class CostAlertNotification {
    
    /**
     * 用户 ID
     */
    private Long userId;
    
    /**
     * 智能体 ID（可选）
     */
    private Long agentId;
    
    /**
     * 告警类型：DAILY_EXCEEDED（日超限）, WEEKLY_EXCEEDED（周超限）, 
     * MONTHLY_EXCEEDED（月超限）, TOTAL_EXCEEDED（总超限）
     */
    private String alertType;
    
    /**
     * 当前成本
     */
    private BigDecimal currentCost;
    
    /**
     * 阈值
     */
    private BigDecimal threshold;
    
    /**
     * 超出金额
     */
    private BigDecimal exceededAmount;
    
    /**
     * 超出百分比
     */
    private Double exceededPercentage;
    
    /**
     * 是否已触发降级
     */
    private Boolean downgradeTriggered;
    
    /**
     * 降级策略
     */
    private String downgradeStrategy;
    
    /**
     * 通知时间
     */
    private Date alertTime;
    
    /**
     * 消息内容
     */
    private String message;
}
