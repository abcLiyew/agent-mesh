package com.esdllm.agentmesh.service.agent.impl;


import cn.hutool.core.util.StrUtil;
import com.esdllm.agentmesh.emun.ModelSelectionStrategy;
import com.esdllm.agentmesh.model.domain.AiModel;
import com.esdllm.agentmesh.model.dto.ModelPerformanceMetrics;
import com.esdllm.agentmesh.repository.dao.AiModelDao;
import com.esdllm.agentmesh.service.agent.ModelStrategyManager;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 模型策略管理器实现类
 */
@Component
@Slf4j
public class ModelStrategyManagerImpl implements ModelStrategyManager {
    
    @Resource
    private AiModelDao aiModelDao;
    
    private static final Map<Long, ModelPerformanceMetrics> MODEL_PERFORMANCE_CACHE = new ConcurrentHashMap<>();
    
    private static final double COST_WEIGHT = 0.4;
    private static final double PERFORMANCE_WEIGHT = 0.4;
    private static final double QUALITY_WEIGHT = 0.2;
    
    @Override
    public AiModel selectOptimalModel(List<AiModel> availableModels, 
                                      ModelSelectionStrategy strategy,
                                      Integer queryComplexity,
                                      Double budgetConstraint) {
        if (availableModels == null || availableModels.isEmpty()) {
            log.warn("可用模型列表为空");
            return null;
        }
        
        log.info("开始选择模型，strategy: {}, complexity: {}, budget: {}", 
                strategy, queryComplexity, budgetConstraint);
        
        List<AiModel> filteredModels = filterByBudget(availableModels, budgetConstraint);
        if (filteredModels.isEmpty()) {
            log.warn("预算内无可用模型，使用最便宜的模型");
            return findCheapestModel(availableModels);
        }
        
        AiModel selectedModel = switch (strategy) {
            case COST_FIRST -> findCheapestModel(filteredModels);
            case PERFORMANCE_FIRST -> findFastestModel(filteredModels);
            case QUALITY_FIRST -> findBestQualityModel(filteredModels, queryComplexity);
            case BALANCED -> findBalancedModel(filteredModels);
            case ADAPTIVE -> selectAdaptiveModel(filteredModels, queryComplexity);
        };
        
        log.info("模型选择完成，selectedModelId: {}, modelName: {}", 
                selectedModel.getId(), selectedModel.getModelName());
        
        return selectedModel;
    }
    
    @Override
    public Integer evaluateQueryComplexity(String query, Boolean toolCallRequired) {
        if (StrUtil.isBlank(query)) {
            return 1;
        }
        
        int baseScore = 1;
        
        int wordCount = query.split("[\\s,，.。？！;；]").length;
        if (wordCount > 20) baseScore += 2;
        else if (wordCount > 10) baseScore += 1;
        
        if (query.contains("为什么") || query.contains("如何") || 
            query.contains("分析") || query.contains("比较")) {
            baseScore += 2;
        }
        
        if (query.contains("代码") || query.contains("算法") || 
            query.contains("数学") || query.contains("物理")) {
            baseScore += 2;
        }
        
        if (toolCallRequired != null && toolCallRequired) {
            baseScore += 2;
        }
        
        return Math.min(baseScore, 10);
    }
    
    @Override
    public ModelPerformanceMetrics getModelPerformance(Long modelId) {
        return MODEL_PERFORMANCE_CACHE.computeIfAbsent(modelId, k -> {
            AiModel model = aiModelDao.getById(k);
            if (model == null) {
                return createDefaultMetrics(k);
            }
            
            ModelPerformanceMetrics metrics = new ModelPerformanceMetrics();
            metrics.setModelId(k);
            metrics.setInputCostPer1k(model.getInputCostPer1k());
            metrics.setOutputCostPer1k(model.getOutputCostPer1k());
            metrics.setContextWindow(model.getContextWindow());
            metrics.setSuccessRate(0.95);
            metrics.setAvgResponseTimeMs(1000.0);
            metrics.setPerformanceScore(75.0);
            
            return metrics;
        });
    }
    
    @Override
    public void updateModelStats(Long modelId, long responseTimeMs, boolean success, 
                                int inputTokens, int outputTokens) {
        ModelPerformanceMetrics metrics = getModelPerformance(modelId);
        
        double alpha = 0.1;
        double currentAvg = metrics.getAvgResponseTimeMs();
        metrics.setAvgResponseTimeMs(currentAvg * (1 - alpha) + responseTimeMs * alpha);
        
        double currentSuccessRate = metrics.getSuccessRate();
        double newSuccessValue = success ? 1.0 : 0.0;
        metrics.setSuccessRate(currentSuccessRate * (1 - alpha) + newSuccessValue * alpha);
        
        double score = calculatePerformanceScore(metrics);
        metrics.setPerformanceScore(score);
        
        MODEL_PERFORMANCE_CACHE.put(modelId, metrics);
        
        log.debug("模型性能更新，modelId: {}, responseTime: {}ms, success: {}, score: {}", 
                modelId, responseTimeMs, success, score);
    }
    
