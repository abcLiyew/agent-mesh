package com.esdllm.agentmesh.service.unified.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.esdllm.agentmesh.common.ErrorCode;
import com.esdllm.agentmesh.exception.BusinessException;
import com.esdllm.agentmesh.model.domain.Agent;
import com.esdllm.agentmesh.model.domain.AgentDependencyEntity;
import com.esdllm.agentmesh.model.dto.DecisionExecutionResult;
import com.esdllm.agentmesh.repository.dao.AgentDao;
import com.esdllm.agentmesh.repository.dao.AgentDependencyDao;
import jakarta.annotation.Resource;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

/**
 * 智能体编排器 - 实现多智能体协同机制
 * 
 * 职责:
 * 1. 任务分发:根据任务类型分发给合适的子智能体
 * 2. 结果合并:汇总多个智能体的执行结果
 * 3. 依赖管理:处理智能体之间的调用关系
 * 
 * 参考"龙虾"架构的协作模式
 */
@Component
@Slf4j
public class AgentOrchestrator {
    
    @Resource
    private AgentDao agentDao;
    
    @Resource
    private AgentDependencyDao agentDependencyDao;
    
    @Resource
    @Lazy
    private UnifiedAgentEngineImpl unifiedAgentEngine;
    
    private final ExecutorService executor = Executors.newFixedThreadPool(10);
    
    /**
     * 多智能体协同执行
     * 
     * @param mainAgentId 主智能体ID
     * @param query 用户查询
     * @param userId 用户ID
     * @param context 上下文
     * @return 协同执行结果
     */
    public CollaborativeExecutionResult executeCollaboratively(
            Long mainAgentId, String query, Long userId, Map<String, Object> context) {
        
        log.info("=== 开始多智能体协同执行 ===");
        log.info("主智能体: {}, 查询: {}", mainAgentId, query);
        
        long startTime = System.currentTimeMillis();
        
        try {
            // 1. 分析任务是否需要多智能体协作
            CollaborationPlan plan = analyzeCollaborationNeed(mainAgentId, query, userId);
            
            if (!plan.isNeedsCollaboration()) {
                log.info("任务不需要协作,使用单智能体执行");
                DecisionExecutionResult result = unifiedAgentEngine.execute(
                    mainAgentId, query, userId, null, context
                );
                
                return convertToCollaborativeResult(result, Collections.emptyList());
            }
            
            log.info("检测到需要协作,参与智能体数: {}", plan.getSubAgents().size());
            
            // 2. 并行执行子智能体任务
            List<SubAgentResult> subResults = executeSubAgentsInParallel(plan, query, userId, context);
            
            // 3. 合并结果
            DecisionExecutionResult mergedResult = mergeSubAgentResults(plan, subResults, query);
            
            long executionTime = System.currentTimeMillis() - startTime;
            mergedResult.setExecutionTimeMs(executionTime);
            
            log.info("=== 多智能体协同执行完成 === 耗时: {}ms", executionTime);
            
            return new CollaborativeExecutionResult(
                true,
                mergedResult,
                plan.getSubAgents(),
                subResults,
                executionTime
            );
            
        } catch (Exception e) {
            log.error("多智能体协同执行失败", e);
            
            return new CollaborativeExecutionResult(
                false,
                createErrorResult(e.getMessage()),
                Collections.emptyList(),
                Collections.emptyList(),
                System.currentTimeMillis() - startTime
            );
        }
    }
    
    /**
     * 分析协作需求
     */
    private CollaborationPlan analyzeCollaborationNeed(Long mainAgentId, String query, Long userId) {
        log.info("分析协作需求...");
        
        // 1. 获取主智能体信息
        Agent mainAgent = agentDao.getById(mainAgentId);
        if (mainAgent == null) {
            throw new BusinessException(ErrorCode.AGENT_NOT_FOUND, "主智能体不存在");
        }
        
        // 2. 检查是否有依赖的智能体
        List<SubAgentInfo> dependentAgents = getDependentAgents(mainAgentId, userId);
        
        // 3. 基于任务复杂度判断是否需要协作
        boolean needsCollaboration = evaluateCollaborationNeed(query, dependentAgents);
        
        log.info("协作需求分析完成: needsCollaboration={}, subAgents={}", 
                needsCollaboration, dependentAgents.size());
        
        return CollaborationPlan.builder()
            .mainAgentId(mainAgentId)
            .subAgents(dependentAgents)
            .needsCollaboration(needsCollaboration)
            .collaborationStrategy(determineStrategy(dependentAgents))
            .build();
    }
    
