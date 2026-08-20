package com.esdllm.agentmesh.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.esdllm.agentmesh.common.ErrorCode;
import com.esdllm.agentmesh.exception.BusinessException;
import com.esdllm.agentmesh.model.domain.McpServers;
import com.esdllm.agentmesh.repository.dao.McpServersDao;
import com.esdllm.agentmesh.service.McpServerService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;

/**
 * MCP 服务器服务实现类
 */
@Service
@Slf4j
public class McpServerServiceImpl implements McpServerService {

    @Resource
    private McpServersDao mcpServersDao;

    @Transactional(rollbackFor = Exception.class)
    @Override
    public Long createMcpServer(McpServers mcpServer, Long userId) {
        // 1. 基础参数校验
        validateBasicParams(mcpServer);

        // 2. 设置归属用户（MyBatis-Plus 会自动填充 is_delete=0）
        mcpServer.setOwnerId(userId);

        // 3. 保存到数据库
        boolean saved = mcpServersDao.save(mcpServer);
        if (!saved) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "创建 MCP 服务器失败");
        }

       log.info("创建 MCP 服务器成功，serverId: {}, userId: {}", mcpServer.getId(), userId);
        return mcpServer.getId();
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public Boolean updateMcpServer(McpServers mcpServer, Long userId) {
        // 1. 基础参数校验
        validateBasicParams(mcpServer);

        // 2. 查询服务器是否存在且属于当前用户
        McpServers existingServer = mcpServersDao.getById(mcpServer.getId());
        if (existingServer == null) {
            throw new BusinessException(ErrorCode.MCP_SERVER_NOT_FOUND, "MCP 服务器不存在");
        }

        if (!existingServer.getOwnerId().equals(userId)) {
            throw new BusinessException(ErrorCode.NO_AUTH, "无权限修改该服务器");
        }

        // 3. 更新服务器信息（MyBatis-Plus 会自动填充 updated_at）
        mcpServer.setOwnerId(userId);
        
        boolean updated = mcpServersDao.updateById(mcpServer);
        if (!updated) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "更新 MCP 服务器失败");
        }

       log.info("更新 MCP 服务器成功，serverId: {}, userId: {}", mcpServer.getId(), userId);
        return true;
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public Boolean deleteMcpServer(Long serverId, Long userId) {
        // 1. 查询服务器是否存在
        McpServers existingServer = mcpServersDao.getById(serverId);
        if (existingServer == null) {
            throw new BusinessException(ErrorCode.MCP_SERVER_NOT_FOUND, "MCP 服务器不存在");
        }

        // 2. 验证权限
        if (!existingServer.getOwnerId().equals(userId)) {
            throw new BusinessException(ErrorCode.NO_AUTH, "无权限删除该服务器");
        }

        // 3. 使用 MyBatis-Plus 的逻辑删除（自动设置 is_delete=1）
        boolean deleted = mcpServersDao.removeById(serverId);
        if (!deleted) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "删除 MCP 服务器失败");
        }

       log.info("删除 MCP 服务器成功，serverId: {}, userId: {}", serverId, userId);
        return true;
    }

    @Override
    public List<McpServers> getMyMcpServers(Long userId) {
        // 使用 DAO 层的查询方法，自动过滤已删除数据
        return mcpServersDao.getMyMcpServers(userId);
    }

    @Override
    public McpServers getMcpServerById(Long serverId, Long userId) {
        McpServers server = mcpServersDao.getById(serverId);
        if (server == null) {
            throw new BusinessException(ErrorCode.MCP_SERVER_NOT_FOUND, "MCP 服务器不存在");
        }

        if (!server.getOwnerId().equals(userId)) {
            throw new BusinessException(ErrorCode.NO_AUTH, "无权限查看该服务器");
        }

        return server;
    }

    @Override
    public Page<McpServers> getMcpServersPage(int page, int pageSize) {
        // 查询所有 MCP 服务器（排除已删除的）
        return mcpServersDao.lambdaQuery()
                .eq(McpServers::getIsDelete, 0)
                .orderByDesc(McpServers::getCreatedAt)
                .page(new Page<>(page, pageSize));
    }

    @Override
    public Boolean updateMcpServerStatus(Long serverId, Integer status, com.esdllm.agentmesh.model.domain.User loginUser) {
        if (serverId == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "服务器 ID 不能为空");
        }

        if (status == null || (status != 0 && status != 1)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "状态值必须为 0 或 1");
        }

        // 查询服务器是否存在
        McpServers existingServer = mcpServersDao.getById(serverId);
        if (existingServer == null) {
            throw new BusinessException(ErrorCode.MCP_SERVER_NOT_FOUND, "MCP 服务器不存在");
        }

        // 更新服务器状态
        existingServer.setStatus(status);
        existingServer.setUpdatedAt(new java.util.Date());
        
        boolean updated = mcpServersDao.updateById(existingServer);
        if (!updated) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "更新服务器状态失败");
        }

        log.info("更新 MCP 服务器状态成功，serverId: {}, status: {}, userId: {}", serverId, status, loginUser.getId());
        return true;
    }

    /**
     * 验证基础参数
     */
    private void validateBasicParams(McpServers mcpServer) {
        // 服务名称不能为空
        if (StrUtil.isBlank(mcpServer.getServerName())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "服务名称不能为空");
        }

        // 服务名称长度限制
        if (mcpServer.getServerName().length() < 2 || mcpServer.getServerName().length() > 100) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "服务名称长度应在 2-100 个字符之间");
        }

        // 传输协议不能为空
        if (StrUtil.isBlank(mcpServer.getTransportType())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "传输协议不能为空");
        }

        // 验证传输协议枚举值
        List<String> validTypes = Arrays.asList("SSE", "STDIO", "STREAMABLE_HTTP");
        if (!validTypes.contains(mcpServer.getTransportType().toUpperCase())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, 
                "无效的传输协议：" + mcpServer.getTransportType() + "，有效值为：SSE, STDIO, STREAMABLE_HTTP");
        }

        // 根据传输类型验证 URL 或命令参数
        String transportType = mcpServer.getTransportType().toUpperCase();
        if ("SSE".equals(transportType) || "STREAMABLE_HTTP".equals(transportType)) {
            if (StrUtil.isBlank(mcpServer.getEndpointUrl())) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, 
                    transportType + " 模式下接入 URL 不能为空");
            }
        } else if ("STDIO".equals(transportType)) {
            if (mcpServer.getCommandArgs() == null) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, 
                    "STDIO 模式下启动命令参数不能为空");
            }
        }
    }
}
