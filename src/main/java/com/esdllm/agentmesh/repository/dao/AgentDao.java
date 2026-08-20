package com.esdllm.agentmesh.repository.dao;

import com.esdllm.agentmesh.model.domain.Agent;
import com.baomidou.mybatisplus.extension.service.IService;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import java.util.List;

/**
 * @author LiYehe
 * @description 针对表【agent(智能体主表：存储用户创建的 AI 智能体配置)】的数据库操作 Service
 * @createDate 2026-03-09 13:26:58
 */
public interface AgentDao extends IService<Agent> {

    /**
     * 分页获取智能体列表
     * @param page 页码
     * @param pageSize 每页数量
     * @return 智能体列表
     */
    List<Agent> getAgentListBypage(int page, int pageSize);

    /**
     * 获取智能体总数
     * @return 智能体总数
     */
    Long getAgentNum();

    /**
     * 分页获取用户的智能体列表
     * @param userId 用户 ID
     * @param page 页码
     * @param pageSize 每页数量
     * @return 智能体列表
     */
    Page<Agent> getMyAgentsPage(Long userId, int page, int pageSize);

    /**
     * 分页搜索智能体
     * @param keyword 关键词
     * @param page 页码
     * @param pageSize 每页数量
     * @return 智能体列表
     */
    Page<Agent> searchAgentsPage(String keyword, int page, int pageSize);

    List<Agent> getUserAgents(Long userId);

    /**
     * 获取已发布已公开的智能体列表（分页）
     * @param page 页码
     * @param pageSize 每页数量
     * @return 智能体列表
     */
    Page<Agent> getPublicAgentsPage(int page, int pageSize);
}
