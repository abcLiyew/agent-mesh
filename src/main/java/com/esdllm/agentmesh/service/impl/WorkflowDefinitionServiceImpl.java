package com.esdllm.agentmesh.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.esdllm.agentmesh.model.domain.WorkflowDefinitionEntity;
import com.esdllm.agentmesh.repository.mapper.WorkflowDefinitionMapper;
import com.esdllm.agentmesh.service.WorkflowDefinitionService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

/**
 * 工作流定义服务实现
 */
@Service
@Slf4j
public class WorkflowDefinitionServiceImpl implements WorkflowDefinitionService {
    
    @Resource
    private WorkflowDefinitionMapper workflowDefinitionMapper;
    
    @Override
    public Long createWorkflow(WorkflowDefinitionEntity workflow) {
        log.info("创建工作流: {}", workflow.getWorkflowName());
        
        // 设置默认值
        if (workflow.getEnabled() == null) {
            workflow.setEnabled(true);
        }
        if (workflow.getIsDelete() == null) {
            workflow.setIsDelete(0);
        }
        if (workflow.getVersion() == null) {
            workflow.setVersion("1.0.0");
        }
        if (workflow.getTimeoutMs() == null) {
            workflow.setTimeoutMs(30000L); // 默认30秒超时
        }
        
        workflow.setCreatedAt(new Date());
        workflow.setUpdatedAt(new Date());
        
        workflowDefinitionMapper.insert(workflow);
        
        log.info("工作流创建成功, ID: {}", workflow.getId());
        return workflow.getId();
    }
    
    @Override
    public boolean updateWorkflow(WorkflowDefinitionEntity workflow) {
        log.info("更新工作流: ID={}", workflow.getId());
        
        // 验证工作流是否存在
        WorkflowDefinitionEntity existing = workflowDefinitionMapper.selectById(workflow.getId());
        if (existing == null || existing.getIsDelete() == 1) {
            log.warn("工作流不存在或已删除: ID={}", workflow.getId());
            return false;
        }
        
        // 验证权限（只能修改自己的工作流）
        if (!existing.getUserId().equals(workflow.getUserId())) {
            log.warn("无权修改此工作流: workflowId={}, userId={}", workflow.getId(), workflow.getUserId());
            return false;
        }
        
        workflow.setUpdatedAt(new Date());
        
        int rows = workflowDefinitionMapper.updateById(workflow);
        log.info("工作流更新成功: ID={}", workflow.getId());
        return rows > 0;
    }
    
    @Override
    public boolean deleteWorkflow(Long workflowId) {
        log.info("删除工作流: ID={}", workflowId);
        
        WorkflowDefinitionEntity workflow = workflowDefinitionMapper.selectById(workflowId);
        if (workflow == null || workflow.getIsDelete() == 1) {
            log.warn("工作流不存在或已删除: ID={}", workflowId);
            return false;
        }
        
        // 软删除
        workflow.setIsDelete(1);
        workflow.setUpdatedAt(new Date());
        
        int rows = workflowDefinitionMapper.updateById(workflow);
        log.info("工作流删除成功: ID={}", workflowId);
        return rows > 0;
    }
    
    @Override
    public WorkflowDefinitionEntity getWorkflowById(Long workflowId) {
        WorkflowDefinitionEntity workflow = workflowDefinitionMapper.selectById(workflowId);
        
        if (workflow == null || workflow.getIsDelete() == 1) {
            log.warn("工作流不存在或已删除: ID={}", workflowId);
            return null;
        }
        
        return workflow;
    }
    
    @Override
    public Page<WorkflowDefinitionEntity> getUserWorkflows(Long userId, int page, int pageSize) {
        log.info("获取用户工作流列表: userId={}, page={}, pageSize={}", userId, page, pageSize);
        
        Page<WorkflowDefinitionEntity> pageParam = new Page<>(page, pageSize);
        
        LambdaQueryWrapper<WorkflowDefinitionEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(WorkflowDefinitionEntity::getUserId, userId)
               .eq(WorkflowDefinitionEntity::getIsDelete, 0)
               .orderByDesc(WorkflowDefinitionEntity::getUpdatedAt);
        
        Page<WorkflowDefinitionEntity> result = workflowDefinitionMapper.selectPage(pageParam, wrapper);
        
        log.info("查询到 {} 个工作流", result.getTotal());
        return result;
    }
    
    @Override
    public List<WorkflowDefinitionEntity> getAgentWorkflows(Long agentId) {
        log.info("获取智能体工作流列表: agentId={}", agentId);
        
        LambdaQueryWrapper<WorkflowDefinitionEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(WorkflowDefinitionEntity::getAgentId, agentId)
               .eq(WorkflowDefinitionEntity::getIsDelete, 0)
               .eq(WorkflowDefinitionEntity::getEnabled, true)
               .orderByDesc(WorkflowDefinitionEntity::getUpdatedAt);
        
        List<WorkflowDefinitionEntity> workflows = workflowDefinitionMapper.selectList(wrapper);
        
        log.info("查询到 {} 个启用的工作流", workflows.size());
        return workflows;
    }
    
    @Override
    public boolean toggleWorkflow(Long workflowId, boolean enabled) {
        log.info("{}工作流: ID={}", enabled ? "启用" : "禁用", workflowId);
        
        WorkflowDefinitionEntity workflow = workflowDefinitionMapper.selectById(workflowId);
        if (workflow == null || workflow.getIsDelete() == 1) {
            log.warn("工作流不存在或已删除: ID={}", workflowId);
            return false;
        }
        
        workflow.setEnabled(enabled);
        workflow.setUpdatedAt(new Date());
        
        int rows = workflowDefinitionMapper.updateById(workflow);
        log.info("工作流{}成功: ID={}", enabled ? "启用" : "禁用", workflowId);
        return rows > 0;
    }
}

