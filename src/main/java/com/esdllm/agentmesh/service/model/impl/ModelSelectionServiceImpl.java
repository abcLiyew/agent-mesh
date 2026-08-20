package com.esdllm.agentmesh.service.model.impl;

import com.esdllm.agentmesh.common.ErrorCode;
import com.esdllm.agentmesh.exception.BusinessException;
import com.esdllm.agentmesh.model.domain.AiModel;
import com.esdllm.agentmesh.repository.dao.AiModelDao;
import com.esdllm.agentmesh.service.model.ModelSelectionService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 模型选择服务实现
 * 基于成本、性能、任务复杂度的自适应模型路由
 */
@Service
@Slf4j
public class ModelSelectionServiceImpl implements ModelSelectionService {
    
    @Resource
    private AiModelDao aiModelDao;
    
    // 意图类型到模型类型的映射
    private static final Map<String, String> INTENT_TO_MODEL_TYPE = new HashMap<>();
    static {
        INTENT_TO_MODEL_TYPE.put("CHAT", "CHAT");
        INTENT_TO_MODEL_TYPE.put("PRODUCT_QUERY", "CHAT");
        INTENT_TO_MODEL_TYPE.put("ORDER_QUERY", "CHAT");
        INTENT_TO_MODEL_TYPE.put("KNOWLEDGE_QA", "CHAT");
        INTENT_TO_MODEL_TYPE.put("TOOL_CALL", "CHAT");
        INTENT_TO_MODEL_TYPE.put("AGENT_CALL", "CHAT");
        INTENT_TO_MODEL_TYPE.put("EMBEDDING", "EMBEDDING");
    }
    
    @Override
    public AiModel selectOptimalModel(Long userId, int taskComplexity, 
                                      Double budgetConstraint, Long latencyRequirement) {
        
        log.info("开始选择最优模型，userId: {}, 复杂度: {}, 预算: {}, 延迟要求: {}ms",
                userId, taskComplexity, budgetConstraint, latencyRequirement);
        
        // 1. 获取用户可用的所有聊天模型
        List<AiModel> availableModels = getAvailableChatModels(userId);
        
        if (availableModels.isEmpty()) {
            throw new BusinessException(ErrorCode.MODEL_NOT_FOUND, "没有可用的模型");
        }
        
        // 2. 根据约束条件过滤
        List<AiModel> filteredModels = filterModelsByConstraints(
            availableModels, budgetConstraint, latencyRequirement
        );
        
        if (filteredModels.isEmpty()) {
            log.warn("没有满足约束条件的模型，使用默认模型");
            return aiModelDao.getDefaultChatModel(userId);
        }
        
        // 3. 根据任务复杂度选择策略
        SelectionStrategy strategy = determineStrategy(taskComplexity);
        
        log.info("选择策略: {}", strategy);
        
        // 4. 评分并选择最优模型
        AiModel selectedModel = scoreAndSelectModel(filteredModels, taskComplexity, strategy);
        
        log.info("最终选择模型: {}, 策略: {}", selectedModel.getModelName(), strategy);
        
        return selectedModel;
    }
    
    @Override
    public AiModel selectModelByIntentType(Long userId, String intentType) {
        if (intentType == null || intentType.isEmpty()) {
            return aiModelDao.getDefaultChatModel(userId);
        }
        
        // 根据意图类型确定模型类型
        String modelType = INTENT_TO_MODEL_TYPE.getOrDefault(intentType, "CHAT");
        
        // 获取该类型的模型
        List<AiModel> models = aiModelDao.lambdaQuery()
            .eq(AiModel::getUserId, userId)
            .eq(AiModel::getModelType, modelType)
            .eq(AiModel::getIsActive, true)
            .eq(AiModel::getIsDelete, 0)
            .list();
        
        if (models.isEmpty()) {
            log.warn("未找到{}类型的模型，使用默认模型", modelType);
            return aiModelDao.getDefaultChatModel(userId);
        }
        
        // 简单场景使用小模型，复杂场景使用大模型
        boolean isComplexIntent = isComplexIntent(intentType);
        
        if (isComplexIntent) {
            // 复杂意图：选择上下文窗口最大的模型
            return models.stream()
                .max(Comparator.comparing(m -> m.getContextWindow() != null ? m.getContextWindow() : 0))
                .orElse(models.get(0));
        } else {
            // 简单意图：选择成本最低的模型
            return models.stream()
                .min(Comparator.comparing(m -> {
                    BigDecimal cost = m.getInputCostPer1k();
                    return cost != null ? cost : BigDecimal.ZERO;
                }))
                .orElse(models.get(0));
        }
    }
    
