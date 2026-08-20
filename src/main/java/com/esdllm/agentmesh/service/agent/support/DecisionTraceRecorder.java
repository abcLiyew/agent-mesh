package com.esdllm.agentmesh.service.agent.support;

import com.esdllm.agentmesh.model.dto.DecisionExecutionResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 调用链追踪记录器：负责记录和统计工具调用性能
 */
@Service
@Slf4j
public class DecisionTraceRecorder {

    /**
     * 记录工具调用性能
     */
    public void recordToolPerformance(Map<String, Object> traceContext, Long toolId,
                                      String toolCodeName, String toolType,
                                      long executionTime, boolean success) {
        Object callChainObj = traceContext.get("call_chain");
        if (!(callChainObj instanceof List)) {
            log.warn("调用链数据结构异常");
            return;
        }
        
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> callChain = (List<Map<String, Object>>) callChainObj;
        
        Map<String, Object> perfRecord = new LinkedHashMap<>();
        perfRecord.put("type", "TOOL_PERFORMANCE");
        perfRecord.put("timestamp", System.currentTimeMillis());
        perfRecord.put("tool_id", toolId);
        perfRecord.put("tool_code_name", toolCodeName);
        perfRecord.put("tool_type", toolType);
        perfRecord.put("execution_time_ms", executionTime);
        perfRecord.put("success", success);
        
        callChain.add(perfRecord);
    }

    /**
     * 记录智能体工具调用
     */
    @SuppressWarnings("unchecked")
    public void recordAgentToolCall(Map<String, Object> traceContext, Long agentId,
                                    String toolCodeName, long executionTime, boolean success) {
        List<Map<String, Object>> callChain = (List<Map<String, Object>>) traceContext.get("call_chain");
        if (callChain != null) {
            Map<String, Object> callRecord = new HashMap<>();
            callRecord.put("agent_id", agentId);
            callRecord.put("tool_code_name", toolCodeName);
            callRecord.put("execution_time_ms", executionTime);
            callRecord.put("success", success);
            callRecord.put("timestamp", System.currentTimeMillis());
            callRecord.put("type", "AGENT_TOOL");
            callChain.add(callRecord);
        }
    }

    /**
     * 记录调用链汇总信息并打印日志
     */
    @SuppressWarnings("unchecked")
    public void recordCallChainSummary(Map<String, Object> traceContext, Long agentId, long totalExecutionTime) {
        Object callChainObj = traceContext.get("call_chain");
        if (!(callChainObj instanceof List)) {
            return;
        }
        
        List<Map<String, Object>> callChain = (List<Map<String, Object>>) callChainObj;
        if (callChain.isEmpty()) {
            return;
        }

        log.info("=== 智能体调用链追踪 ===");
        log.info("根智能体 ID: {}, 总调用次数：{}, 总耗时：{}ms", agentId, callChain.size(), totalExecutionTime);

        CallChainStats stats = analyzeCallChain(callChain);

        log.info("智能体调用：{} 次，普通工具调用：{} 次", stats.agentToolCalls, stats.toolCalls);
        log.info("成功率：{:.1f}%, 平均耗时：{}ms", 
                stats.successRate, stats.avgExecutionTime);

        if (log.isDebugEnabled()) {
            callChain.forEach(record -> log.debug("调用记录：type={}, id={}, tool={}, time={}ms, success={}",
                    record.get("type"),
                    record.get("agent_id") != null ? record.get("agent_id") : record.get("tool_id"),
                    record.get("tool_code_name"),
                    record.get("execution_time_ms"),
                    record.get("success")));
        }

        traceContext.put("stats_summary", buildStatsMap(stats));
    }

    private CallChainStats analyzeCallChain(List<Map<String, Object>> callChain) {
        CallChainStats stats = new CallChainStats();
        
        stats.agentToolCalls = callChain.stream()
                .filter(r -> "AGENT_TOOL".equals(r.get("type")))
                .count();

        stats.toolCalls = callChain.stream()
                .filter(r -> "TOOL_PERFORMANCE".equals(r.get("type")))
                .count();

        stats.successCount = callChain.stream()
                .filter(r -> Boolean.TRUE.equals(r.get("success")))
                .count();

        stats.failureCount = callChain.stream()
                .filter(r -> Boolean.FALSE.equals(r.get("success")))
                .count();

        stats.totalCalls = callChain.size();
        stats.successRate = stats.totalCalls > 0 ? 
                (double) stats.successCount / stats.totalCalls * 100 : 0.0;

        stats.avgExecutionTime = callChain.stream()
                .mapToLong(r -> extractExecutionTime(r))
                .average()
                .orElse(0.0);

        stats.maxExecutionTime = callChain.stream()
                .mapToLong(r -> extractExecutionTime(r))
                .max()
                .orElse(0L);

        stats.minExecutionTime = callChain.stream()
                .mapToLong(r -> extractExecutionTime(r))
                .min()
                .orElse(0L);

        return stats;
    }

