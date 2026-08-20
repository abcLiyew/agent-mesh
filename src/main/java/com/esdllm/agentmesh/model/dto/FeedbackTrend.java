package com.esdllm.agentmesh.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * 反馈趋势数据
 */
@Data
@Builder
@Schema(description = "反馈趋势数据")
public class FeedbackTrend {
    
    @Schema(description = "日期")
    private String date;
    
    @Schema(description = "反馈数量")
    private Integer feedbackCount;
    
    @Schema(description = "平均评分")
    private Double averageRating;
    
    @Schema(description = "好评数量")
    private Integer positiveCount;
    
    @Schema(description = "差评数量")
    private Integer negativeCount;
}
