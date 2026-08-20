package com.esdllm.agentmesh.repository.dao.impl;

import com.esdllm.agentmesh.model.domain.AgentDependencyEntity;
import com.esdllm.agentmesh.repository.dao.AgentDependencyDao;
import com.esdllm.agentmesh.repository.mapper.AgentDependencyMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 智能体依赖关系 DAO 实现
 */
@Repository
public class AgentDependencyDaoImpl implements AgentDependencyDao {
    
    @Resource
    private AgentDependencyMapper agentDependencyMapper;
    
    @Override
    public boolean save(AgentDependencyEntity entity) {
        return agentDependencyMapper.insert(entity) > 0;
    }
    
    @Override
    public boolean delete(Long id) {
        return agentDependencyMapper.deleteById(id) > 0;
    }
    
    @Override
    public List<AgentDependencyEntity> listByAgentId(Long agentId) {
        return agentDependencyMapper.selectByAgentId(agentId);
    }
    
    @Override
    public List<AgentDependencyEntity> listByDependsOnAgentId(Long dependsOnAgentId) {
        return agentDependencyMapper.selectList(
            new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<AgentDependencyEntity>()
                .eq("depends_on_agent_id", dependsOnAgentId)
        );
    }
    
    @Override
    public AgentDependencyEntity getByPair(Long agentId, Long dependsOnAgentId) {
        return agentDependencyMapper.selectByPair(agentId, dependsOnAgentId);
    }
    
    @Override
    public boolean update(AgentDependencyEntity entity) {
        return agentDependencyMapper.updateById(entity) > 0;
    }
}
