package com.esdllm.agentmesh.controller;

import com.esdllm.agentmesh.common.BaseResponse;
import com.esdllm.agentmesh.common.ErrorCode;
import com.esdllm.agentmesh.common.ResultUtils;
import com.esdllm.agentmesh.exception.BusinessException;
import com.esdllm.agentmesh.model.domain.ConversationLog;
import com.esdllm.agentmesh.model.domain.User;
import com.esdllm.agentmesh.model.dto.FeedbackStatistics;
import com.esdllm.agentmesh.model.dto.FeedbackTrend;
import com.esdllm.agentmesh.model.dto.request.FeedbackRequest;
import com.esdllm.agentmesh.service.ConversationLogService;
import com.esdllm.agentmesh.service.UserService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 用户反馈控制器
 */
@RestController
@RequestMapping("/api/feedback")
@Slf4j
public class FeedbackController {

    @Resource
    private ConversationLogService conversationLogService;
    @Resource
    private UserService userService;
    @Resource
    private com.esdllm.agentmesh.service.FeedbackAnalysisService feedbackAnalysisService;

    /**
     * 提交用户反馈
     */
    @PostMapping("/submit")
    public BaseResponse<Boolean> submitFeedback(
            @RequestBody @Validated FeedbackRequest request,
            HttpSession session) {
        
        User loginUser = getLoginUser(session);
        log.info("收到用户反馈，userId: {}, logId: {}, rating: {}", 
                loginUser.getId(), request.getLogId(), request.getRating());
        
        try {
            // 验证反馈所属用户
            ConversationLog conversationLog = conversationLogService.getFeedbackDetail(request.getLogId());
            if (!conversationLog.getUserId().equals(loginUser.getId())) {
                throw new BusinessException(ErrorCode.FORBIDDEN_ERROR, "只能对自己的对话进行反馈");
            }
            
            conversationLogService.updateFeedback(
                request.getLogId(), 
                request.getRating(), 
                request.getFeedback()
            );
            
            return ResultUtils.success(true);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("提交反馈失败", e);
            return ResultUtils.error(ErrorCode.SYSTEM_ERROR, "提交反馈失败：" + e.getMessage());
        }
    }

    /**
     * 获取用户的反馈统计
     */
    @GetMapping("/my-statistics")
    public BaseResponse<FeedbackStatistics> getMyFeedbackStatistics(HttpSession session) {
        User loginUser = getLoginUser(session);
        
        try {
            FeedbackStatistics statistics = conversationLogService.getUserFeedbackStats(loginUser.getId());
            return ResultUtils.success(statistics);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("获取用户反馈统计失败", e);
            return ResultUtils.error(ErrorCode.SYSTEM_ERROR, "获取反馈统计失败：" + e.getMessage());
        }
    }

    /**
     * 获取智能体的反馈统计
     */
    @GetMapping("/agent/{agentId}/statistics")
    public BaseResponse<FeedbackStatistics> getAgentFeedbackStatistics(
            @PathVariable Long agentId,
            HttpSession session) {
        
        User loginUser = getLoginUser(session);
        log.info("获取智能体反馈统计，userId: {}, agentId: {}", loginUser.getId(), agentId);
        
        try {
            FeedbackStatistics statistics = conversationLogService.getAgentFeedbackStats(agentId);
            return ResultUtils.success(statistics);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("获取智能体反馈统计失败", e);
            return ResultUtils.error(ErrorCode.SYSTEM_ERROR, "获取反馈统计失败：" + e.getMessage());
        }
    }

    /**
     * 获取反馈趋势（最近 7 天）
     */
    @GetMapping("/my-trend")
    public BaseResponse<List<FeedbackTrend>> getMyFeedbackTrend(
            HttpSession session,
            @RequestParam(defaultValue = "7") int days) {
        
        User loginUser = getLoginUser(session);
        
        try {
            List<FeedbackTrend> trendList = conversationLogService.getFeedbackTrend(loginUser.getId(), days);
            return ResultUtils.success(trendList);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("获取反馈趋势失败", e);
            return ResultUtils.error(ErrorCode.SYSTEM_ERROR, "获取反馈趋势失败：" + e.getMessage());
        }
    }

    /**
     * 获取低分反馈列表（用于问题分析）
     */
    @GetMapping("/agent/{agentId}/low-ratings")
    public BaseResponse<List<FeedbackStatistics.LowRatingFeedback>> getAgentLowRatingFeedbacks(
            @PathVariable Long agentId,
            @RequestParam(defaultValue = "20") int limit,
            HttpSession session) {
        
        User loginUser = getLoginUser(session);
        log.info("获取智能体低分反馈，userId: {}, agentId: {}, limit: {}", loginUser.getId(), agentId, limit);
        
        try {
            List<FeedbackStatistics.LowRatingFeedback> feedbackList = 
                conversationLogService.getLowRatingFeedbacks(agentId, limit);
            return ResultUtils.success(feedbackList);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("获取低分反馈失败", e);
            return ResultUtils.error(ErrorCode.SYSTEM_ERROR, "获取低分反馈失败：" + e.getMessage());
        }
    }

