package com.esdllm.agentmesh.model.domain;


import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 用户成本阈值配置
 */
@Data
@TableName("user_cost_threshold")
public class UserCostThreshold {
    
    /**
     * 主键 ID
     */
    @TableId
    private Long id;
    
    /**
     * 用户 ID
     */
    private Long userId;
    
    /**
     * 智能体 ID（可选，为空表示全局配置）
     */
    private Long agentId;
    
    /**
     * 日成本阈值（元）
     */
    private BigDecimal dailyThreshold;
    
    /**
     * 周成本阈值（元）
     */
    private BigDecimal weeklyThreshold;
    
    /**
     * 月成本阈值（元）
     */
    private BigDecimal monthlyThreshold;
    
    /**
     * 总成本阈值（元）
     */
    private BigDecimal totalThreshold;
    
    /**
     * 是否启用告警
     */
    private Boolean alertEnabled = true;
    
    /**
     * 是否启用自动降级
     */
    private Boolean autoDowngradeEnabled = false;
    
    /**
     * 降级策略：DOWNGRADE_MODEL（降级模型）, DISABLE_AGENT（禁用智能体）, REDUCE_CALLS（限制调用）
     */
    private String downgradeStrategy;
    
    /**
     * 降级目标模型 ID（当策略为 DOWNGRADE_MODEL 时）
     */
    private Long targetModelId;
    
    /**
     * 通知方式：EMAIL（邮件）, SMS（短信）, WEBHOOK（回调）
     */
    private String notificationMethod;
    
    /**
     * 通知接收地址（邮箱/手机号/Webhook URL）
     */
    private String notificationTarget;
    
    /**
     * 最后告警时间
     */
    private Date lastAlertTime;
    
    /**
     * 告警次数（今日）
     */
    private Integer alertCountToday = 0;
    
    /**
     * 状态：0-正常，1-已暂停
     */
    private Integer status = 0;
    
    /**
     * 创建时间
     */
    private Date createdAt = new Date();
    
    /**
     * 更新时间
     */
    private Date updatedAt = new Date();
}
