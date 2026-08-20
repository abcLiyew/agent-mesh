package com.esdllm.agentmesh.repository.dao.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.esdllm.agentmesh.model.domain.ConversationLog;
import com.esdllm.agentmesh.model.dto.IntentStatistics;
import com.esdllm.agentmesh.model.dto.ToolUsageStatistics;
import com.esdllm.agentmesh.repository.dao.ConversationLogDao;
import com.esdllm.agentmesh.repository.mapper.ConversationLogMapper;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;

/**
 * 对话日志数据访问对象实现
 */
@Repository
public class ConversationLogDaoImpl extends ServiceImpl<ConversationLogMapper, ConversationLog> 
    implements ConversationLogDao {

    @Override
    public Page<ConversationLog> getUserConversationLogs(Long userId, int page, int pageSize) {
        LambdaQueryWrapper<ConversationLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ConversationLog::getUserId, userId)
               .orderByDesc(ConversationLog::getCreatedAt);
        
        return this.page(new Page<>(page, pageSize), wrapper);
    }

    @Override
    public Page<ConversationLog> getUserAgentConversationLogs(Long userId, Long agentId, int page, int pageSize) {
        LambdaQueryWrapper<ConversationLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ConversationLog::getUserId, userId)
               .eq(ConversationLog::getAgentId, agentId)
               .orderByDesc(ConversationLog::getCreatedAt);
        
        return this.page(new Page<>(page, pageSize), wrapper);
    }

    @Override
    public Page<ConversationLog> getAgentConversationLogs(Long agentId, int page, int pageSize) {
        LambdaQueryWrapper<ConversationLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ConversationLog::getAgentId, agentId)
               .orderByDesc(ConversationLog::getCreatedAt);
        
        return this.page(new Page<>(page, pageSize), wrapper);
    }

    @Override
    public List<ConversationLog> getBySessionId(String sessionId) {
        LambdaQueryWrapper<ConversationLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ConversationLog::getSessionId, sessionId)
               .orderByAsc(ConversationLog::getCreatedAt);
        
        return this.list(wrapper);
    }

    @Override
    public Long countConversations(Long userId, Date startDate, Date endDate) {
        LambdaQueryWrapper<ConversationLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ConversationLog::getUserId, userId);
        
        if (startDate != null) {
            wrapper.ge(ConversationLog::getCreatedAt, startDate);
        }
        if (endDate != null) {
            wrapper.le(ConversationLog::getCreatedAt, endDate);
        }
        
        return this.count(wrapper);
    }

    @Override
    public Double getTotalCost(Long userId, Date startDate, Date endDate) {
        LambdaQueryWrapper<ConversationLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ConversationLog::getUserId, userId)
               .select(ConversationLog::getTotalCost);
        
        if (startDate != null) {
            wrapper.ge(ConversationLog::getCreatedAt, startDate);
        }
        if (endDate != null) {
            wrapper.le(ConversationLog::getCreatedAt, endDate);
        }
        
        return this.list(wrapper).stream()
            .mapToDouble(log -> log.getTotalCost() != null ? 
                log.getTotalCost().doubleValue() : 0.0)
            .sum();
    }

    @Override
    public Long getAverageResponseTime(Long agentId, Date startDate, Date endDate) {
        LambdaQueryWrapper<ConversationLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ConversationLog::getAgentId, agentId)
               .select(ConversationLog::getExecutionTimeMs);
        
        if (startDate != null) {
            wrapper.ge(ConversationLog::getCreatedAt, startDate);
        }
        if (endDate != null) {
            wrapper.le(ConversationLog::getCreatedAt, endDate);
        }
        
        return (long) this.list(wrapper).stream()
            .mapToLong(log -> log.getExecutionTimeMs() != null ? 
                log.getExecutionTimeMs() : 0L)
            .average()
            .orElse(0L);
    }

    @Override
    public Double getSuccessRate(Long agentId, Date startDate, Date endDate) {
        LambdaQueryWrapper<ConversationLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ConversationLog::getAgentId, agentId)
               .select(ConversationLog::getStatus);
        
        if (startDate != null) {
            wrapper.ge(ConversationLog::getCreatedAt, startDate);
        }
        if (endDate != null) {
            wrapper.le(ConversationLog::getCreatedAt, endDate);
        }
        
        List<ConversationLog> logs = this.list(wrapper);
        if (logs.isEmpty()) {
            return 0.0;
        }
        
        long successCount = logs.stream()
            .filter(log -> log.getStatus() != null && log.getStatus() == 1)
            .count();
        
        return (double) successCount / logs.size() * 100;
    }

    @Override
    public List<IntentStatistics> getTopIntentTypes(Long agentId, int limit) {
        return baseMapper.getTopIntentTypes(agentId, limit);
    }

    @Override
    public List<ToolUsageStatistics> getTopUsedTools(Long agentId, int limit) {
        return baseMapper.getTopUsedTools(agentId, limit);
    }

    @Override
    public List<ConversationLog> getFeedbackByUserId(Long userId) {
        LambdaQueryWrapper<ConversationLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ConversationLog::getUserId, userId)
               .isNotNull(ConversationLog::getUserRating)
               .orderByDesc(ConversationLog::getCreatedAt);
        
        return this.list(wrapper);
    }

    @Override
    public List<ConversationLog> getFeedbackByAgentId(Long agentId) {
        LambdaQueryWrapper<ConversationLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ConversationLog::getAgentId, agentId)
               .isNotNull(ConversationLog::getUserRating)
               .orderByDesc(ConversationLog::getCreatedAt);
        
        return this.list(wrapper);
    }

    @Override
    public List<ConversationLog> getFeedbackInDateRange(Long userId, Date startDate, Date endDate) {
        LambdaQueryWrapper<ConversationLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ConversationLog::getUserId, userId)
               .isNotNull(ConversationLog::getUserRating)
               .between(ConversationLog::getCreatedAt, startDate, endDate);
        
        return this.list(wrapper);
    }

    @Override
    public List<ConversationLog> getLowRatingFeedbacks(Long agentId, int limit) {
        LambdaQueryWrapper<ConversationLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ConversationLog::getAgentId, agentId)
               .isNotNull(ConversationLog::getUserRating)
               .le(ConversationLog::getUserRating, 2)
               .orderByDesc(ConversationLog::getCreatedAt);
        
        Page<ConversationLog> page = new Page<>(1, limit);
        return this.page(page, wrapper).getRecords();
    }

    @Override
    public Long countActiveSessions(Long userId, int days) {
        Date startDate = new Date(System.currentTimeMillis() - days * 24L * 60 * 60 * 1000);
        LambdaQueryWrapper<ConversationLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ConversationLog::getUserId, userId)
               .ge(ConversationLog::getCreatedAt, startDate);
        
        return this.list(wrapper).stream()
            .map(ConversationLog::getSessionId)
            .distinct()
            .count();
    }
}
