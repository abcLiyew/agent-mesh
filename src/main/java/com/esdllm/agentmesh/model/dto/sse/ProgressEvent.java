package com.esdllm.agentmesh.model.dto.sse;


import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 流式进度事件
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "流式处理进度")
public class ProgressEvent {
    
    @Schema(description = "当前步骤")
    private Integer currentStep;
    
    @Schema(description = "总步骤数")
    private Integer totalSteps;
    
    @Schema(description = "进度百分比 (0-100)")
    private Double progress;
    
    @Schema(description = "当前步骤描述")
    private String stepDescription;
    
    @Schema(description = "预计剩余时间 (毫秒)")
    private Long estimatedRemainingMs;
}
