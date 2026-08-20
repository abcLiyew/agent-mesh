package com.esdllm.agentmesh.service;

import java.util.List;

/**
 * 智能体网络服务接口
 */
public interface AgentNetworkService {
    
    /**
     * 添加智能体依赖关系
     * @param agentId 智能体 ID
     * @param dependsOnAgentId 被依赖的智能体 ID
     * @param dependencyType 依赖类型
     * @param priority 优先级
     * @param userId 用户 ID
     */
    void addAgentDependency(Long agentId, Long dependsOnAgentId, 
                          String dependencyType, Integer priority, Long userId);

    void addAgentDependency(AgentDependency dependency, Long userId);

    /**
     * 移除智能体依赖关系
     * @param agentId 智能体 ID
     * @param dependsOnAgentId 被依赖的智能体 ID
     * @param userId 用户 ID
     */
    void removeAgentDependency(Long agentId, Long dependsOnAgentId, Long userId);
    
    /**
     * 获取智能体的依赖列表
     * @param agentId 智能体 ID
     * @return 依赖列表
     */
    List<AgentDependency> getAgentDependencies(Long agentId);
    
    /**
     * 检测循环依赖
     * @param agentId 智能体 ID
     * @return 是否存在循环依赖
     */
    boolean hasCircularDependency(Long agentId);
    
    /**
     * 获取智能体协作拓扑图数据
     * @param userId 用户 ID
     * @return 拓扑图数据
     */
    Object getNetworkTopology(Long userId);
}
