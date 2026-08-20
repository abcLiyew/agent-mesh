package com.esdllm.agentmesh.service;

import com.esdllm.agentmesh.model.dto.ActivityLog;
import com.esdllm.agentmesh.model.dto.DashboardStatistics;
import com.esdllm.agentmesh.model.dto.SystemStatus;
import jakarta.servlet.http.HttpSession;

import java.util.List;

/**
 * 系统管理后台服务接口
 */
public interface DashboardService {
    
    /**
     * 获取系统统计概览
     * @return 统计概览数据
     */
    DashboardStatistics getDashboardStatistics(HttpSession session);
    
    /**
     * 获取系统运行状态
     * @return 系统状态
     */
    SystemStatus getSystemStatus(HttpSession session);
    
    /**
     * 获取最近活动日志
     * @param limit 返回数量
     * @return 活动日志列表
     */
    List<ActivityLog> getRecentActivities(int limit, HttpSession session);
}
