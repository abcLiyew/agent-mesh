package com.esdllm.agentmesh.service;

import com.esdllm.agentmesh.model.domain.ConversationLog;
import com.esdllm.agentmesh.model.dto.FeedbackStatistics;

import java.util.List;
import java.util.Map;

/**
 * 反馈分析服务接口
 */
public interface FeedbackAnalysisService {

    /**
     * 分析智能体的反馈问题
     * @param agentId 智能体 ID
     * @return 问题分析报告
     */
    FeedbackAnalysisReport analyzeAgentFeedback(Long agentId);

    /**
     * 生成优化建议
     * @param agentId 智能体 ID
     * @return 优化建议列表
     */
    List<OptimizationSuggestion> generateOptimizationSuggestions(Long agentId);

    /**
     * 获取反馈关键词分析
     * @param agentId 智能体 ID
     * @return 关键词频率统计
     */
    Map<String, Integer> getFeedbackKeywords(Long agentId, int limit);

    /**
     * 反馈分析报告
     */
    record FeedbackAnalysisReport(
        Double averageRating,
        Long totalFeedbacks,
        String mainIssue,
        List<String> problemCategories,
        List<OptimizationSuggestion> suggestions
    ) {}

    /**
     * 优化建议
     */
    record OptimizationSuggestion(
        String category,
        String suggestion,
        String reason,
        Integer priority
    ) {}
}
