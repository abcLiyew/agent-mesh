package com.esdllm.agentmesh.model.dto.request;

import lombok.Data;

/**
 * AI 辅助优化请求
 */
@Data
public class AiOptimizeRequest {
    
    /**
     * 优化类型：description=仅优化描述，system_prompt=仅优化系统提示词
     */
    private String optimizeType;
    
    /**
     * 当前描述内容（可选）
     */
    private String currentDescription;
    
    /**
     * 当前系统提示词（可选）
     */
    private String currentSystemPrompt;
    
    /**
     * 优化目标或要求（可选）
     */
    private String optimizationGoal;
}