    private long extractExecutionTime(Map<String, Object> record) {
        Object timeObj = record.get("execution_time_ms");
        if (timeObj instanceof Number) {
            return ((Number) timeObj).longValue();
        }
        return 0L;
    }

    private Map<String, Object> buildStatsMap(CallChainStats stats) {
        return Map.of(
                "total_calls", stats.totalCalls,
                "agent_tool_calls", (int) stats.agentToolCalls,
                "tool_calls", (int) stats.toolCalls,
                "success_count", (int) stats.successCount,
                "failure_count", (int) stats.failureCount,
                "success_rate", stats.successRate,
                "avg_execution_time_ms", stats.avgExecutionTime,
                "max_execution_time_ms", stats.maxExecutionTime,
                "min_execution_time_ms", stats.minExecutionTime
        );
    }

    private static class CallChainStats {
        long totalCalls;
        long agentToolCalls;
        long toolCalls;
        long successCount;
        long failureCount;
        double successRate;
        double avgExecutionTime;
        long maxExecutionTime;
        long minExecutionTime;
    }

    /**
     * 构建调用链追踪信息到结果对象
     */
    @SuppressWarnings("unchecked")
    public void buildCallChainTrace(Map<String, Object> traceContext, Long agentId,
                                    DecisionExecutionResult result) {
        List<Map<String, Object>> callChain = (List<Map<String, Object>>) traceContext.get("call_chain");
        if (callChain == null || callChain.isEmpty()) {
            return;
        }

        DecisionExecutionResult.CallChainTrace trace = new DecisionExecutionResult.CallChainTrace();
        trace.setRootAgentId(agentId);
        trace.setCallRecords(new ArrayList<>(callChain));

        try {
            StringBuilder topology = buildCallTopology(callChain);
            trace.setCallTopology(topology.toString());
        } catch (Exception e) {
            log.error("构建调用拓扑失败", e);
            trace.setCallTopology("{}");
        }

        result.setCallChainTrace(trace);
        result.setPerformanceStats(buildPerformanceStats(callChain));
    }

    // ==================== 辅助方法 ====================

    private StringBuilder buildCallTopology(List<Map<String, Object>> callChain) {
        StringBuilder topology = new StringBuilder("{");
        topology.append("\"root\":").append(callChain.getFirst().get("agent_id") != null ?
                callChain.getFirst().get("agent_id") : "unknown").append(",");
        topology.append("\"calls\":[");

        for (int i = 0; i < callChain.size(); i++) {
            Map<String, Object> record = callChain.get(i);
            if (i > 0) topology.append(",");
            topology.append("{");
            topology.append("\"type\":\"").append(record.get("type")).append("\",");
            if (record.get("agent_id") != null) {
                topology.append("\"agent_id\":").append(record.get("agent_id")).append(",");
            }
            if (record.get("tool_id") != null) {
                topology.append("\"tool_id\":").append(record.get("tool_id")).append(",");
            }
            topology.append("\"tool\":\"").append(record.get("tool_code_name")).append("\",");
            topology.append("\"time\":").append(record.get("execution_time_ms")).append(",");
            topology.append("\"success\":").append(record.get("success"));
            topology.append("}");
        }

        topology.append("]}");
        return topology;
    }

    private DecisionExecutionResult.PerformanceStats buildPerformanceStats(List<Map<String, Object>> callChain) {
        DecisionExecutionResult.PerformanceStats stats = new DecisionExecutionResult.PerformanceStats();

        long agentToolCalls = callChain.stream()
                .filter(r -> "AGENT_TOOL".equals(r.get("type")))
                .count();

        long toolCalls = callChain.stream()
                .filter(r -> "TOOL_PERFORMANCE".equals(r.get("type")))
                .count();

        long successCalls = callChain.stream()
                .filter(r -> Boolean.TRUE.equals(r.get("success")))
                .count();

        long failedCalls = callChain.stream()
                .filter(r -> Boolean.FALSE.equals(r.get("success")))
                .count();

        double avgExecutionTime = callChain.stream()
                .mapToLong(r -> ((Number) r.get("execution_time_ms")).longValue())
                .average()
                .orElse(0.0);

        long maxTime = callChain.stream()
                .mapToLong(r -> ((Number) r.get("execution_time_ms")).longValue())
                .max()
                .orElse(0L);

        long minTime = callChain.stream()
                .mapToLong(r -> ((Number) r.get("execution_time_ms")).longValue())
                .min()
                .orElse(0L);

        stats.setTotalCalls(callChain.size());
        stats.setAgentToolCalls((int) agentToolCalls);
        stats.setToolCalls((int) toolCalls);
        stats.setSuccessCount((int) successCalls);
        stats.setFailureCount((int) failedCalls);
        stats.setAvgExecutionTimeMs(avgExecutionTime);
        stats.setMaxExecutionTimeMs(maxTime);
        stats.setMinExecutionTimeMs(minTime);

        return stats;
    }
}
