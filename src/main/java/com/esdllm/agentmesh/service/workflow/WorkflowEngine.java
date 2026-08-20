package com.esdllm.agentmesh.service.workflow;

import com.esdllm.agentmesh.model.dto.workflow.WorkflowDefinition;
import com.esdllm.agentmesh.model.dto.workflow.WorkflowExecutionResult;

import java.util.Map;

/**
 * 工作流引擎接口
 */
public interface WorkflowEngine {
    
    /**
     * 执行工作流
     * @param workflowDefinition 工作流定义
     * @param inputParams 输入参数
     * @param userId 用户 ID
     * @return 执行结果
     */
    WorkflowExecutionResult execute(WorkflowDefinition workflowDefinition, 
                                    Map<String, Object> inputParams, 
                                    Long userId);
    
    /**
     * 异步执行工作流
     * @param workflowDefinition 工作流定义
     * @param inputParams 输入参数
     * @param userId 用户 ID
     */
    void executeAsync(WorkflowDefinition workflowDefinition, 
                     Map<String, Object> inputParams, 
                     Long userId);
    
    /**
     * 验证工作流定义
     * @param workflowDefinition 工作流定义
     * @return 是否有效
     */
    boolean validateWorkflow(WorkflowDefinition workflowDefinition);
}
