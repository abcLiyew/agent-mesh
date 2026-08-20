package com.esdllm.agentmesh.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.esdllm.agentmesh.model.domain.ConversationLog;
import com.esdllm.agentmesh.model.dto.*;

import java.util.Date;
import java.util.List;

/**
 * 对话日志服务
 */
public interface ConversationLogService {

    /**
     * 记录对话日志
     * @param userId 用户 ID
     * @param agentId 智能体 ID
     * @param query 用户问题
     * @param result 执行结果
     * @return 日志 ID
     */
    Long logConversation(
            Long userId,
            Long agentId,
            String query,
            DecisionExecutionResult result
    );

    /**
     * 记录对话日志（带意图识别结果）
     * @param userId 用户 ID
     * @param agentId 智能体 ID
     * @param sessionId 会话 ID
     * @param query 用户问题
     * @param intentResult 意图识别结果
     * @param executionResult 执行结果
     * @return 日志 ID
     */
    Long logConversationWithIntent(
            Long userId,
            Long agentId,
            String sessionId,
            String query,
            IntentRecognitionResult intentResult,
            DecisionExecutionResult executionResult
    );

    /**
     * 分页查询用户的对话日志（按会话分组）
     * @param userId 用户 ID
     * @param page 页码
     * @param pageSize 每页数量
     * @return 会话组分页
     */
    Page<ConversationSessionGroup> getUserConversationSessionGroups(Long userId, int page, int pageSize);

    /**
     * 分页查询用户的对话日志
     * @param userId 用户 ID
     * @param page 页码
     * @param pageSize 每页数量
     * @return 对话日志分页
     */
    Page<ConversationLog> getUserConversationLogs(Long userId, int page, int pageSize);

    /**
     * 分页查询用户与指定智能体的对话日志
     * @param userId 用户 ID
     * @param agentId 智能体 ID
     * @param page 页码
     * @param pageSize 每页数量
     * @return 对话日志分页
     */
    Page<ConversationLog> getUserAgentConversationLogs(Long userId, Long agentId, int page, int pageSize);

    /**
     * 分页查询智能体的对话日志
     * @param agentId 智能体 ID
     * @param page 页码
     * @param pageSize 每页数量
     * @return 对话日志分页
     */
    Page<ConversationLog> getAgentConversationLogs(Long agentId, int page, int pageSize);

    /**
     * 获取会话详情
     * @param sessionId 会话 ID
     * @return 对话日志列表
     */
    List<ConversationLog> getSessionDetail(String sessionId);

    /**
     * 统计某段时间内的对话数据
     *
     * @param userId    用户 ID
     * @param startDate 开始日期
     * @param endDate   结束日期
     * @return 统计数据
     */
    ConversationStatistics getStatistics(Long userId, Date startDate, Date endDate);

    /**
     * 获取智能体的热门意图类型
     * @param agentId 智能体 ID
     * @param limit 返回数量
     * @return 意图统计列表
     */
    List<IntentStatistics> getAgentIntentStats(Long agentId, int limit);

    /**
     * 获取智能体的常用工具
     * @param agentId 智能体 ID
     * @param limit 返回数量
     * @return 工具统计列表
     */
    List<ToolUsageStatistics> getAgentToolStats(Long agentId, int limit);

    /**
     * 更新用户反馈
     * @param logId 日志 ID
     * @param rating 评分（1-5）
     * @param feedback 反馈备注
     */
    void updateFeedback(Long logId, Integer rating, String feedback);

    /**
     * 获取用户的反馈统计
     * @param userId 用户 ID
     * @return 反馈统计数据
     */
    com.esdllm.agentmesh.model.dto.FeedbackStatistics getUserFeedbackStats(Long userId);

    /**
     * 获取智能体的反馈统计
     * @param agentId 智能体 ID
     * @return 反馈统计数据
     */
    com.esdllm.agentmesh.model.dto.FeedbackStatistics getAgentFeedbackStats(Long agentId);

    /**
     * 获取反馈趋势（按天）
     * @param userId 用户 ID
     * @param days 天数
     * @return 反馈趋势列表
     */
    java.util.List<com.esdllm.agentmesh.model.dto.FeedbackTrend> getFeedbackTrend(Long userId, int days);

    /**
     * 获取低分反馈列表（用于问题分析）
     * @param agentId 智能体 ID
     * @param limit 返回数量
     * @return 低分反馈列表
     */
    java.util.List<com.esdllm.agentmesh.model.dto.FeedbackStatistics.LowRatingFeedback> 
        getLowRatingFeedbacks(Long agentId, int limit);

    /**
     * 根据反馈 ID 查询反馈详情
     * @param logId 日志 ID
     * @return 反馈详情
     */
    com.esdllm.agentmesh.model.domain.ConversationLog getFeedbackDetail(Long logId);

}
