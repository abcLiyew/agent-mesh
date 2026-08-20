package com.esdllm.agentmesh.service;

import com.esdllm.agentmesh.model.domain.User;
import com.esdllm.agentmesh.model.dto.request.AgentAddRequest;
import com.esdllm.agentmesh.model.dto.response.AgentResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public interface AgentService {
    @Transactional(rollbackFor = Exception.class)
    Long addAgent(AgentAddRequest request, User loginUser);

    List<AgentResponse> getAgentListByPage(int page, int pageSize);

    AgentResponse getAgentById(Long id);

    Long getAgentNum();

    /**
     * 更新智能体
     * @param id 智能体 ID
     * @param request 更新请求
     * @param loginUser 登录用户
     * @return 是否成功
     */
    Boolean updateAgent(Long id, AgentAddRequest request, User loginUser);

    /**
     * 删除智能体（逻辑删除）
     * @param id 智能体 ID
     * @param loginUser 登录用户
     * @return 是否成功
     */
    Boolean deleteAgent(Long id, User loginUser);

    /**
     * 获取用户的智能体列表（分页）
     * @param userId 用户 ID
     * @param page 页码
     * @param pageSize 每页数量
     * @return 智能体列表
     */
    List<AgentResponse> getMyAgents(Long userId, int page, int pageSize);

    /**
     * 更新智能体状态
     * @param id 智能体 ID
     * @param status 状态（0=草稿，1=发布）
     * @param loginUser 登录用户
     * @return 是否成功
     */
    Boolean updateAgentStatus(Long id, Integer status, User loginUser);

    /**
     * 搜索智能体
     * @param keyword 关键词
     * @param page 页码
     * @param pageSize 每页数量
     * @return 智能体列表
     */
    List<AgentResponse> searchAgents(String keyword, int page, int pageSize);

    /**
     * 获取已发布已公开的智能体列表（分页）
     * @param page 页码
     * @param pageSize 每页数量
     * @return 智能体列表
     */
    List<AgentResponse> getPublicAgents(int page, int pageSize);
}
