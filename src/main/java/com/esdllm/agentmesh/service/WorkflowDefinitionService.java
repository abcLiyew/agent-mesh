package com.esdllm.agentmesh.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.esdllm.agentmesh.model.domain.WorkflowDefinitionEntity;

import java.util.List;
import java.util.Map;

/**
 * 工作流定义服务接口
 */
public interface WorkflowDefinitionService {
    
    /**
     * 创建工作流
     * @param workflow 工作流定义
     * @return 工作流ID
     */
    Long createWorkflow(WorkflowDefinitionEntity workflow);
    
    /**
     * 更新工作流
     * @param workflow 工作流定义
     * @return 是否成功
     */
    boolean updateWorkflow(WorkflowDefinitionEntity workflow);
    
    /**
     * 删除工作流（软删除）
     * @param workflowId 工作流ID
     * @return 是否成功
     */
    boolean deleteWorkflow(Long workflowId);
    
    /**
     * 获取工作流详情
     * @param workflowId 工作流ID
     * @return 工作流定义
     */
    WorkflowDefinitionEntity getWorkflowById(Long workflowId);
    
    /**
     * 获取用户的工作流列表
     * @param userId 用户ID
     * @param page 页码
     * @param pageSize 每页数量
     * @return 分页结果
     */
    Page<WorkflowDefinitionEntity> getUserWorkflows(Long userId, int page, int pageSize);
    
    /**
     * 获取智能体的工作流列表
     * @param agentId 智能体ID
     * @return 工作流列表
     */
    List<WorkflowDefinitionEntity> getAgentWorkflows(Long agentId);
    
    /**
     * 启用/禁用工作流
     * @param workflowId 工作流ID
     * @param enabled 是否启用
     * @return 是否成功
     */
    boolean toggleWorkflow(Long workflowId, boolean enabled);
}
