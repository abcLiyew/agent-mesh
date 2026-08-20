package com.esdllm.agentmesh.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 反馈统计数据
 */
@Data
@Builder
@Schema(description = "反馈统计数据")
public class FeedbackStatistics {
    
    @Schema(description = "总反馈数量")
    private Long totalFeedbacks;
    
    @Schema(description = "平均评分")
    private Double averageRating;
    
    @Schema(description = "好评数（4-5 星）")
    private Long positiveFeedbacks;
    
    @Schema(description = "中评数（3 星）")
    private Long neutralFeedbacks;
    
    @Schema(description = "差评数（1-2 星）")
    private Long negativeFeedbacks;
    
    @Schema(description = "好评率（百分比）")
    private Double positiveRate;
    
    @Schema(description = "差评率（百分比）")
    private Double negativeRate;
    
    @Schema(description = "评分分布")
    private RatingDistribution distribution;
    
    @Schema(description = "低分反馈列表（用于问题分析）")
    private List<LowRatingFeedback> lowRatingFeedbacks;
    
    /**
     * 评分分布
     */
    @Data
    @Builder
    @Schema(description = "评分分布")
    public static class RatingDistribution {
        @Schema(description = "1 星数量")
        private Integer oneStar;
        
        @Schema(description = "2 星数量")
        private Integer twoStar;
        
        @Schema(description = "3 星数量")
        private Integer threeStar;
        
        @Schema(description = "4 星数量")
        private Integer fourStar;
        
        @Schema(description = "5 星数量")
        private Integer fiveStar;
    }
    
    /**
     * 低分反馈详情
     */
    @Data
    @Builder
    @Schema(description = "低分反馈详情")
    public static class LowRatingFeedback {
        @Schema(description = "日志 ID")
        private Long logId;
        
        @Schema(description = "用户 ID")
        private Long userId;
        
        @Schema(description = "智能体 ID")
        private Long agentId;
        
        @Schema(description = "用户问题")
        private String userQuery;
        
        @Schema(description = "系统回答")
        private String finalResponse;
        
        @Schema(description = "评分")
        private Integer rating;
        
        @Schema(description = "反馈备注")
        private String feedback;
        
        @Schema(description = "意图类型")
        private String intentType;
        
        @Schema(description = "错误信息")
        private String errorMessage;
        
        @Schema(description = "创建时间")
        private java.util.Date createdAt;
    }
}
