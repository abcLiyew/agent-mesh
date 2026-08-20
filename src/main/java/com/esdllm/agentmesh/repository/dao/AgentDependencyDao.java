package com.esdllm.agentmesh.repository.dao;

import com.esdllm.agentmesh.model.domain.AgentDependencyEntity;

import java.util.List;

/**
 * 智能体依赖关系 DAO
 */
public interface AgentDependencyDao {
    
    /**
     * 保存依赖关系
     */
    boolean save(AgentDependencyEntity entity);
    
    /**
     * 删除依赖关系
     */
    boolean delete(Long id);
    
    /**
     * 根据智能体 ID 查询依赖列表
     */
    List<AgentDependencyEntity> listByAgentId(Long agentId);
    
    /**
     * 根据智能体 ID 对查询依赖列表
     */
    List<AgentDependencyEntity> listByDependsOnAgentId(Long dependsOnAgentId);
    
    /**
     * 查询指定的依赖关系
     */
    AgentDependencyEntity getByPair(Long agentId, Long dependsOnAgentId);
    
    /**
     * 更新依赖关系
     */
    boolean update(AgentDependencyEntity entity);
}
