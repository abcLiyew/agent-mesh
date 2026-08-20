package com.esdllm.agentmesh.model.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import jakarta.validation.constraints.NotEmpty;
import java.util.List;

/**
 * 批量更新智能体配置请求
 */
@Data
@Schema(description = "批量更新智能体配置请求")
public class BatchUpdateAgentsRequest {
    
    @NotEmpty(message = "智能体 ID 列表不能为空")
    @Schema(description = "智能体 ID 列表", required = true, example = "[1, 2, 3]")
    private List<Long> agentIds;
    
    @Schema(description = "要更新的配置字段")
    private AgentUpdateConfig updateConfig;
    
    /**
     * 智能体更新配置
     */
    @Data
    @Schema(description = "智能体更新配置")
    public static class AgentUpdateConfig {
        
        @Schema(description = "智能体状态：1=发布，0=草稿", example = "1")
        private Integer status;
        
        @Schema(description = "是否启用工具", example = "true")
        private Boolean isToolEnabled;
        
        @Schema(description = "模型选择策略", example = "BALANCED")
        private String modelSelectionStrategy;
        
        @Schema(description = "预算约束", example = "0.5")
        private Double budgetConstraint;
    }
}
