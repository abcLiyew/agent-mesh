package com.esdllm.agentmesh.controller;


import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.esdllm.agentmesh.common.BaseResponse;
import com.esdllm.agentmesh.common.ErrorCode;
import com.esdllm.agentmesh.common.ResultUtils;
import com.esdllm.agentmesh.exception.BusinessException;
import com.esdllm.agentmesh.model.domain.ConversationLog;
import com.esdllm.agentmesh.model.domain.User;
import com.esdllm.agentmesh.model.dto.ConversationSessionGroup;
import com.esdllm.agentmesh.model.dto.ConversationStatistics;
import com.esdllm.agentmesh.model.dto.IntentStatistics;
import com.esdllm.agentmesh.model.dto.ToolUsageStatistics;
import com.esdllm.agentmesh.service.ConversationLogService;
import com.esdllm.agentmesh.service.UserService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.List;

/**
 * 对话日志 Controller
 */
@RestController
@RequestMapping("/api/conversation")
@Slf4j
public class ConversationLogController {

    @Resource
    private ConversationLogService conversationLogService;

    @Resource
    private UserService userService;

    /**
     * 分页查询用户的对话日志（按会话分组）
     */
    @GetMapping("/sessions")
    public BaseResponse<Page<ConversationSessionGroup>> getUserConversationSessionGroups(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize,
            HttpServletRequest request
    ) {
        User loginUser = userService.getLoginUser(request.getSession());
        if (loginUser == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }

        Page<ConversationSessionGroup> sessionGroups = conversationLogService.getUserConversationSessionGroups(
                loginUser.getId(), page, pageSize
        );

        return ResultUtils.success(sessionGroups);
    }

    /**
     * 分页查询用户的对话日志（传统模式，每条记录一行）
     */
    @GetMapping("/list")
    public BaseResponse<Page<ConversationLog>> getUserConversationLogs(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            HttpServletRequest request
    ) {
        User loginUser = userService.getLoginUser(request.getSession());
        if (loginUser == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }

        Page<ConversationLog> logs = conversationLogService.getUserConversationLogs(
                loginUser.getId(), page, pageSize
        );

        return ResultUtils.success(logs);
    }

    /**
     * 分页查询指定智能体的对话日志
     */
    @GetMapping("/agent/{agentId}")
    public BaseResponse<Page<ConversationLog>> getAgentConversationLogs(
            @PathVariable Long agentId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            HttpServletRequest request
    ) {
        User loginUser = userService.getLoginUser(request.getSession());
        if (loginUser == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }

        Page<ConversationLog> logs = conversationLogService.getAgentConversationLogs(
                agentId, page, pageSize
        );

        return ResultUtils.success(logs);
    }

    /**
     * 获取会话详情（多轮对话）
     */
    @GetMapping("/session/{sessionId}")
    public BaseResponse<List<ConversationLog>> getSessionDetail(
            @PathVariable String sessionId,
            HttpServletRequest request
    ) {
        User loginUser = userService.getLoginUser(request.getSession());
        if (loginUser == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }

        List<ConversationLog> logs = conversationLogService.getSessionDetail(sessionId);

        return ResultUtils.success(logs);
    }

    /**
     * 获取用户对话统计
     */
    @GetMapping("/stats")
    public BaseResponse<ConversationStatistics> getUserStats(
            @RequestParam(defaultValue = "7") int days,
            HttpServletRequest request
    ) {
        User loginUser = userService.getLoginUser(request.getSession());
        if (loginUser == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }

        Date endDate = new Date();
        Date startDate = new Date(System.currentTimeMillis() - days * 24L * 60 * 60 * 1000);

        ConversationStatistics stats = conversationLogService.getStatistics(
                loginUser.getId(), startDate, endDate
        );

        return ResultUtils.success(stats);
    }

    /**
     * 获取智能体意图统计
     */
    @GetMapping("/agent/{agentId}/intent-stats")
    public BaseResponse<List<IntentStatistics>> getAgentIntentStats(
            @PathVariable Long agentId,
            @RequestParam(defaultValue = "10") int limit
    ) {
        List<IntentStatistics> stats = conversationLogService.getAgentIntentStats(agentId, limit);
        return ResultUtils.success(stats);
    }

    /**
     * 获取智能体工具使用统计
     */
    @GetMapping("/agent/{agentId}/tool-stats")
    public BaseResponse<List<ToolUsageStatistics>> getAgentToolStats(
            @PathVariable Long agentId,
            @RequestParam(defaultValue = "10") int limit
    ) {
        List<ToolUsageStatistics> stats = conversationLogService.getAgentToolStats(agentId, limit);
        return ResultUtils.success(stats);
    }

    /**
     * 更新对话反馈
     */
    @PutMapping("/feedback/{logId}")
    public BaseResponse<Boolean> updateFeedback(
            @PathVariable Long logId,
            @RequestParam Integer rating,
            @RequestParam(required = false) String feedback,
            HttpServletRequest request
    ) {
        User loginUser = userService.getLoginUser(request.getSession());
        if (loginUser == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }

        if (rating < 1 || rating > 5) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "评分必须在 1-5 之间");
        }

        conversationLogService.updateFeedback(logId, rating, feedback);

        return ResultUtils.success(true);
    }

    /**
     * 获取对话反馈详情
     */
    @GetMapping("/feedback/{logId}")
    public BaseResponse<ConversationLog> getFeedbackDetail(
            @PathVariable Long logId,
            HttpServletRequest request
    ) {
        User loginUser = userService.getLoginUser(request.getSession());
        if (loginUser == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }

        ConversationLog log = conversationLogService.getFeedbackDetail(logId);
        return ResultUtils.success(log);
    }
}
