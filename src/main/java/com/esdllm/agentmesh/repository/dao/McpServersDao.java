package com.esdllm.agentmesh.repository.dao;

import com.esdllm.agentmesh.model.domain.McpServers;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
* @author LiYehe
* @description 针对表【mcp_servers(MCP 服务器主表)】的数据库操作 Service
* @createDate 2026-03-10
*/
public interface McpServersDao extends IService<McpServers> {

    /**
     * 获取用户的 MCP 服务器列表（排除已删除）
     * @param userId 用户 ID
     * @return 服务器列表
     */
    List<McpServers> getMyMcpServers(Long userId);
}
