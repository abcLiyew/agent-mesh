package com.esdllm.agentmesh.repository.dao;

import com.esdllm.agentmesh.model.domain.AgentKbRelation;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
* @author LiYehe
* @description 针对表【agent_kb_relation(智能体 - 知识库关联表)】的数据库操作 Service
* @createDate 2026-03-10
*/
public interface AgentKbRelationDao extends IService<AgentKbRelation> {

    /**
     * 根据智能体 ID 查询关联列表
     * @param agentId 智能体 ID
     * @return 关联列表
     */
    List<AgentKbRelation> getByAgentId(Long agentId);

    /**
     * 根据智能体 ID 和知识库 ID 查询关联
     * @param agentId 智能体 ID
     * @param kbId 知识库 ID
     * @return 关联信息
     */
    AgentKbRelation getByAgentIdAndKbId(Long agentId, Long kbId);

    /**
     * 根据用户 ID 查询关联列表
     * @param userId 用户 ID
     * @return 关联列表
     */
    List<AgentKbRelation> getByUserId(Long userId);
}
