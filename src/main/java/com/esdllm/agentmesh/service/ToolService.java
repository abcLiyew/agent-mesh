package com.esdllm.agentmesh.service;

import com.esdllm.agentmesh.model.domain.Tools;

import java.util.List;

/**
 * 工具服务接口
 */
public interface ToolService {
    
    /**
     * 创建工具
     * @param tool 工具信息
     * @param userId 用户 ID
     * @return 工具 ID
     */
    Long createTool(Tools tool, Long userId);
    
    /**
     * 更新工具
     * @param tool 工具信息
     * @param userId 用户 ID
     * @return 是否成功
     */
    Boolean updateTool(Tools tool, Long userId);
    
    /**
     * 删除工具（逻辑删除）
     * @param toolId 工具 ID
     * @param userId 用户 ID
     * @return 是否成功
     */
    Boolean deleteTool(Long toolId, Long userId);
    
    /**
     * 获取用户的工具列表（包括系统内置工具）
     * @param userId 用户 ID
     * @return 工具列表
     */
    List<Tools> getMyTools(Long userId);
    
    /**
     * 根据 ID 获取工具
     * @param toolId 工具 ID
     * @param userId 用户 ID
     * @return 工具信息
     */
    Tools getToolById(Long toolId, Long userId);
    
    /**
     * 获取所有系统内置工具
     * @return 工具列表
     */
    List<Tools> getSystemTools();
    
    /**
     * 获取所有工具分页列表（管理员功能）
     * @param page 页码
     * @param pageSize 每页数量
     * @return 工具列表
     */
    List<Tools> getAllTools(int page, int pageSize);
    
    /**
     * 获取工具总数
     * @return 工具总数
     */
    Long getToolsCount();
}