    @Override
    public ModelSelectionStrategy recommendStrategy(Double budget, Integer urgency) {
        if (budget == null || budget <= 0) {
            return ModelSelectionStrategy.COST_FIRST;
        }
        
        if (urgency != null && urgency >= 8) {
            return ModelSelectionStrategy.PERFORMANCE_FIRST;
        }
        
        if (budget.compareTo(10.0) >= 0) {
            return ModelSelectionStrategy.BALANCED;
        }
        
        return ModelSelectionStrategy.ADAPTIVE;
    }
    
    private List<AiModel> filterByBudget(List<AiModel> models, Double budgetConstraint) {
        if (budgetConstraint == null || budgetConstraint <= 0) {
            return models;
        }
        
        BigDecimal maxBudget = new BigDecimal(budgetConstraint.toString());
        
        return models.stream()
            .filter(model -> {
                if (model.getInputCostPer1k() == null || model.getOutputCostPer1k() == null) {
                    return true;
                }
                BigDecimal estimatedCost = model.getInputCostPer1k()
                    .add(model.getOutputCostPer1k())
                    .divide(new BigDecimal("1000"), RoundingMode.HALF_UP);
                return estimatedCost.compareTo(maxBudget) <= 0;
            })
            .toList();
    }
    
    private AiModel findCheapestModel(List<AiModel> models) {
        return models.stream()
            .min(Comparator.comparing(m -> {
                if (m.getInputCostPer1k() == null || m.getOutputCostPer1k() == null) {
                    return BigDecimal.ZERO;
                }
                return m.getInputCostPer1k().add(m.getOutputCostPer1k());
            }))
            .orElse(models.get(0));
    }
    
    private AiModel findFastestModel(List<AiModel> models) {
        return models.stream()
            .min(Comparator.comparing(m -> {
                ModelPerformanceMetrics metrics = getModelPerformance(m.getId());
                return metrics.getAvgResponseTimeMs();
            }))
            .orElse(models.get(0));
    }
    
    private AiModel findBestQualityModel(List<AiModel> models, Integer queryComplexity) {
        if (queryComplexity == null || queryComplexity <= 3) {
            return models.get(0);
        }
        
        return models.stream()
            .max(Comparator.comparing(m -> {
                ModelPerformanceMetrics metrics = getModelPerformance(m.getId());
                double score = metrics.getPerformanceScore();
                
                if (m.getContextWindow() != null && m.getContextWindow() > 8000) {
                    score += 10;
                }
                
                return score;
            }))
            .orElse(models.get(0));
    }
    
    private AiModel findBalancedModel(List<AiModel> models) {
        return models.stream()
            .max(Comparator.comparing(m -> {
                ModelPerformanceMetrics metrics = getModelPerformance(m.getId());
                
                double costScore = calculateCostScore(m);
                double perfScore = metrics.getPerformanceScore();
                
                return costScore * COST_WEIGHT + perfScore * PERFORMANCE_WEIGHT;
            }))
            .orElse(models.get(0));
    }
    
    private AiModel selectAdaptiveModel(List<AiModel> models, Integer queryComplexity) {
        if (queryComplexity == null) {
            queryComplexity = 5;
        }
        
        if (queryComplexity <= 3) {
            log.debug("简单问题，选择经济型模型");
            return findCheapestModel(models);
        } else if (queryComplexity <= 6) {
            log.debug("中等复杂度，选择平衡型模型");
            return findBalancedModel(models);
        } else {
            log.debug("高复杂度问题，选择高质量模型");
            return findBestQualityModel(models, queryComplexity);
        }
    }
    
    private double calculateCostScore(AiModel model) {
        if (model.getInputCostPer1k() == null || model.getOutputCostPer1k() == null) {
            return 50.0;
        }
        
        BigDecimal totalCost = model.getInputCostPer1k().add(model.getOutputCostPer1k());
        
        if (totalCost.compareTo(BigDecimal.ZERO) <= 0) {
            return 100.0;
        }
        
        return Math.max(0, 100 - totalCost.doubleValue() * 10);
    }
    
    private double calculatePerformanceScore(ModelPerformanceMetrics metrics) {
        double responseTimeScore = Math.max(0, 100 - (metrics.getAvgResponseTimeMs() / 100));
        double successScore = metrics.getSuccessRate() * 100;
        
        return responseTimeScore * 0.6 + successScore * 0.4;
    }
    
    private ModelPerformanceMetrics createDefaultMetrics(Long modelId) {
        ModelPerformanceMetrics metrics = new ModelPerformanceMetrics();
        metrics.setModelId(modelId);
        metrics.setAvgResponseTimeMs(1000.0);
        metrics.setSuccessRate(0.95);
        metrics.setPerformanceScore(75.0);
        return metrics;
    }
}
