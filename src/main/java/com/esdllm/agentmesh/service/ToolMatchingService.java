package com.esdllm.agentmesh.service;

import com.esdllm.agentmesh.model.domain.Tools;

import java.util.List;

/**
 * 工具匹配服务
 */
public interface ToolMatchingService {
    
    /**
     * 根据意图匹配工具
     * @param intentType 意图类型
     * @param query 用户问题
     * @param userId 用户 ID
     * @return 匹配的工具列表
     */
    List<Tools> matchToolsByIntent(String intentType, String query, Long userId);
    
    /**
     * 根据关键词搜索工具
     * @param keyword 关键词
     * @param userId 用户 ID
     * @return 匹配的工具列表
     */
    List<Tools> searchTools(String keyword, Long userId);
    
    /**
     * 获取用户可用的所有工具
     * @param userId 用户 ID
     * @return 工具列表
     */
    List<Tools> getAvailableTools(Long userId);
}
