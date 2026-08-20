package com.esdllm.agentmesh.model.dto.unified;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.Map;

/**
 * 任务规划请求DTO
 */
@Data
@Schema(description = "任务规划请求")
public class TaskPlanRequest {
    
    @Schema(description = "智能体ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long agentId;
    
    @Schema(description = "用户查询/任务描述", requiredMode = Schema.RequiredMode.REQUIRED)
    private String query;
    
    @Schema(description = "上下文参数（可选）")
    private Map<String, Object> context;
    
    @Schema(description = "会话ID（可选，用于多轮对话关联）")
    private String sessionId;
}
