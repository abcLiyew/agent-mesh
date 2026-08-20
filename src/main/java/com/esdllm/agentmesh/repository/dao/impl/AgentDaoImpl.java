package com.esdllm.agentmesh.repository.dao.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.esdllm.agentmesh.model.domain.Agent;
import com.esdllm.agentmesh.repository.dao.AgentDao;
import com.esdllm.agentmesh.repository.mapper.AgentMapper;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @author LiYehe
 * @description 针对表【agent(智能体主表：存储用户创建的 AI 智能体配置)】的数据库操作 Service 实现
 * @createDate 2026-03-09 13:26:58
 */
@Service
public class AgentDaoImpl extends ServiceImpl<AgentMapper, Agent>
        implements AgentDao {

    @Override
    public List<Agent> getAgentListBypage(int page, int pageSize) {
        Page<Agent> agentPage = new Page<>(page, pageSize);
        LambdaQueryWrapper<Agent> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.orderByDesc(Agent::getCreatedAt);

        return this.page(agentPage, queryWrapper).getRecords();
    }

    @Override
    public Long getAgentNum() {
        LambdaQueryWrapper<Agent> queryWrapper= new LambdaQueryWrapper<>();
        queryWrapper.eq(Agent::getIsDelete, 0);
        return this.count(queryWrapper);
    }

    @Override
    public Page<Agent> getMyAgentsPage(Long userId, int page, int pageSize) {
        Page<Agent> agentPage = new Page<>(page, pageSize);
        LambdaQueryWrapper<Agent> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Agent::getUserId, userId)
                .orderByDesc(Agent::getCreatedAt);

        return this.page(agentPage, queryWrapper);
    }

    @Override
    public Page<Agent> searchAgentsPage(String keyword, int page, int pageSize) {
        Page<Agent> agentPage = new Page<>(page, pageSize);
        LambdaQueryWrapper<Agent> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Agent::getIsDelete, 0)
                .eq(Agent::getStatus, 1)
                .and(wrapper -> wrapper
                        .like(Agent::getName, keyword)
                        .or()
                        .like(Agent::getDescription, keyword)
                )
                .orderByDesc(Agent::getCreatedAt);

        return this.page(agentPage, queryWrapper);
    }

    @Override
    public List<Agent> getUserAgents(Long userId) {
        LambdaQueryWrapper<Agent> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Agent::getUserId, userId);

        return this.list(queryWrapper);

    }

    @Override
    public Page<Agent> getPublicAgentsPage(int page, int pageSize) {
        Page<Agent> agentPage = new Page<>(page, pageSize);
        LambdaQueryWrapper<Agent> queryWrapper = new LambdaQueryWrapper<>();
        // 查询已发布的智能体(status=1)
        queryWrapper.eq(Agent::getStatus, 1)
                .eq(Agent::getIsDelete, 0)
                .orderByDesc(Agent::getCreatedAt);

        return this.page(agentPage, queryWrapper);
    }
}
