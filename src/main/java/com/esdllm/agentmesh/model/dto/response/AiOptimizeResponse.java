package com.esdllm.agentmesh.model.dto.response;

import lombok.Data;

/**
 * AI 辅助优化响应
 */
@Data
public class AiOptimizeResponse {
    
    /**
     * 优化后的描述
     */
    private String optimizedDescription;
    
    /**
     * 优化后的系统提示词
     */
    private String optimizedSystemPrompt;
    
    /**
     * 优化说明
     */
    private String optimizationExplanation;
}