    @Override
    public double evaluateCostPerformance(AiModel model, int taskComplexity) {
        if (model == null) {
            return 0.0;
        }
        
        // 计算成本分(0-100,越低越便宜得分越高)
        double avgCost = calculateAverageCost(model);
        double costScore = Math.max(0, 100 - (avgCost * 10)); // 假设平均成本10元为基准
        
        // 计算性能分(0-100,基于上下文窗口和最大token)
        int contextWindow = model.getContextWindow() != null ? model.getContextWindow() : 0;
        int maxTokens = model.getMaxTokens() != null ? model.getMaxTokens() : 0;
        double performanceScore = Math.min(100, (contextWindow / 1000.0) * 50 + (maxTokens / 1000.0) * 50);
        
        // 根据任务复杂度调整权重
        double complexityWeight = taskComplexity / 10.0; // 0.1-1.0
        
        // 综合评分 = 性能分 * 复杂度权重 + 成本分 * (1-复杂度权重)
        double totalScore = performanceScore * complexityWeight + costScore * (1 - complexityWeight);
        
        log.debug("模型 {} 性价比评分: {:.2f} (性能: {:.2f}, 成本: {:.2f})",
                model.getModelName(), totalScore, performanceScore, costScore);
        
        return totalScore;
    }
    
    // ========== 私有方法 ==========
    
    /**
     * 获取用户可用的聊天模型列表
     */
    private List<AiModel> getAvailableChatModels(Long userId) {
        return aiModelDao.lambdaQuery()
            .eq(AiModel::getUserId, userId)
            .eq(AiModel::getModelType, "CHAT")
            .eq(AiModel::getIsActive, true)
            .eq(AiModel::getIsDelete, 0)
            .list();
    }
    
    /**
     * 根据约束条件过滤模型
     */
    private List<AiModel> filterModelsByConstraints(List<AiModel> models, 
                                                     Double budgetConstraint,
                                                     Long latencyRequirement) {
        return models.stream()
            .filter(model -> {
                // 预算过滤
                if (budgetConstraint != null && budgetConstraint > 0) {
                    BigDecimal avgCost = calculateAverageCostBigDecimal(model);
                    if (avgCost.compareTo(new BigDecimal(budgetConstraint)) > 0) {
                        return false;
                    }
                }
                
                // TODO: 延迟过滤(需要历史性能数据)
                // 当前简化：假设所有模型都满足延迟要求
                
                return true;
            })
            .collect(Collectors.toList());
    }
    
    /**
     * 根据任务复杂度确定选择策略
     */
    private SelectionStrategy determineStrategy(int taskComplexity) {
        if (taskComplexity <= 3) {
            return SelectionStrategy.COST_FIRST;
        } else if (taskComplexity >= 8) {
            return SelectionStrategy.PERFORMANCE_FIRST;
        } else if (taskComplexity >= 6) {
            return SelectionStrategy.BALANCED;
        } else {
            return SelectionStrategy.ADAPTIVE;
        }
    }
    
    /**
     * 评分并选择最优模型
     */
    private AiModel scoreAndSelectModel(List<AiModel> models, int taskComplexity, 
                                        SelectionStrategy strategy) {
        
        List<ModelScore> scoredModels = models.stream()
            .map(model -> calculateModelScore(model, taskComplexity, strategy))
            .sorted((a, b) -> Double.compare(b.getTotalScore(), a.getTotalScore()))
            .collect(Collectors.toList());
        
        if (scoredModels.isEmpty()) {
            return models.get(0);
        }
        
        // 返回评分最高的模型
        ModelScore bestScore = scoredModels.get(0);
        log.info("模型评分结果: {}", bestScore.getReason());
        
        return models.stream()
            .filter(m -> m.getId().equals(bestScore.getModelId()))
            .findFirst()
            .orElse(models.get(0));
    }
    
