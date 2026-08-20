package com.esdllm.agentmesh.model.domain;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 模型调用成本记录
 */
@Data
@TableName("model_usage_cost")
public class ModelUsageCost {
    
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
     * 智能体 ID
     */
   private Long agentId;
    
    /**
     * 模型 ID
     */
   private Long modelId;
    
    /**
     * 模型类型：INTERNAL_DECISION, FINAL_RESPONSE
     */
   private String modelType;
    
    /**
     * 输入 Token 数
     */
   private Integer inputTokens;
    
    /**
     * 输出 Token 数
     */
   private Integer outputTokens;
    
    /**
     * 总 Token 数
     */
   private Integer totalTokens;
    
    /**
     * 成本（元）
     */
   private BigDecimal cost;
    
    /**
     * 货币类型
     */
   private String currencyType;
    
    /**
     * 调用状态：SUCCESS, FAILED
     */
   private Integer status;
    
    /**
     * 创建时间
     */
   private Date createdAt;
}
