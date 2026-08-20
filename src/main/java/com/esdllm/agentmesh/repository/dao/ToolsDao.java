package com.esdllm.agentmesh.repository.dao;

import com.esdllm.agentmesh.model.domain.Tools;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * @author LiYehe
 * @description 针对表【tools(统一工具表)】的数据库操作 Service
 * @createDate 2026-03-09 13:34:39
 */
public interface ToolsDao extends IService<Tools> {

    /**
     * 根据智能体 ID 查询工具
     * @param agentId 智能体 ID
     * @return 工具信息
     */
    Tools getByAgentId(Long agentId);

    /**
     * 获取系统工具列表
     * @return 系统工具列表
     */
    List<Tools> getSystemTools();

    /**
     * 获取用户的工具列表
     * @param userId 用户 ID
     * @return 用户工具列表
     */
    List<Tools> getUserTools(Long userId);

    /**
     * 获取产品相关的工具
     * @return 产品相关工具列表
     */
    List<Tools> getProductTools();

    List<Tools> getAgentTools(Long userId);
    
    /**
     * 获取用户可用的所有工具（包含系统工具和用户私有工具）
     * @param userId 用户 ID
     * @return 工具列表
     */
    List<Tools> getMyTools(Long userId);
}