    /**
     * 计算模型综合评分
     */
    private ModelScore calculateModelScore(AiModel model, int taskComplexity, 
                                           SelectionStrategy strategy) {
        
        // 成本评分(0-100)
        double avgCost = calculateAverageCost(model);
        double costScore = Math.max(0, 100 - (avgCost * 5));
        
        // 性能评分(0-100)
        int contextWindow = model.getContextWindow() != null ? model.getContextWindow() : 0;
        int maxTokens = model.getMaxTokens() != null ? model.getMaxTokens() : 0;
        double performanceScore = Math.min(100, 
            (contextWindow / 32000.0) * 60 + (maxTokens / 8000.0) * 40
        );
        
        // 延迟评分(0-100,简化：小模型更快)
        double latencyScore = contextWindow < 8000 ? 90 : (contextWindow < 16000 ? 70 : 50);
        
        // 根据策略计算综合评分
        double totalScore;
        String reason;
        
        switch (strategy) {
            case COST_FIRST:
                totalScore = costScore * 0.7 + performanceScore * 0.2 + latencyScore * 0.1;
                reason = String.format("成本优先: 成本分%.1f, 性能分%.1f", costScore, performanceScore);
                break;
                
            case PERFORMANCE_FIRST:
                totalScore = costScore * 0.1 + performanceScore * 0.7 + latencyScore * 0.2;
                reason = String.format("性能优先: 性能分%.1f, 成本分%.1f", performanceScore, costScore);
                break;
                
            case BALANCED:
                totalScore = costScore * 0.4 + performanceScore * 0.4 + latencyScore * 0.2;
                reason = String.format("平衡模式: 成本分%.1f, 性能分%.1f, 延迟分%.1f", 
                        costScore, performanceScore, latencyScore);
                break;
                
            case ADAPTIVE:
            default:
                // 根据任务复杂度动态调整权重
                double perfWeight = taskComplexity / 10.0;
                double costWeight = 1.0 - perfWeight;
                totalScore = costScore * costWeight + performanceScore * perfWeight;
                reason = String.format("自适应: 复杂度%d, 性能权重%.1f, 成本权重%.1f", 
                        taskComplexity, perfWeight, costWeight);
                break;
        }
        
        return ModelScore.builder()
            .modelId(model.getId())
            .modelName(model.getModelName())
            .totalScore(totalScore)
            .costScore(costScore)
            .performanceScore(performanceScore)
            .latencyScore(latencyScore)
            .reason(reason)
            .build();
    }
    
    /**
     * 计算平均成本(每次调用的预估成本)
     */
    private double calculateAverageCost(AiModel model) {
        BigDecimal inputCost = model.getInputCostPer1k();
        BigDecimal outputCost = model.getOutputCostPer1k();
        
        if (inputCost == null && outputCost == null) {
            return 0.0;
        }
        
        // 假设平均输入500 tokens,输出200 tokens
        BigDecimal avgInputCost = inputCost != null ? inputCost.multiply(new BigDecimal("0.5")) : BigDecimal.ZERO;
        BigDecimal avgOutputCost = outputCost != null ? outputCost.multiply(new BigDecimal("0.2")) : BigDecimal.ZERO;
        
        return avgInputCost.add(avgOutputCost).doubleValue();
    }
    
    /**
     * 计算平均成本(BigDecimal版本)
     */
    private BigDecimal calculateAverageCostBigDecimal(AiModel model) {
        BigDecimal inputCost = model.getInputCostPer1k();
        BigDecimal outputCost = model.getOutputCostPer1k();
        
        if (inputCost == null && outputCost == null) {
            return BigDecimal.ZERO;
        }
        
        BigDecimal avgInputCost = inputCost != null ? inputCost.multiply(new BigDecimal("0.5")) : BigDecimal.ZERO;
        BigDecimal avgOutputCost = outputCost != null ? outputCost.multiply(new BigDecimal("0.2")) : BigDecimal.ZERO;
        
        return avgInputCost.add(avgOutputCost);
    }
    
    /**
     * 判断是否为复杂意图
     */
    private boolean isComplexIntent(String intentType) {
        // 复杂意图需要更强的推理能力
        return "KNOWLEDGE_QA".equals(intentType) || 
               "TOOL_CALL".equals(intentType) || 
               "AGENT_CALL".equals(intentType);
    }
}
