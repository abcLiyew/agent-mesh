package com.esdllm.agentmesh.repository.dao.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.esdllm.agentmesh.model.domain.AgentToolRelation;
import com.esdllm.agentmesh.repository.dao.AgentToolRelationDao;
import com.esdllm.agentmesh.repository.mapper.AgentToolRelationMapper;
import org.springframework.stereotype.Service;

/**
* @author LiYehe
* @description 针对表【agent_tool_relation(智能体 - 工具关联表：定义某个智能体可以使用哪些工具 (多对多关系))】的数据库操作Service实现
* @createDate 2026-03-09 13:34:38
*/
@Service
public class AgentToolRelationDaoImpl extends ServiceImpl<AgentToolRelationMapper, AgentToolRelation>
    implements AgentToolRelationDao{

}




