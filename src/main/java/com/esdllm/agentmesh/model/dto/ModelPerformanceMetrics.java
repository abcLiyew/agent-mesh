package com.esdllm.agentmesh.model.dto;


import lombok.Data;
import java.math.BigDecimal;

/**
 * 模型性能指标
 */
@Data
public class ModelPerformanceMetrics {
    
    /**
     * 模型 ID
     */
    private Long modelId;
    
    /**
     * 平均响应时间（毫秒）
     */
    private Double avgResponseTimeMs;
    
    /**
     * 成功率（0-1）
     */
    private Double successRate;
    
    /**
     * 输入成本（每 1k tokens）
     */
    private BigDecimal inputCostPer1k;
    
    /**
     * 输出成本（每 1k tokens）
     */
    private BigDecimal outputCostPer1k;
    
    /**
     * 上下文窗口大小
     */
    private Integer contextWindow;
    
    /**
     * 性能评分（综合计算得出，0-100）
     */
    private Double performanceScore;
}
