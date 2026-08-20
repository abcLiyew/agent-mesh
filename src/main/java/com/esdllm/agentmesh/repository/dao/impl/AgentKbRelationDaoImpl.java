package com.esdllm.agentmesh.repository.dao.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.esdllm.agentmesh.model.domain.AgentKbRelation;
import com.esdllm.agentmesh.repository.dao.AgentKbRelationDao;
import com.esdllm.agentmesh.repository.mapper.AgentKbRelationMapper;
import org.springframework.stereotype.Service;

import java.util.List;

/**
* @author LiYehe
* @description 针对表【agent_kb_relation(智能体 - 知识库关联表)】的数据库操作 Service 实现
* @createDate 2026-03-10
*/
@Service
public class AgentKbRelationDaoImpl extends ServiceImpl<AgentKbRelationMapper, AgentKbRelation>
    implements AgentKbRelationDao {

    @Override
    public List<AgentKbRelation> getByAgentId(Long agentId) {
        LambdaQueryWrapper<AgentKbRelation> queryWrapper= new LambdaQueryWrapper<>();
        queryWrapper.eq(AgentKbRelation::getAgentId, agentId)
                   .eq(AgentKbRelation::getIsDelete, 0)
                   .orderByAsc(AgentKbRelation::getSortOrder);
        
        return this.list(queryWrapper);
    }

    @Override
    public AgentKbRelation getByAgentIdAndKbId(Long agentId, Long kbId) {
        LambdaQueryWrapper<AgentKbRelation> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(AgentKbRelation::getAgentId, agentId)
                   .eq(AgentKbRelation::getKbId, kbId)
                   .eq(AgentKbRelation::getIsDelete, 0);
        
        return this.getOne(queryWrapper);
    }

    @Override
    public List<AgentKbRelation> getByUserId(Long userId) {
        LambdaQueryWrapper<AgentKbRelation> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.inSql(AgentKbRelation::getAgentId, 
            "SELECT id FROM agent WHERE user_id = " + userId + " AND is_delete= 0")
                   .eq(AgentKbRelation::getIsDelete, 0);
        
        return this.list(queryWrapper);
    }
}
