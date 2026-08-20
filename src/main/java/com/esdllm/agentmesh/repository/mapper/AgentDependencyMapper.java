package com.esdllm.agentmesh.repository.mapper;

import com.esdllm.agentmesh.model.domain.AgentDependencyEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 智能体依赖关系 Mapper
 */
public interface AgentDependencyMapper extends BaseMapper<AgentDependencyEntity> {
    
    /**
     * 根据智能体 ID 查询依赖列表
     * @param agentId 智能体 ID
     * @return 依赖列表
     */
    List<AgentDependencyEntity> selectByAgentId(@Param("agentId") Long agentId);
    
    /**
     * 检查依赖关系是否存在
     * @param agentId 智能体 ID
     * @param dependsOnAgentId 被依赖的智能体 ID
     * @return 是否存在
     */
    AgentDependencyEntity selectByPair(@Param("agentId") Long agentId, 
                                       @Param("dependsOnAgentId") Long dependsOnAgentId);
}
