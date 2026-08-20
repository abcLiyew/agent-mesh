package com.esdllm.agentmesh.repository.dao.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.esdllm.agentmesh.model.domain.McpServers;
import com.esdllm.agentmesh.repository.dao.McpServersDao;
import com.esdllm.agentmesh.repository.mapper.McpServersMapper;
import org.springframework.stereotype.Service;

import java.util.List;

/**
* @author LiYehe
* @description 针对表【mcp_servers(MCP 服务器配置表：存储用户配置的 Model Context Protocol 服务连接信息)】的数据库操作 Service 实现
* @createDate 2026-03-09 13:34:39
*/
@Service
public class McpServersDaoImpl extends ServiceImpl<McpServersMapper, McpServers>
    implements McpServersDao {

    @Override
    public List<McpServers> getMyMcpServers(Long userId) {
        return this.lambdaQuery()
            .eq(McpServers::getOwnerId, userId)
            .list();
    }
}




