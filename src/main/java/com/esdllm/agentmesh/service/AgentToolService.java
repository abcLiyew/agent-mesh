package com.esdllm.agentmesh.service;

import com.esdllm.agentmesh.model.domain.Tools;
import com.esdllm.agentmesh.model.dto.response.AgentToolResponse;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 智能体工具管理服务接口（增加缓存支持）
 */
public interface AgentToolService {
    /**
     * 获取用户的智能体工具列表（带缓存）
     * @param userId 用户 ID
     * @return 工具列表
     */
    @Cacheable(value = "toolConfig", key = "'user_tools:' + #userId")
    List<Tools> getAgentToolsWithCache(Long userId);
    
    /**
     * 获取智能体工具配置（带缓存）
     * @param agentId 智能体 ID
     * @return 工具配置
     */
    @Cacheable(value = "toolConfig", key = "'agent_tools:' + #agentId")
    Object getAgentToolConfigWithCache(Long agentId);

    /**
     * 注册智能体为工具
     * @param agentId 智能体 ID
     * @param toolCodeName 工具代码名称
     * @param displayName 工具显示名称
     * @param description 工具描述
     * @param userId 用户 ID
     * @return 工具 ID
     */
    @Transactional(rollbackFor = Exception.class)
    Long registerAgentAsTool(Long agentId, String toolCodeName,
                             String displayName, String description, Long userId);

    /**
     * 调用智能体工具
     * @param agentId 智能体 ID
     * @param query 查询参数
     * @param parameters 参数
     * @param userId 用户 ID
     * @return 响应结果
     */
    AgentToolResponse invokeAgentTool(Long agentId, String query,
                                      Object parameters, Long userId);

    /**
     * 获取用户的智能体工具列表
     * @param userId 用户 ID
     * @return 智能体工具列表
     */
    List<Tools> getAgentTools(Long userId);

    /**
     * 更新智能体工具状态
     * @param agentId 智能体 ID
     * @param isEnabled 是否启用
     * @param userId 用户 ID
     */
    @Transactional(rollbackFor = Exception.class)
    void updateAgentToolStatus(Long agentId, Boolean isEnabled, Long userId);
}
