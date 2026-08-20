package com.esdllm.agentmesh.model.dto.workflow;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * 工作流定义
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowDefinition {
    
    /**
     * 工作流 ID
     */
    private Long workflowId;
    
    /**
     * 工作流名称
     */
    private String workflowName;
    
    /**
     * 工作流描述
     */
    private String description;
    
    /**
     * 关联的智能体 ID
     */
    private Long agentId;
    
    /**
     * 工作流版本
     */
    private String version;
    
    /**
     * 节点列表
     */
    private List<WorkflowNode> nodes;
    
    /**
     * 起始节点 ID
     */
    private String startNodeId;
    
    /**
     * 全局变量 (在工作流执行过程中共享)
     */
    private Map<String, Object> globalVariables;
    
    /**
     * 超时时间 (毫秒)
     */
    private Long timeoutMs;
    
    /**
     * 是否启用
     */
    private Boolean enabled;
    
    /**
     * 创建者用户 ID
     */
    private Long userId;
}
