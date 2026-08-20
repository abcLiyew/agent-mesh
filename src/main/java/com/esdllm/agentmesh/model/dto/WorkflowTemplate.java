package com.esdllm.agentmesh.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * 工作流模板定义
 * 支持用户自定义、半自定义和AI辅助生成
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowTemplate {
    
    /**
     * 模板ID
     */
    private Long templateId;
    
    /**
     * 模板名称
     */
    private String templateName;
    
    /**
     * 任务描述（用于AI辅助生成工作流）
     */
    private String taskDescription;
    
    /**
     * 模板描述
     */
    private String description;
    
    /**
     * 工作流模式：FULL_AUTO(全自动), SEMI_CUSTOM(半自定义), FULL_CUSTOM(完全自定义)
     */
    private String workflowMode;
    
    /**
     * 用户定义的节点列表（半自定义和完全自定义模式使用）
     */
    private List<TemplateNode> userDefinedNodes;
    
    /**
     * AI自动填充的节点列表（半自定义模式使用）
     */
    private List<TemplateNode> aiGeneratedNodes;
    
    /**
     * 完整的节点列表（合并后的结果）
     */
    private List<TemplateNode> allNodes;
    
    /**
     * 起始节点ID
     */
    private String startNodeId;
    
    /**
     * 智能体ID
     */
    private Long agentId;
    
    /**
     * 创建者用户ID
     */
    private Long userId;
    
    /**
     * 是否公开模板
     */
    private Boolean isPublic;
    
    /**
     * 使用次数
     */
    private Integer usageCount;
    
    /**
     * 评分（1-5）
     */
    private Double rating;
    
    /**
     * 标签（JSON数组）
     */
    private Object tagsJson;
    
    /**
     * 创建时间
     */
    private Long createdAt;
    
    /**
     * 更新时间
     */
    private Long updatedAt;
    
    /**
     * 模板节点
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TemplateNode {
        
        /**
         * 节点ID
         */
        private String nodeId;
        
        /**
         * 节点名称
         */
        private String nodeName;
        
        /**
         * 节点类型：TOOL_CALL, AGENT_CALL, CONDITION, SEQUENCE, PARALLEL等
         */
        private String nodeType;
        
        /**
         * 资源ID（工具ID或智能体ID）
         */
        private Long resourceId;
        
        /**
         * 资源名称
         */
        private String resourceName;
        
        /**
         * 输入参数模板
         */
        private Map<String, Object> inputParamsTemplate;
        
        /**
         * 条件表达式（条件节点使用）
         */
        private String conditionExpression;
        
        /**
         * 子节点ID列表（复合节点使用）
         */
        private List<String> childNodeIds;
        
        /**
         * 下一个节点ID
         */
        private String nextNodeId;
        
        /**
         * 是否为AI自动生成
         */
        private Boolean isAiGenerated;
        
        /**
         * 是否可编辑（半自定义模式中，false表示AI生成的不可编辑）
         */
        private Boolean isEditable;
        
        /**
         * 节点描述
         */
        private String description;
        
        /**
         * 超时时间（毫秒）
         */
        private Long timeoutMs;
        
        /**
         * 错误处理策略：FAIL_FAST, CONTINUE, RETRY
         */
        private String errorStrategy;
    }
}
