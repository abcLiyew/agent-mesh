package com.esdllm.agentmesh.model.dto;

import lombok.Data;

/**
 * 工具使用统计信息
 */
@Data
public class ToolUsageStatistics {
    
    /**
     * 工具 ID
     */
    private Long toolId;
    
    /**
     * 工具名称
     */
    private String toolName;
    
    /**
     * 使用次数
     */
    private Long count;
    
    /**
     * 平均耗时（毫秒）
     */
    private Long averageDurationMs;
}
