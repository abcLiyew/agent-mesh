package com.esdllm.agentmesh.service.model;

import com.esdllm.agentmesh.model.domain.AiModel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 模型选择服务接口
 * 实现基于成本、性能、场景的自适应模型路由
 */
public interface ModelSelectionService {
    
    /**
     * 根据任务复杂度选择最合适的模型
     * 
     * @param userId 用户ID
     * @param taskComplexity 任务复杂度(1-10)
     * @param budgetConstraint 预算约束(元)
     * @param latencyRequirement 延迟要求(ms)
     * @return 选中的模型
     */
    AiModel selectOptimalModel(Long userId, int taskComplexity, 
                               Double budgetConstraint, Long latencyRequirement);
    
    /**
     * 根据意图类型选择模型
     * 
     * @param userId 用户ID
     * @param intentType 意图类型
     * @return 选中的模型
     */
    AiModel selectModelByIntentType(Long userId, String intentType);
    
    /**
     * 评估模型的性价比
     * 
     * @param model 模型对象
     * @param taskComplexity 任务复杂度
     * @return 性价比评分(越高越好)
     */
    double evaluateCostPerformance(AiModel model, int taskComplexity);
    
    // ========== 内部类 ==========
    
    /**
     * 模型选择策略
     */
    enum SelectionStrategy {
        /**
         * 成本优先：选择最便宜的模型
         */
        COST_FIRST,
        
        /**
         * 性能优先：选择最强的模型
         */
        PERFORMANCE_FIRST,
        
        /**
         * 平衡模式：权衡成本和性能
         */
        BALANCED,
        
        /**
         * 自适应：根据任务复杂度动态选择
         */
        ADAPTIVE
    }
    
    /**
     * 模型评分
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    class ModelScore {
        /**
         * 模型ID
         */
        private Long modelId;
        
        /**
         * 模型名称
         */
        private String modelName;
        
        /**
         * 综合评分(0-100)
         */
        private double totalScore;
        
        /**
         * 成本评分(0-100,越高越便宜)
         */
        private double costScore;
        
        /**
         * 性能评分(0-100,越高越强)
         */
        private double performanceScore;
        
        /**
         * 延迟评分(0-100,越高越快)
         */
        private double latencyScore;
        
        /**
         * 推荐理由
         */
        private String reason;
    }
}
