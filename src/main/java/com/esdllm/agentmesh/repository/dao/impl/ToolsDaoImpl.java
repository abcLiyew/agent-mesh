package com.esdllm.agentmesh.repository.dao.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.esdllm.agentmesh.model.domain.Tools;
import com.esdllm.agentmesh.repository.dao.ToolsDao;
import com.esdllm.agentmesh.repository.mapper.ToolsMapper;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
* @author LiYehe
* @description 针对表【tools(统一工具表：存储所有可用工具定义 (系统内置 + 用户自定义 HTTP + 用户 MCP 暴露的工具))】的数据库操作 Service 实现
* @createDate 2026-03-09 13:34:39
*/
@Service
public class ToolsDaoImpl extends ServiceImpl<ToolsMapper, Tools>
    implements ToolsDao {

    @Override
    public List<Tools> getSystemTools() {
        return this.lambdaQuery()
            .eq(Tools::getSourceType, "SYSTEM")
            .eq(Tools::getIsEnabled, true)
            .list();
    }

    @Override
    public List<Tools> getUserTools(Long userId) {
        if (userId == null) {
            return new ArrayList<>();
        }

        return this.lambdaQuery()
            .eq(Tools::getOwnerId, userId)
            .eq(Tools::getIsEnabled, true)
            .list();
    }

    @Override
    public List<Tools> getProductTools() {
        return this.lambdaQuery()
            .eq(Tools::getSourceType, "SYSTEM")
            .eq(Tools::getIsEnabled, true)
            .and(wrapper -> wrapper
                    .like(Tools::getDescription, "产品")
                    .or()
                    .like(Tools::getDescription, "商品")
                    .or()
                    .like(Tools::getToolCodeName, "product")
            )
            .list();
    }

    @Override
    public Tools getByAgentId(Long agentId) {
        if (agentId == null) {
            return null;
        }

        // 查询描述中包含智能体 ID 的工具（简化实现，实际应用关联表）
        return this.lambdaQuery()
            .eq(Tools::getSourceType, "AGENT_TOOL")
            .and(wrapper -> wrapper.like(Tools::getDescription, "agent:" + agentId))
            .one();
    }

    @Override
    public List<Tools> getAgentTools(Long userId) {
        // 获取类型为 AGENT_TOOL 的工具
        return this.lambdaQuery()
            .eq(Tools::getOwnerId, userId)
            .eq(Tools::getSourceType, "AGENT_TOOL")
            .eq(Tools::getIsEnabled, true)
            .list();
    }

    @Override
    public List<Tools> getMyTools(Long userId) {
        // 查询用户私有工具和系统内置工具（自动过滤已删除）
        return this.list().stream()
            .filter(tool -> tool.getIsDelete() == 0)
            .filter(tool -> tool.getOwnerId() == null || tool.getOwnerId().equals(userId))
            .toList();
    }
}




