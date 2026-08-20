package com.esdllm.agentmesh.model.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * 用户反馈请求 DTO
 */
@Data
@Schema(description = "用户反馈请求")
public class FeedbackRequest {
    
    @NotNull(message = "日志 ID 不能为空")
    @Schema(description = "对话日志 ID", required = true, example = "1")
    private Long logId;
    
    @NotNull(message = "评分不能为空")
    @Min(value = 1, message = "评分最小值为 1")
    @Max(value = 5, message = "评分最大值为 5")
    @Schema(description = "评分（1-5 星）", required = true, example = "5")
    private Integer rating;
    
    @Schema(description = "反馈备注", example = "回答非常准确，很有帮助！")
    private String feedback;
}
