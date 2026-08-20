package com.esdllm.agentmesh.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 系统后台管理统计信息
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardStatistics {
    
    /**
     * 用户统计
     */
    private UserStatistics userStatistics;
    
    /**
     * 智能体统计
     */
    private AgentStatistics agentStatistics;
    
    /**
     * 工具统计
     */
    private ToolStatistics toolStatistics;
    
    /**
     * 知识库统计
     */
    private KnowledgeBaseStatistics knowledgeBaseStatistics;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserStatistics {
        /**
         * 总用户数
         */
        private Long totalUsers;
        
        /**
         * 今日新增用户数
         */
        private Long todayNewUsers;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AgentStatistics {
        /**
         * 智能体总数
         */
        private Long totalAgents;
        
        /**
         * 已发布公开的智能体数
         */
        private Long publishedAgents;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ToolStatistics {
        /**
         * 工具总数
         */
        private Long totalTools;
        
        /**
         * 公开可用工具数
         */
        private Long publicTools;
        
        /**
         * 健康工具数
         */
        private Long healthyTools;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class KnowledgeBaseStatistics {
        /**
         * 知识库总数
         */
        private Long totalKnowledgeBases;
        
        /**
         * 文档总数
         */
        private Long totalDocuments;
        
        /**
         * 向量化文档数
         */
        private Long vectorizedDocuments;
    }
}
