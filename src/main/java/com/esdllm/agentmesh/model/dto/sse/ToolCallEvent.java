package com.esdllm.agentmesh.model.dto.sse;


import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * 工具调用事件
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "工具调用事件")
public class ToolCallEvent {
    
    /**
     * 步骤 ID（用于前端追踪）
     */
    @Schema(description = "步骤 ID")
    private String stepId;
    
    @Schema(description = "工具 ID")
    private Long toolId;
    
    @Schema(description = "工具名称")
    private String toolName;
    
    @Schema(description = "工具类型")
    private String toolType;
    
    @Schema(description = "调用状态：STARTED, COMPLETED, FAILED")
    private String status;
    
    @Schema(description = "输入参数")
    private Map<String, Object> parameters;
    
    @Schema(description = "输出结果")
    private Object result;
    
    @Schema(description = "执行耗时 (毫秒)")
    private Long durationMs;
    
    @Schema(description = "错误信息")
    private String errorMessage;
}
