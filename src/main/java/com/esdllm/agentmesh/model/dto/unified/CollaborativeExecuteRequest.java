package com.esdllm.agentmesh.model.dto.unified;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.Map;

/**
 * 多智能体协同执行请求DTO
 */
@Data
@Schema(description = "多智能体协同执行请求")
public class CollaborativeExecuteRequest {
    
    @Schema(description = "主智能体ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long agentId;
    
    @Schema(description = "用户查询/任务描述", requiredMode = Schema.RequiredMode.REQUIRED)
    private String query;
    
    @Schema(description = "上下文参数（可选）")
    private Map<String, Object> context;
}
