package com.esdllm.agentmesh.service.agent.support;


import com.esdllm.agentmesh.model.dto.DecisionExecutionResult;
import com.esdllm.agentmesh.model.dto.DecisionStep;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 决策路径追踪服务
 * 用于记录和查询决策执行路径
 */
@Service
@Slf4j
public class DecisionPathTracker {

    /**
     * 内存存储决策路径（生产环境建议使用数据库）
     * key: sessionId, value: 决策路径列表
     */
    private final Map<String, List<DecisionStep>> decisionPathCache = new ConcurrentHashMap<>();

    /**
     * 记录决策路径
     */
    public void recordDecisionPath(String sessionId, List<DecisionStep> decisionPath) {
        if (sessionId == null || decisionPath == null) {
            log.warn("sessionId 或 decisionPath 为 null");
            return;
        }

        // 为每个步骤生成唯一 ID 和状态
        for (int i = 0; i < decisionPath.size(); i++) {
            DecisionStep step = decisionPath.get(i);
            if (step.getStepId() == null) {
                step.setStepId(generateStepId(step.getStepType(), i));
            }
            if (step.getStatus() == null) {
                step.setStatus(determineStepStatus(step));
            }
        }

        decisionPathCache.put(sessionId, new ArrayList<>(decisionPath));
        log.info("记录决策路径成功，sessionId: {}, 步骤数：{}", sessionId, decisionPath.size());
    }

    /**
     * 获取决策路径
     */
    public List<DecisionStep> getDecisionPath(String sessionId) {
        return decisionPathCache.getOrDefault(sessionId, new ArrayList<>());
    }

    /**
     * 获取最近的决策路径（按时间倒序）
     */
    public List<Map.Entry<String, List<DecisionStep>>> getRecentDecisionPaths(int limit) {
        return decisionPathCache.entrySet().stream()
                .limit(limit)
                .toList();
    }

    /**
     * 清除过期的决策路径
     */
    public void clearExpiredPaths(long expireTimeMs) {
        // 简单实现：清空所有（实际应该根据时间戳清理）
        decisionPathCache.clear();
        log.info("已清除所有决策路径缓存");
    }

    /**
     * 从执行结果中提取决策路径并记录
     */
    public void recordFromExecutionResult(String sessionId, DecisionExecutionResult result) {
        if (result != null && result.getDecisionPath() != null) {
            recordDecisionPath(sessionId, result.getDecisionPath());
        }
    }

    /**
     * 生成步骤 ID
     */
    private String generateStepId(String stepType, int index) {
        String prefix = switch (stepType) {
            case "INTENT_RECOGNITION" -> "INTENT";
            case "TOOL_MATCHING" -> "MATCH";
            case "TOOL_CALL" -> "TOOL";
            case "MODEL_RESPONSE" -> "MODEL";
            default -> "STEP";
        };
        return prefix + "_" + System.currentTimeMillis() + "_" + index;
    }

    /**
     * 判断步骤状态
     */
    private String determineStepStatus(DecisionStep step) {
        if (step.getErrorMessage() != null && !step.getErrorMessage().isEmpty()) {
            return "FAILED";
        }
        if (step.getOutputData() != null) {
            return "COMPLETED";
        }
        return "PENDING";
    }

    /**
     * 获取统计信息
     */
    public Map<String, Object> getStatistics() {
        return Map.of(
                "totalSessions", decisionPathCache.size(),
                "cacheSize", decisionPathCache.size()
        );
    }
}
