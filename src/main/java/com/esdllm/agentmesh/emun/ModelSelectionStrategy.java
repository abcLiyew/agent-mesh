package com.esdllm.agentmesh.emun;


/**
 * 模型选择策略
 */
public enum ModelSelectionStrategy {
    
    /**
     * 成本优先：选择最便宜的模型
     */
    COST_FIRST("COST_FIRST", "成本优先"),
    
    /**
     * 性能优先：选择响应最快的模型
     */
    PERFORMANCE_FIRST("PERFORMANCE_FIRST", "性能优先"),
    
    /**
     * 质量优先：选择能力最强的模型
     */
    QUALITY_FIRST("QUALITY_FIRST", "质量优先"),
    
    /**
     * 平衡模式：综合考虑成本和性能
     */
    BALANCED("BALANCED", "平衡模式"),
    
    /**
     * 智能切换：根据问题复杂度自动选择
     */
    ADAPTIVE("ADAPTIVE", "自适应切换");
    
    private final String code;
    private final String description;
    
    ModelSelectionStrategy(String code, String description) {
        this.code = code;
        this.description = description;
    }
    
    public String getCode() {
        return code;
    }
    
    public String getDescription() {
        return description;
    }
}
