package com.esdllm.agentmesh.repository.dao;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.esdllm.agentmesh.model.domain.ConversationLog;
import com.esdllm.agentmesh.model.dto.IntentStatistics;
import com.esdllm.agentmesh.model.dto.ToolUsageStatistics;

import java.util.Date;
import java.util.List;

/**
 * 对话日志数据访问对象
 */
public interface ConversationLogDao extends IService<ConversationLog> {

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
     * 按会话 ID 查询对话日志
     * @param sessionId 会话 ID
     * @return 对话日志列表
     */
    List<ConversationLog> getBySessionId(String sessionId);

    /**
     * 统计某段时间内的对话次数
     * @param userId 用户 ID
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 对话次数
     */
    Long countConversations(Long userId, Date startDate, Date endDate);

    /**
     * 统计某段时间内的总成本
     * @param userId 用户 ID
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 总成本
     */
    Double getTotalCost(Long userId, Date startDate, Date endDate);

    /**
     * 统计平均响应时间
     * @param agentId 智能体 ID
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 平均响应时间（毫秒）
     */
    Long getAverageResponseTime(Long agentId, Date startDate, Date endDate);

    /**
     * 统计成功率
     * @param agentId 智能体 ID
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 成功率（0-100）
     */
    Double getSuccessRate(Long agentId, Date startDate, Date endDate);

    /**
     * 查询热门意图类型
     * @param agentId 智能体 ID
     * @param limit 返回数量限制
     * @return 意图类型及次数
     */
    List<IntentStatistics> getTopIntentTypes(Long agentId, int limit);

    /**
     * 查询常用工具
     * @param agentId 智能体 ID
     * @param limit 返回数量限制
     * @return 工具 ID 及使用次数
     */
    List<ToolUsageStatistics> getTopUsedTools(Long agentId, int limit);

    /**
     * 根据用户 ID 查询有反馈的记录
     * @param userId 用户 ID
     * @return 对话日志列表
     */
    List<ConversationLog> getFeedbackByUserId(Long userId);

    /**
     * 根据智能体 ID 查询有反馈的记录
     * @param agentId 智能体 ID
     * @return 对话日志列表
     */
    List<ConversationLog> getFeedbackByAgentId(Long agentId);

    /**
     * 查询指定日期范围内的反馈
     * @param userId 用户 ID
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 对话日志列表
     */
    List<ConversationLog> getFeedbackInDateRange(Long userId, Date startDate, Date endDate);

    /**
     * 查询低分反馈（1-2 星）
     * @param agentId 智能体 ID
     * @param limit 返回数量
     * @return 对话日志列表
     */
    List<ConversationLog> getLowRatingFeedbacks(Long agentId, int limit);

    /**
     * 统计活跃会话数（最近指定天数内的会话）
     * @param userId 用户 ID
     * @param days 天数
     * @return 活跃会话数
     */
    Long countActiveSessions(Long userId, int days);

}
