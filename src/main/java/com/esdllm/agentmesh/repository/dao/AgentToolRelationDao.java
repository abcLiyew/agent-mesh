package com.esdllm.agentmesh.repository.dao;

import com.esdllm.agentmesh.model.domain.AgentToolRelation;
import com.baomidou.mybatisplus.extension.service.IService;

/**
* @author LiYehe
* @description 针对表【agent_tool_relation(智能体 - 工具关联表：定义某个智能体可以使用哪些工具 (多对多关系))】的数据库操作Service
* @createDate 2026-03-09 13:26:58
*/
public interface AgentToolRelationDao extends IService<AgentToolRelation> {

}
