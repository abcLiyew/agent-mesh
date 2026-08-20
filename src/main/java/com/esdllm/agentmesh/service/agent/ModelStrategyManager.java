package com.esdllm.agentmesh.service.agent;


import com.esdllm.agentmesh.emun.ModelSelectionStrategy;
import com.esdllm.agentmesh.model.domain.AiModel;
import com.esdllm.agentmesh.model.dto.ModelPerformanceMetrics;

import java.util.List;
import java.util.Map;

/**
 * 模型策略管理器
 */
public interface ModelStrategyManager {
    
    /**
     * 根据策略选择最优模型
     * @param availableModels 可用模型列表
     * @param strategy 选择策略
     * @param queryComplexity 问题复杂度（1-10）
     * @param budgetConstraint 预算约束（元）
     * @return 选中的模型
     */
    AiModel selectOptimalModel(List<AiModel> availableModels, 
                               ModelSelectionStrategy strategy,
                               Integer queryComplexity,
                               Double budgetConstraint);
    
    /**
     * 评估问题复杂度
     * @param query 用户问题
     * @param toolCallRequired 是否需要工具调用
     * @return 复杂度评分（1-10）
     */
    Integer evaluateQueryComplexity(String query, Boolean toolCallRequired);
    
    /**
     * 获取模型性能指标
     * @param modelId 模型 ID
     * @return 性能指标
     */
    ModelPerformanceMetrics getModelPerformance(Long modelId);
    
    /**
     * 更新模型性能统计
     * @param modelId 模型 ID
     * @param responseTimeMs 响应时间
     * @param success 是否成功
     * @param inputTokens 输入 token 数
     * @param outputTokens 输出 token 数
     */
    void updateModelStats(Long modelId, long responseTimeMs, boolean success, 
                         int inputTokens, int outputTokens);
    
    /**
     * 获取推荐的选择策略
     * @param budget 预算
     * @param urgency 紧急程度（1-10）
     * @return 推荐的策略
     */
    ModelSelectionStrategy recommendStrategy(Double budget, Integer urgency);
}