    /**
     * 获取单个反馈详情
     */
    @GetMapping("/{logId}")
    public BaseResponse<Map<String, Object>> getFeedbackDetail(
            @PathVariable Long logId,
            HttpSession session) {
        
        User loginUser = getLoginUser(session);
        
        try {
            ConversationLog conversationLog = conversationLogService.getFeedbackDetail(logId);
            
            // 权限检查：只能查看自己的反馈
            if (!conversationLog.getUserId().equals(loginUser.getId())) {
                throw new BusinessException(ErrorCode.FORBIDDEN_ERROR, "无权查看该反馈");
            }
            
            Map<String, Object> result = new HashMap<>();
            result.put("logId", conversationLog.getId());
            result.put("userId", conversationLog.getUserId());
            result.put("agentId", conversationLog.getAgentId());
            result.put("userQuery", conversationLog.getUserQuery());
            result.put("finalResponse", conversationLog.getFinalResponse());
            result.put("rating", conversationLog.getUserRating());
            result.put("feedback", conversationLog.getUserFeedback());
            result.put("intentType", conversationLog.getIntentType());
            result.put("status", conversationLog.getStatus());
            result.put("errorMessage", conversationLog.getErrorMessage());
            result.put("createdAt", conversationLog.getCreatedAt());
            
            return ResultUtils.success(result);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("获取反馈详情失败", e);
            return ResultUtils.error(ErrorCode.SYSTEM_ERROR, "获取反馈详情失败：" + e.getMessage());
        }
    }

    /**
     * 获取当前登录用户
     */
    private User getLoginUser(HttpSession session) {
        User userObj = userService.getLoginUser(session);
        if (userObj == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }
        return userObj;
    }
    
    // ========== 反馈分析功能 ==========
    
    /**
     * 获取智能体反馈分析报告
     */
    @GetMapping("/agent/{agentId}/analysis/report")
    public BaseResponse<Map<String, Object>> getAgentFeedbackReport(
            @PathVariable Long agentId,
            HttpSession session) {
        
        User loginUser = getLoginUser(session);
        log.info("获取智能体反馈分析报告，userId: {}, agentId: {}", loginUser.getId(), agentId);
        
        var report = feedbackAnalysisService.analyzeAgentFeedback(agentId);
        
        Map<String, Object> result = new HashMap<>();
        result.put("averageRating", report.averageRating());
        result.put("totalFeedbacks", report.totalFeedbacks());
        result.put("mainIssue", report.mainIssue());
        result.put("problemCategories", report.problemCategories());
        
        List<Map<String, Object>> suggestionsList = report.suggestions().stream()
            .map(suggestion -> {
                Map<String, Object> suggestionMap = new HashMap<>();
                suggestionMap.put("category", suggestion.category());
                suggestionMap.put("suggestion", suggestion.suggestion());
                suggestionMap.put("reason", suggestion.reason());
                suggestionMap.put("priority", suggestion.priority());
                return suggestionMap;
            })
            .toList();
        
        result.put("suggestions", suggestionsList);
        
        return ResultUtils.success(result);
    }

    /**
     * 获取反馈关键词分析
     */
    @GetMapping("/agent/{agentId}/analysis/keywords")
    public BaseResponse<Map<String, Integer>> getAgentFeedbackKeywords(
            @PathVariable Long agentId,
            @RequestParam(defaultValue = "20") int limit,
            HttpSession session) {
        
        User loginUser = getLoginUser(session);
        log.info("获取智能体反馈关键词，userId: {}, agentId: {}", loginUser.getId(), agentId);
        
        Map<String, Integer> keywords = feedbackAnalysisService.getFeedbackKeywords(agentId, limit);
        return ResultUtils.success(keywords);
    }

    /**
     * 获取优化建议列表
     */
    @GetMapping("/agent/{agentId}/analysis/suggestions")
    public BaseResponse<List<Map<String, Object>>> getAgentOptimizationSuggestions(
            @PathVariable Long agentId,
            HttpSession session) {
        
        User loginUser = getLoginUser(session);
        log.info("获取智能体优化建议，userId: {}, agentId: {}", loginUser.getId(), agentId);
        
        var suggestions = feedbackAnalysisService.generateOptimizationSuggestions(agentId);
        
        List<Map<String, Object>> suggestionsList = suggestions.stream()
            .map(suggestion -> {
                Map<String, Object> suggestionMap = new HashMap<>();
                suggestionMap.put("category", suggestion.category());
                suggestionMap.put("suggestion", suggestion.suggestion());
                suggestionMap.put("reason", suggestion.reason());
                suggestionMap.put("priority", suggestion.priority());
                return suggestionMap;
            })
            .toList();
        
        return ResultUtils.success(suggestionsList);
    }
}