    /**
     * 获取依赖的子智能体列表
     */
    private List<SubAgentInfo> getDependentAgents(Long mainAgentId, Long userId) {
        log.debug("查询智能体 {} 的依赖关系", mainAgentId);
        
        try {
            // 从数据库查询agent_dependency表
            List<AgentDependencyEntity> dependencies = agentDependencyDao.listByAgentId(mainAgentId);
            
            if (dependencies == null || dependencies.isEmpty()) {
                log.debug("智能体 {} 没有配置依赖关系", mainAgentId);
                return Collections.emptyList();
            }
            
            // 过滤并按优先级排序（当前表结构中没有isEnabled字段，默认全部启用）
            List<SubAgentInfo> subAgents = dependencies.stream()
                .sorted(Comparator.comparingInt(dep -> dep.getPriority() != null ? dep.getPriority() : 999))
                .map(dep -> {
                    Agent agent = agentDao.getById(dep.getDependsOnAgentId());
                    if (agent == null) {
                        log.warn("依赖的智能体不存在: dependencyId={}", dep.getId());
                        return null;
                    }
                    
                    return SubAgentInfo.builder()
                        .agentId(agent.getId())
                        .agentName(agent.getName())
                        .role(agent.getRoleDefinition())
                        .priority(dep.getPriority())
                        .build();
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
            
            log.info("找到 {} 个依赖的子智能体", subAgents.size());
            return subAgents;
            
        } catch (Exception e) {
            log.error("查询依赖关系失败", e);
            return Collections.emptyList();
        }
    }
    
    /**
     * 评估是否需要协作
     */
    private boolean evaluateCollaborationNeed(String query, List<SubAgentInfo> dependentAgents) {
        // 判断标准1:有依赖的智能体
        if (!dependentAgents.isEmpty()) {
            return true;
        }
        
        // 判断标准2:任务涉及多个领域
        String[] multiDomainKeywords = {"对比", "分析", "综合", "总结", "报告"};
        for (String keyword : multiDomainKeywords) {
            if (query.contains(keyword)) {
                return true;
            }
        }
        
        // 判断标准3:查询特别复杂
        if (query.length() > 100) {
            return true;
        }
        
        return false;
    }
    
    /**
     * 确定协作策略
     */
    private String determineStrategy(List<SubAgentInfo> subAgents) {
        if (subAgents.isEmpty()) {
            return "SINGLE_AGENT";
        }
        
        // 简单策略:并行执行
        return "PARALLEL_EXECUTION";
    }
    
    /**
     * 并行执行子智能体
     */
    private List<SubAgentResult> executeSubAgentsInParallel(
            CollaborationPlan plan, String query, Long userId, Map<String, Object> context) {
        
        log.info("并行执行 {} 个子智能体", plan.getSubAgents().size());
        
        List<CompletableFuture<SubAgentResult>> futures = plan.getSubAgents().stream()
            .map(subAgent -> CompletableFuture.supplyAsync(() -> {
                try {
                    log.info("执行子智能体: {} ({})", subAgent.getAgentName(), subAgent.getAgentId());
                    
                    // 为子智能体生成特定的查询
                    String subQuery = generateSubAgentQuery(query, subAgent);
                    
                    long subStartTime = System.currentTimeMillis();
                    DecisionExecutionResult result = unifiedAgentEngine.execute(
                        subAgent.getAgentId(), subQuery, userId, null, context
                    );
                    long subExecutionTime = System.currentTimeMillis() - subStartTime;
                    
                    log.info("子智能体 {} 执行完成,耗时: {}ms", subAgent.getAgentId(), subExecutionTime);
                    
                    return SubAgentResult.builder()
                        .agentId(subAgent.getAgentId())
                        .agentName(subAgent.getAgentName())
                        .result(result)
                        .executionTimeMs(subExecutionTime)
                        .success(result.getSuccess())
                        .build();
                        
                } catch (Exception e) {
                    log.error("子智能体 {} 执行失败", subAgent.getAgentId(), e);
                    
                    return SubAgentResult.builder()
                        .agentId(subAgent.getAgentId())
                        .agentName(subAgent.getAgentName())
                        .result(createErrorResult(e.getMessage()))
                        .executionTimeMs(0L)
                        .success(false)
                        .errorMessage(e.getMessage())
                        .build();
                }
            }, executor))
            .collect(Collectors.toList());
        
        // 等待所有子智能体执行完成
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        
        return futures.stream()
            .map(CompletableFuture::join)
            .collect(Collectors.toList());
    }
    
    /**
     * 为子智能体生成特定查询
     */
    private String generateSubAgentQuery(String originalQuery, SubAgentInfo subAgent) {
        // 简化实现:直接使用原查询
        // TODO: 可以根据子智能体的角色定制查询
        
        return originalQuery;
    }
    
    /**
     * 合并子智能体结果
     */
    private DecisionExecutionResult mergeSubAgentResults(
            CollaborationPlan plan, List<SubAgentResult> subResults, String originalQuery) {
        
        log.info("合并 {} 个子智能体的结果", subResults.size());
        
        StringBuilder mergedResponse = new StringBuilder();
        mergedResponse.append("### 多智能体协同执行结果\n\n");
        
        // 统计成功/失败
        long successCount = subResults.stream().filter(SubAgentResult::isSuccess).count();
        long failCount = subResults.size() - successCount;
        
        mergedResponse.append(String.format("**执行统计**: 总计 %d 个智能体, 成功 %d 个, 失败 %d 个\n\n",
            subResults.size(), successCount, failCount));
        
        // 汇总各智能体的结果
        for (int i = 0; i < subResults.size(); i++) {
            SubAgentResult subResult = subResults.get(i);
            
            mergedResponse.append(String.format("#### %d. %s\n\n", i + 1, subResult.getAgentName()));
            
            if (subResult.isSuccess()) {
                mergedResponse.append(subResult.getResult().getFinalResponse());
            } else {
                mergedResponse.append(String.format("**执行失败**: %s\n", subResult.getErrorMessage()));
            }
            
            mergedResponse.append("\n\n---\n\n");
        }
        
        // 生成最终总结
        mergedResponse.append("### 总结\n\n");
        mergedResponse.append("以上是由多个智能体协同完成的分析结果。");
        
        DecisionExecutionResult result = new DecisionExecutionResult();
        result.setSuccess(successCount > 0);
        result.setFinalResponse(mergedResponse.toString());
        result.setDecisionPath(buildCollaborativeDecisionPath(subResults));
        
        return result;
    }
    
    /**
     * 构建协同决策路径
     */
    private List<com.esdllm.agentmesh.model.dto.DecisionStep> buildCollaborativeDecisionPath(
            List<SubAgentResult> subResults) {
        
        List<com.esdllm.agentmesh.model.dto.DecisionStep> decisionPath = new ArrayList<>();
        
        for (SubAgentResult subResult : subResults) {
            var step = new com.esdllm.agentmesh.model.dto.DecisionStep();
            step.setStepId("sub_agent_" + subResult.getAgentId());
            step.setStepType("AGENT_COLLABORATION");
            step.setDescription("调用子智能体: " + subResult.getAgentName());
            step.setStatus(subResult.isSuccess() ? "COMPLETED" : "FAILED");
            step.setDurationMs(subResult.getExecutionTimeMs());
            
            decisionPath.add(step);
        }
        
        return decisionPath;
    }
    
    /**
     * 转换为协同执行结果
     */
    private CollaborativeExecutionResult convertToCollaborativeResult(
            DecisionExecutionResult singleResult, List<SubAgentInfo> subAgents) {
        
        return new CollaborativeExecutionResult(
            true,
            singleResult,
            subAgents,
            Collections.emptyList(),
            singleResult.getExecutionTimeMs()
        );
    }
    
    /**
     * 创建错误结果
     */
    private DecisionExecutionResult createErrorResult(String errorMessage) {
        DecisionExecutionResult result = new DecisionExecutionResult();
        result.setSuccess(false);
        result.setErrorMessage(errorMessage);
        return result;
    }
    
    // ========== 内部类 ==========
    
    /**
     * 协作计划
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CollaborationPlan {
        private Long mainAgentId;
        private List<SubAgentInfo> subAgents;
        private boolean needsCollaboration;
        private String collaborationStrategy;
    }
    
    /**
     * 子智能体信息
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SubAgentInfo {
        private Long agentId;
        private String agentName;
        private String role;
        private Integer priority;
    }
    
    /**
     * 子智能体执行结果
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SubAgentResult {
        private Long agentId;
        private String agentName;
        private DecisionExecutionResult result;
        private Long executionTimeMs;
        private boolean success;
        private String errorMessage;
    }
    
    /**
     * 协同执行结果
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CollaborativeExecutionResult {
        private boolean success;
        private DecisionExecutionResult mergedResult;
        private List<SubAgentInfo> participatingAgents;
        private List<SubAgentResult> subAgentResults;
        private Long totalExecutionTimeMs;
    }
}
