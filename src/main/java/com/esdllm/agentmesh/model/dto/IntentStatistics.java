package com.esdllm.agentmesh.model.dto;

import lombok.Data;

/**
 * 意图统计信息
 */
@Data
public class IntentStatistics {
    
    /**
     * 意图类型
     */
    private String intentType;
    
    /**
     * 使用次数
     */
    private Long count;
    
    /**
     * 平均置信度
     */
    private Double averageConfidence;
}
