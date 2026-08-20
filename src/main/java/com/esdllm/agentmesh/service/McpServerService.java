package com.esdllm.agentmesh.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.esdllm.agentmesh.model.domain.McpServers;

import java.util.List;

/**
 * MCP 服务器服务接口
 */
public interface McpServerService {
    
    /**
     * 创建 MCP 服务器
     * @param mcpServer MCP 服务器信息
     * @param userId 用户 ID
     * @return 服务器 ID
     */
    Long createMcpServer(McpServers mcpServer, Long userId);
    
    /**
     * 更新 MCP 服务器
     * @param mcpServer MCP 服务器信息
     * @param userId 用户 ID
     * @return 是否成功
     */
    Boolean updateMcpServer(McpServers mcpServer, Long userId);
    
    /**
     * 删除 MCP 服务器（逻辑删除）
     * @param serverId 服务器 ID
     * @param userId 用户 ID
     * @return 是否成功
     */
    Boolean deleteMcpServer(Long serverId, Long userId);
    
    /**
     * 获取用户的 MCP 服务器列表
     * @param userId 用户 ID
     * @return 服务器列表
     */
    List<McpServers> getMyMcpServers(Long userId);
    
    /**
     * 根据 ID 获取 MCP 服务器
     * @param serverId 服务器 ID
     * @param userId 用户 ID
     * @return 服务器信息
     */
    McpServers getMcpServerById(Long serverId, Long userId);
    
    /**
     * 获取所有 MCP 服务器分页列表（管理员功能）
     * @param page 页码
     * @param pageSize 每页数量
     * @return 服务器分页数据
     */
    Page<McpServers> getMcpServersPage(int page, int pageSize);
    
    /**
     * 更新 MCP 服务器状态（管理员功能）
     * @param serverId 服务器 ID
     * @param status 状态（0=停止，1=运行中）
     * @param loginUser 登录用户
     * @return 是否成功
     */
    Boolean updateMcpServerStatus(Long serverId, Integer status, com.esdllm.agentmesh.model.domain.User loginUser);
}
