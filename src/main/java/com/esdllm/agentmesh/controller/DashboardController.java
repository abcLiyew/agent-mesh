package com.esdllm.agentmesh.controller;

import com.esdllm.agentmesh.common.BaseResponse;
import com.esdllm.agentmesh.common.ResultUtils;
import com.esdllm.agentmesh.model.dto.ActivityLog;
import com.esdllm.agentmesh.model.dto.DashboardStatistics;
import com.esdllm.agentmesh.model.dto.SystemStatus;
import com.esdllm.agentmesh.service.DashboardService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 系统管理后台 Dashboard 接口
 */
@RestController
@RequestMapping("/api/dashboard")
@Slf4j
public class DashboardController {

    @Resource
    private DashboardService dashboardService;

    /**
     * 获取系统统计概览
     * @return 统计概览数据
     */
    @GetMapping("/statistics")
    public BaseResponse<DashboardStatistics> getDashboardStatistics(HttpSession session) {
        DashboardStatistics statistics = dashboardService.getDashboardStatistics(session);
        return ResultUtils.success(statistics);
    }

    /**
     * 获取系统运行状态
     * @return 系统状态
     */
    @GetMapping("/status")
    public BaseResponse<SystemStatus> getSystemStatus(HttpSession session) {
        SystemStatus status = dashboardService.getSystemStatus(session);
        return ResultUtils.success(status);
    }

    /**
     * 获取最近活动日志
     * @param limit 返回数量，默认 20
     * @return 活动日志列表
     */
    @GetMapping("/activities")
    public BaseResponse<List<ActivityLog>> getRecentActivities(
            @RequestParam(value = "limit", defaultValue = "20") Integer limit,HttpSession session) {
        List<ActivityLog> activities = dashboardService.getRecentActivities(limit, session);
        return ResultUtils.success(activities);
    }
}
