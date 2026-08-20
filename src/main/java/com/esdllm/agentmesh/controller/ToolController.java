package com.esdllm.agentmesh.controller;

import com.esdllm.agentmesh.common.BaseResponse;
import com.esdllm.agentmesh.common.ErrorCode;
import com.esdllm.agentmesh.common.ResultUtils;
import com.esdllm.agentmesh.exception.BusinessException;
import com.esdllm.agentmesh.model.domain.Tools;
import com.esdllm.agentmesh.model.domain.User;
import com.esdllm.agentmesh.model.dto.HealthCheckResult;
import com.esdllm.agentmesh.model.dto.HealthStatistics;
import com.esdllm.agentmesh.service.ToolService;
import com.esdllm.agentmesh.service.UserService;
import com.esdllm.agentmesh.service.agent.ToolHealthCheckService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/tools")
@Slf4j
public class ToolController {

    @Resource
    private ToolService toolService;

    @Resource
    private ToolHealthCheckService toolHealthCheckService;
    @Autowired
    private UserService userService;

    @PostMapping("/add")
    public BaseResponse<Long> addTool(@RequestBody Tools tool, HttpSession session) {
        User loginUser = getLoginUser(session);
        if (loginUser == null){
            return ResultUtils.error(ErrorCode.NOT_LOGIN);
        }
        Long toolId = toolService.createTool(tool, loginUser.getId());
        return ResultUtils.success(toolId);
    }

    @PutMapping("/update")
    public BaseResponse<Boolean> updateTool(@RequestBody Tools tool, HttpSession session) {
        User loginUser = getLoginUser(session);
        if (loginUser == null){
            return ResultUtils.error(ErrorCode.NOT_LOGIN);
        }
        Boolean result = toolService.updateTool(tool, loginUser.getId());
        return ResultUtils.success(result);
    }

    @DeleteMapping("/delete/{toolId}")
    public BaseResponse<Boolean> deleteTool(@PathVariable Long toolId, HttpSession session) {
        User loginUser = getLoginUser(session);
        if (loginUser == null){
            return ResultUtils.error(ErrorCode.NOT_LOGIN);
        }
        Boolean result = toolService.deleteTool(toolId, loginUser.getId());
        return ResultUtils.success(result);
    }

    @GetMapping("/my-list")
    public BaseResponse<List<Tools>> getMyTools(HttpSession session) {
        User loginUser= getLoginUser(session);
        if (loginUser == null){
            return ResultUtils.error(ErrorCode.NOT_LOGIN);
        }
        List<Tools> toolList = toolService.getMyTools(loginUser.getId());
        return ResultUtils.success(toolList);
    }

    @GetMapping("/{toolId}")
    public BaseResponse<Tools> getTool(@PathVariable Long toolId, HttpSession session) {
        User loginUser= getLoginUser(session);
        if (loginUser == null){
            return ResultUtils.error(ErrorCode.NOT_LOGIN);
        }
        Tools tool = toolService.getToolById(toolId, loginUser.getId());
        return ResultUtils.success(tool);
    }

    @GetMapping("/system-list")
    public BaseResponse<List<Tools>> getSystemTools() {
        List<Tools> toolList = toolService.getSystemTools();
        return ResultUtils.success(toolList);
    }

    /**
     * 手动触发工具健康检查
     * @param toolId 工具 ID
     * @return 检查结果
     */
    @PostMapping("/{toolId}/health-check")
    public BaseResponse<HealthCheckResult> manualHealthCheck(@PathVariable Long toolId, HttpSession session) {
        User loginUser = getLoginUser(session);
        if (loginUser == null){
            return ResultUtils.error(ErrorCode.NOT_LOGIN);
        }
        
        log.info("手动触发工具健康检查，toolId: {}, userId: {}", toolId, loginUser.getId());
        
        try {
            HealthCheckResult result = toolHealthCheckService.manualCheck(toolId);
            return ResultUtils.success(result);
        } catch (BusinessException e) {
            return ResultUtils.error(ErrorCode.SYSTEM_ERROR, e.getMessage());
        } catch (Exception e) {
            log.error("手动健康检查失败", e);
            return ResultUtils.error(ErrorCode.SYSTEM_ERROR, "健康检查失败：" + e.getMessage());
        }
    }

    /**
     * 获取工具健康状态
     * @param toolId 工具 ID
     * @return 健康状态
     */
    @GetMapping("/{toolId}/health-status")
    public BaseResponse<Map<String, Object>> getToolHealthStatus(@PathVariable Long toolId) {
        try {
            Integer status = toolHealthCheckService.getToolHealthStatus(toolId);
            
            Map<String, Object> result = new HashMap<>();
            result.put("toolId", toolId);
            result.put("healthStatus", status);
            result.put("statusText", getHealthStatusText(status));
            
            return ResultUtils.success(result);
        } catch (BusinessException e) {
            return ResultUtils.error(ErrorCode.SYSTEM_ERROR, e.getMessage());
        } catch (Exception e) {
            log.error("获取工具健康状态失败", e);
            return ResultUtils.error(ErrorCode.SYSTEM_ERROR, "获取健康状态失败：" + e.getMessage());
        }
    }

    /**
     * 获取所有工具的健康统计
     * @return 统计信息
     */
    @GetMapping("/health-statistics")
    public BaseResponse<HealthStatistics> getHealthStatistics() {
        try {
            HealthStatistics statistics = toolHealthCheckService.getHealthStatistics();
            return ResultUtils.success(statistics);
        } catch (Exception e) {
            log.error("获取健康统计失败", e);
            return ResultUtils.error(ErrorCode.SYSTEM_ERROR, "获取健康统计失败：" + e.getMessage());
        }
    }

    /**
     * 重置工具健康状态
     * @param toolId 工具 ID
     * @return 操作结果
     */
    @PostMapping("/{toolId}/health-reset")
    public BaseResponse<String> resetHealthStatus(@PathVariable Long toolId) {
        try {
            toolHealthCheckService.resetHealthStatus(toolId);
            return ResultUtils.success("健康状态已重置");
        } catch (BusinessException e) {
            return ResultUtils.error(ErrorCode.SYSTEM_ERROR, e.getMessage());
        } catch (Exception e) {
            log.error("重置健康状态失败", e);
            return ResultUtils.error(ErrorCode.SYSTEM_ERROR, "重置健康状态失败：" + e.getMessage());
        }
    }

    /**
     * 获取健康状态文本描述
     */
    private String getHealthStatusText(Integer status) {
        return switch (status) {
            case 0 -> "未知";
            case 1 -> "健康";
            case 2 -> "异常";
            case 3 -> "禁用";
            default -> "未知";
        };
    }

    /**
     * 获取当前登录用户
     */
    private User getLoginUser(HttpSession session) {
       return userService.getLoginUser( session);
    }
}
