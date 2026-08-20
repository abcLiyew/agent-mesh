package com.esdllm.agentmesh.repository.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.esdllm.agentmesh.model.domain.ConversationLog;
import com.esdllm.agentmesh.model.dto.IntentStatistics;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

/**
 * 对话日志 Mapper 接口
 */
@Mapper
public interface ConversationLogMapper extends BaseMapper<ConversationLog> {

    /**
     * 查询热门意图类型
     * @param agentId 智能体 ID
     * @param limit 返回数量
     * @return 意图统计列表
     */
    List<IntentStatistics> getTopIntentTypes(@Param("agentId") Long agentId, @Param("limit") int limit);

    /**
     * 查询常用工具（原始数据）
     * @param agentId 智能体 ID
     * @param limit 返回数量
     * @return 工具使用原始数据
     */
    List<Map<String, Object>> getTopUsedToolsRaw(@Param("agentId") Long agentId, @Param("limit") int limit);

    /**
     * 查询常用工具（转换为 ToolUsageStatistics）
     * @param agentId 智能体 ID
     * @param limit 返回数量
     * @return 工具使用统计列表
     */
    List<com.esdllm.agentmesh.model.dto.ToolUsageStatistics> getTopUsedTools(@Param("agentId") Long agentId, @Param("limit") int limit);

    /**
     * 查询低分反馈记录
     * @param agentId 智能体 ID
     * @param limit 返回数量
     * @return 对话日志列表
     */
    List<ConversationLog> getLowRatingFeedbacks(@Param("agentId") Long agentId, @Param("limit") int limit);

    /**
     * 统计活跃会话数（最近指定天数内的会话）
     * @param userId 用户 ID
     * @param days 天数
     * @return 活跃会话数
     */
    Long countActiveSessions(@Param("userId") Long userId, @Param("days") int days);
}
