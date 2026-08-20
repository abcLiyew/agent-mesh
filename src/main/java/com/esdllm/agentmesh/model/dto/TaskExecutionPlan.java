package com.esdllm.agentmesh.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * 任务执行计划
 * 用于在执行前向用户展示待办清单和执行步骤
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskExecutionPlan {
    
    /**
     * 任务ID（临时生成）
     */
    private String taskId;
    
    /**
     * 任务描述
     */
    private String taskDescription;
    
    /**
     * 待办步骤列表
     */
    private List<TaskStep> steps;
    
    /**
     * 预估总耗时（毫秒）
     */
    private Long estimatedDurationMs;
    
    /**
     * 是否需要用户确认
     */
    private Boolean requiresConfirmation;
    
    /**
     * 智能体ID
     */
    private Long agentId;
    
    /**
     * 用户ID
     */
    private Long userId;
    
    /**
     * 创建时间戳
     */
    private Long createdAt;
    
    /**
     * 扩展上下文（用于存储sessionId等额外信息）
     */
    private Map<String, Object> context;
    
    /**
     * 任务步骤
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TaskStep {
        
        /**
         * 步骤ID
         */
        private String stepId;
        
        /**
         * 步骤序号
         */
        private Integer stepNumber;
        
        /**
         * 步骤描述
         */
        private String description;
        
        /**
         * 步骤类型：INTENT_RECOGNITION, TOOL_CALL, AGENT_CALL, CONDITION, etc.
         */
        private String stepType;
        
        /**
         * 关联的工具ID或智能体ID
         */
        private Long resourceId;
        
        /**
         * 资源名称
         */
        private String resourceName;
        
        /**
         * 输入参数
         */
        private Object inputParams;
        
        /**
         * 预估耗时（毫秒）
         */
        private Long estimatedDurationMs;
        
        /**
         * 是否必须执行
         */
        private Boolean isRequired;
        
        /**
         * 依赖的步骤ID列表
         */
        private List<String> dependencies;
    }
}
