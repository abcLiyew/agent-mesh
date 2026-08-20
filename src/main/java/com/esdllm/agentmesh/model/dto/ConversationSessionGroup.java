package com.esdllm.agentmesh.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;
import java.util.List;

/**
 * 会话组DTO - 用于按sessionId分组返回对话历史
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConversationSessionGroup {
    
    /**
     * 会话ID
     */
    private String sessionId;
    
    /**
     * 用户ID
     */
    private Long userId;
    
    /**
     * 智能体ID
     */
    private Long agentId;
    
    /**
     * 会话中的对话轮数
     */
    private Integer conversationCount;
    
    /**
     * 第一轮对话时间（会话开始时间）
     */
    private Date firstConversationTime;
    
    /**
     * 最后一轮对话时间（会话最后更新时间）
     */
    private Date lastConversationTime;
    
    /**
     * 第一轮的用户提问（作为会话标题/摘要）
     */
    private String firstQuery;
    
    /**
     * 会话标题（由AI生成的简洁标题）
     */
    private String sessionTitle;
    
    /**
     * 最后一轮的AI回复预览
     */
    private String lastResponsePreview;
    
    /**
     * 会话状态（基于最后一轮的状态）
     */
    private Integer status;
    
    /**
     * 用户评分（如果有）
     */
    private Integer userRating;
    
    /**
     * 该会话下的所有对话记录列表
     */
    private List<ConversationLogDetail> conversations;
    
    /**
     * 对话记录详情（简化版）
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ConversationLogDetail {
        private Long id;
        private String userQuery;
        private String finalResponse;
        private Integer status;
        private Long executionTimeMs;
        private Date createdAt;
        private Integer userRating;
    }
}
