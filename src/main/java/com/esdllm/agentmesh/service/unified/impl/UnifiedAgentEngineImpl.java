

package com.esdllm.agentmesh.service.unified.impl;

import cn.hutool.core.util.StrUtil;
import com.esdllm.agentmesh.common.ErrorCode;
import com.esdllm.agentmesh.exception.BusinessException;
import com.esdllm.agentmesh.model.domain.Agent;
import com.esdllm.agentmesh.model.dto.DecisionExecutionResult;
import com.esdllm.agentmesh.model.dto.IntentRecognitionResult;
import com.esdllm.agentmesh.model.dto.TaskExecutionPlan;
import com.esdllm.agentmesh.model.dto.WorkflowTemplate;
import com.esdllm.agentmesh.repository.dao.AiModelDao;
import com.esdllm.agentmesh.repository.dao.AgentDao;
import com.esdllm.agentmesh.repository.dao.ModelProviderDao;
import com.esdllm.agentmesh.service.agent.support.AiModelSupport;
import com.esdllm.agentmesh.service.unified.LongTermMemoryService;
import com.esdllm.agentmesh.service.unified.SkillMarketService;
import com.esdllm.agentmesh.service.unified.UnifiedAgentEngine;
import com.esdllm.agentmesh.service.workflow.WorkflowEngine;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 统一的智能体工作流引擎实现(精简版)
 * 职责:协调各个组件,实现"龙虾"架构的感知-决策-执行-反馈闭环
 */
@Service
@Slf4j
public class UnifiedAgentEngineImpl implements UnifiedAgentEngine {
    @Resource
    private WorkflowEngine workflowEngine;
    
    @Resource
    private LongTermMemoryService memoryService;
    
    @Resource
    private SkillMarketService skillMarketService;
    
    @Resource
    private AgentDao agentDao;
    
    @Resource
    private AiModelDao aiModelDao;
    
    @Resource
    private ModelProviderDao modelProviderDao;
    
    @Resource
    private AiModelSupport aiModelSupport;
    
    @Resource
    private TaskPlanner taskPlanner;
    
    @Resource
    private StepExecutor stepExecutor;
    
    @Resource
    private AgentOrchestrator agentOrchestrator;
    
    @Resource
    private SimpleTaskHandler simpleTaskHandler;
    
    @Resource
    private com.esdllm.agentmesh.service.IntentRecognitionService intentRecognitionService;
    
    @Resource
    private com.esdllm.agentmesh.service.ToolMatchingService toolMatchingService;
    
    @Resource
    private com.esdllm.agentmesh.service.ConversationLogService conversationLogService;
    
    @Resource
    private com.esdllm.agentmesh.repository.dao.AgentKbRelationDao agentKbRelationDao;
    
    private final ExecutorService asyncExecutor = Executors.newFixedThreadPool(20);
    
    // 临时存储任务计划(生产环境应使用Redis或数据库)
    private final Map<String, TaskExecutionPlan> taskPlanCache = new ConcurrentHashMap<>();
    
    // 临时存储工作流模板
    private final Map<Long, WorkflowTemplate> workflowTemplateCache = new ConcurrentHashMap<>();
    private Long nextTemplateId = 1000L;
    
    // 经验数据库:记录执行历史用于学习优化
    private final List<ExecutionExperience> experienceDatabase = Collections.synchronizedList(new ArrayList<>());
    
    // 决策规则权重:用于自适应优化
    private final Map<String, Double> decisionRuleWeights = new ConcurrentHashMap<>();
    
    /**
     * 执行经验记录
     */
    @lombok.Data
    @lombok.AllArgsConstructor
    private static class ExecutionExperience {
        private Long workflowId;
        private String taskDescription;
        private Boolean success;
        private Integer userRating;  // 1-5分
        private String userFeedback;
        private Long executionTimeMs;
        private List<String> decisionPath;
        private Long timestamp;
    }
    
    @Override
    public DecisionExecutionResult execute(Long agentId, String query, Long userId, 
                                          Long workflowId, Map<String, Object> context) {
        long startTime = System.currentTimeMillis();
        
        log.info("=== 统一智能体引擎开始执行 ===");
        log.info("agentId: {}, query: {}, userId: {}, workflowId: {}", 
                agentId, query, userId, workflowId);
        
        // 1. 加载智能体配置
        Agent agent = loadAgent(agentId, userId);
        
        // 2. 检索长期记忆,增强上下文
        Map<String, Object> enrichedContext = enrichContextWithMemories(userId, agentId, query, context);
        
        // 3. 决策路径:有工作流ID则执行工作流,否则由AI自主决策
        DecisionExecutionResult result;
        if (workflowId != null) {
            log.info("使用指定工作流执行,workflowId: {}", workflowId);
            result = executeWithWorkflow(workflowId, enrichedContext, userId, agent);
        } else {
            log.info("由AI自主决策执行路径");
            result = executeWithAutoDecision(agentId, query, userId, enrichedContext);
        }
        
        // 4. 学习和优化:从执行结果中提取记忆
        try {
            memoryService.extractAndStoreMemoriesFromConversation(
                userId, agentId, query + " -> " + result.getFinalResponse(), 
                result.getDecisionPath()
            );
        } catch (Exception e) {
            log.error("记忆提取失败", e);
        }
        
        log.info("=== 统一智能体引擎执行完成 === 耗时: {}ms", 
                System.currentTimeMillis() - startTime);
        
        return result;
    }
    
    /**
     * AI自主决策执行(智能路由:简单任务直接回答,复杂任务生成清单逐步执行)
     */
    private DecisionExecutionResult executeWithAutoDecision(Long agentId, String query, 
                                                            Long userId, Map<String, Object> context) {
        
        log.info("=== 开始智能路由决策 ===");
        
        // 1. 意图识别
        IntentRecognitionResult intent = intentRecognitionService.recognizeIntentWithContext(
            query, agentId, userId
        );
        
        log.info("意图识别完成: type={}, confidence={}", 
                intent.getIntentType(), intent.getConfidence());
        
        // 2. 判断是否为复杂任务
        boolean isComplexTask = evaluateTaskComplexity(intent, query);
        
        log.info("任务复杂度评估: isComplex={}", isComplexTask);
        
        if (isComplexTask) {
            // 复杂任务:生成任务清单并逐步执行
            log.info("检测到复杂任务,启用任务规划模式");
            return executeComplexTaskWithPlanning(agentId, query, userId, context, intent);
        } else {
            // 简单任务:直接调用LLM生成回答
            log.info("检测到简单任务,使用快速响应模式");
            return simpleTaskHandler.executeSimpleTask(query, agentId, userId);
        }
    }
    
    /**
     * 评估任务复杂度
     */
    private boolean evaluateTaskComplexity(IntentRecognitionResult intent, String query) {

        // 判断标准1:需要调用多个工具
        if (intent.getNeedToolCall() && intent.getMatchedToolIds() != null 
                && intent.getMatchedToolIds().size() > 1) {
            return true;
        }

        String[] simpleKeywords = {"帮我介绍一下这个平台的功能","智能体","工作流如何使用","HELLO","你是谁"};
        for (String keyword : simpleKeywords) {
            if (query.contains(keyword)) {
                return false;
            }
        }
        // 判断标准2:包含复杂关键词
        String[] complexKeywords = {"分析", "对比", "比较", "总结", "报告", "流程", "步骤", "如何", "为什么", "评估", "优化","怎么办","什么"};
        for (String keyword : complexKeywords) {
            if (query.contains(keyword)) {
                return true;
            }
        }
        // 判断标准3:查询长度较长
        if (query.length() > 50) {
            return true;
        }
        
        // 判断标准4:意图类型为复杂类型
        if ("ANALYSIS".equals(intent.getIntentType()) 
                || "COMPARISON".equals(intent.getIntentType())
                || "MULTI_STEP".equals(intent.getIntentType())) {
            return true;
        }
        
        return false;
    }
    
    /**
     * 执行复杂任务(生成任务清单并逐步执行)
     */
    private DecisionExecutionResult executeComplexTaskWithPlanning(
            Long agentId, String query, Long userId, 
            Map<String, Object> context, IntentRecognitionResult intent) {
        
        log.info("=== 执行复杂任务(任务规划模式) ===");
        
        // 1. 生成任务计划（自动生成sessionId）
        String sessionId = generateSessionId(userId);
        TaskExecutionPlan plan = planTask(agentId, query, userId, context, sessionId);
        
        log.info("任务规划完成,共 {} 个步骤, sessionId: {}", plan.getSteps().size(), sessionId);
        
        // 2. 自动确认所有步骤
        List<String> allStepIds = plan.getSteps().stream()
            .map(TaskExecutionPlan.TaskStep::getStepId)
            .toList();
        
        // 3. 执行任务计划（使用同一个sessionId）
        return executePlannedTask(plan.getTaskId(), allStepIds, userId, sessionId);
    }
    
    @Override
    public void executeAsync(Long agentId, String query, Long userId, 
                            Long workflowId, Map<String, Object> context) {
        CompletableFuture.runAsync(() -> {
            try {
                execute(agentId, query, userId, workflowId, context);
            } catch (Exception e) {
                log.error("异步执行失败", e);
            }
        }, asyncExecutor);
    }
    
    @Override
    public void executeStream(Long agentId, String query, Long userId, 
                             Long workflowId, Map<String, Object> context, Object emitter, String sessionId) {
        
        if (!(emitter instanceof org.springframework.web.servlet.mvc.method.annotation.SseEmitter)) {
            log.error("emitter 类型不正确");
            return;
        }
        
        var sseEmitter = (org.springframework.web.servlet.mvc.method.annotation.SseEmitter) emitter;
        
        // ✅ 如果没有提供sessionId或为空字符串，生成一个新的
        final String finalSessionId = (sessionId != null && !sessionId.isEmpty()) 
            ? sessionId 
            : generateSessionId(userId);
        
        log.info("=== 开始流式执行 === agentId: {}, userId: {}, sessionId: {}", agentId, userId, finalSessionId);
        
        try {
            // ✅ 先判断是否为简单任务
            IntentRecognitionResult intent = intentRecognitionService.recognizeIntentWithContext(
                query, agentId, userId
            );
            
            boolean isSimpleTask = !evaluateTaskComplexity(intent, query);
            
            if (isSimpleTask) {
                log.info("✅ 检测到简单任务，直接调用LLM，不进行任务规划");
                executeSimpleTaskStream(query, agentId, userId, sseEmitter, finalSessionId);
                return;
            }
            
            log.info("📋 检测到复杂任务，启用任务规划模式");
            
            // 1. 先规划任务
            Map<String, Object> planningData = new HashMap<>();
            planningData.put("message", "正在分析任务...");
            planningData.put("progress", 10);
            planningData.put("sessionId", finalSessionId);
            
            sendSseEvent(sseEmitter, "planning", planningData);
            
            TaskExecutionPlan plan = planTask(agentId, query, userId, context, finalSessionId);
            
            // ✅ 构建 steps 列表，避免 Map.of 的 null 值问题
            List<Map<String, Object>> stepsList = plan.getSteps().stream().map(step -> {
                Map<String, Object> stepMap = new HashMap<>();
                stepMap.put("stepId", step.getStepId());
                stepMap.put("description", step.getDescription());
                stepMap.put("stepType", step.getStepType());
                return stepMap;
            }).toList();
            
            Map<String, Object> planReadyData = new HashMap<>();
            planReadyData.put("message", "任务规划完成");
            planReadyData.put("taskId", plan.getTaskId());
            planReadyData.put("totalSteps", plan.getSteps().size());
            planReadyData.put("steps", stepsList);
            planReadyData.put("progress", 20);
            planReadyData.put("sessionId", finalSessionId);
            
            sendSseEvent(sseEmitter, "plan_ready", planReadyData);
            
            // 2. 逐步执行并实时推送（只推送状态，不推送详细内容）
            long startTime = System.currentTimeMillis();
            StringBuilder stepResults = new StringBuilder();
            Map<String, Object> executionContext = new HashMap<>();
            
            // ✅ 添加用户查询到上下文
            executionContext.put("query", query);
            
            // ✅ 查询智能体关联的知识库ID，并添加到上下文
            List<Long> kbIds = getAgentKnowledgeBaseIds(agentId);
            if (kbIds != null && !kbIds.isEmpty()) {
                executionContext.put("kbIds", kbIds);
                log.info("✅ 流式执行：智能体关联了 {} 个知识库: {}", kbIds.size(), kbIds);
            } else {
                log.warn("⚠️ 流式执行：智能体未关联任何知识库");
            }
            
            List<TaskExecutionPlan.TaskStep> steps = plan.getSteps();
            for (int i = 0; i < steps.size(); i++) {
                TaskExecutionPlan.TaskStep step = steps.get(i);
                
                // ✅ 推送步骤开始执行
                Map<String, Object> stepStartData = new HashMap<>();
                stepStartData.put("stepNumber", i + 1);
                stepStartData.put("totalSteps", steps.size());
                stepStartData.put("stepId", step.getStepId());
                stepStartData.put("description", step.getDescription());
                stepStartData.put("status", "running");
                stepStartData.put("progress", 20 + (int)((i + 1.0) / steps.size() * 70));
                stepStartData.put("sessionId", finalSessionId);
                
                sendSseEvent(sseEmitter, "step_start", stepStartData);
                
                // 执行步骤
                Object stepResult = stepExecutor.executeSingleStep(step, executionContext, userId);
                executionContext.put(step.getStepId(), stepResult);
                stepResults.append("[").append(i + 1).append("] ")
                    .append(step.getDescription()).append(": ")
                    .append(stepResult).append("\n");
                
                // ✅ 推送步骤完成状态（成功/失败）
                String status = getStepStatus(stepResult);
                
                // ✅ 使用 HashMap 避免 aiResponse 为 null
                Map<String, Object> stepCompleteData = new HashMap<>();
                stepCompleteData.put("stepNumber", i + 1);
                stepCompleteData.put("totalSteps", steps.size());
                stepCompleteData.put("stepId", step.getStepId());
                stepCompleteData.put("description", step.getDescription());
                stepCompleteData.put("status", status);
                stepCompleteData.put("progress", 20 + (int)((i + 1.0) / steps.size() * 70));
                stepCompleteData.put("sessionId", finalSessionId);
                
                sendSseEvent(sseEmitter, "step_complete", stepCompleteData);
            }
            
            // 3. 生成最终回答
            Map<String, Object> generatingData = new HashMap<>();
            generatingData.put("message", "正在生成最终回答...");
            generatingData.put("progress", 95);
            generatingData.put("sessionId", finalSessionId);
            
            sendSseEvent(sseEmitter, "generating_response", generatingData);
            
            String finalResponse = generateFinalResponseFromSteps(
                query, stepResults.toString(), executionContext
            );
            
            // 4. 发送最终结果（包含完整的AI回复）
            Map<String, Object> completeData = new HashMap<>();
            completeData.put("success", true);
            completeData.put("finalResponse", finalResponse);
            completeData.put("executionTimeMs", System.currentTimeMillis() - startTime);
            completeData.put("totalSteps", steps.size());
            completeData.put("progress", 100);
            completeData.put("sessionId", finalSessionId);
            
            sendSseEvent(sseEmitter, "complete", completeData);
            
            sseEmitter.complete();
            log.info("=== 流式执行完成 === sessionId: {}", finalSessionId);
            
            // ✅ 5. 保存对话日志（使用相同的sessionId）
            try {
                saveConversationLog(agentId, query, userId, finalResponse, stepResults.toString(), startTime, finalSessionId);
            } catch (Exception e) {
                log.error("保存对话日志失败", e);
            }
            
        } catch (Exception e) {
            log.error("流式执行失败", e);
            try {
                // ✅ 使用 HashMap 避免 null 值问题
                Map<String, Object> errorData = new HashMap<>();
                errorData.put("success", false);
                errorData.put("error", e.getMessage());
                errorData.put("sessionId", finalSessionId != null ? finalSessionId : sessionId);
                
                sendSseEvent(sseEmitter, "error", errorData);
                sseEmitter.completeWithError(e);
            } catch (Exception ex) {
                log.error("发送错误事件失败", ex);
            }
        }
    }
    
    /**
     * 获取步骤状态（简化版）
     */
    private String getStepStatus(Object result) {
        if (result == null) {
            return "unknown";
        }
        
        if (result instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> resultMap = (Map<String, Object>) result;
            Boolean success = (Boolean) resultMap.get("success");
            
            if (Boolean.FALSE.equals(success)) {
                return "failed";
            } else {
                return "success";
            }
        }
        
        return "unknown";
    }
    
    /**
     * 从步骤结果中提取AI推理结论
     */
    private String extractAiConclusionFromStep(Object result) {
        if (result == null || !(result instanceof Map)) {
            return null;
        }
        
        @SuppressWarnings("unchecked")
        Map<String, Object> resultMap = (Map<String, Object>) result;
        
        // 检查是否成功
        Boolean success = (Boolean) resultMap.get("success");
        if (Boolean.FALSE.equals(success)) {
            return null;  // 失败不返回结论
        }
        
        // 提取不同类型的AI结论
        if (resultMap.containsKey("reasoningResult")) {
            // LLM推理结果 - 取前200字符
            String reasoning = (String) resultMap.get("reasoningResult");
            if (reasoning != null && reasoning.length() > 200) {
                reasoning = reasoning.substring(0, 200) + "...";
            }
            return reasoning;
        } else if (resultMap.containsKey("summary")) {
            // 汇总结果
            return (String) resultMap.get("summary");
        } else if (resultMap.containsKey("retrievedDocs")) {
            // 知识库检索
            Integer docCount = (Integer) resultMap.get("retrievedDocs");
            return String.format("检索到 %d 个相关文档", docCount);
        } else if (resultMap.containsKey("result")) {
            // 通用结果
            Object res = resultMap.get("result");
            String resultStr = res != null ? res.toString() : null;
            if (resultStr != null && resultStr.length() > 100) {
                resultStr = resultStr.substring(0, 100) + "...";
            }
            return resultStr;
        }
        
        return null;
    }
    
    /**
     * 从步骤结果中提取AI回复
     */
    private String extractAiResponseFromStep(Object result) {
        if (result == null || !(result instanceof Map)) {
            return null;
        }
        
        @SuppressWarnings("unchecked")
        Map<String, Object> resultMap = (Map<String, Object>) result;
        
        // 检查是否成功
        Boolean success = (Boolean) resultMap.get("success");
        if (Boolean.FALSE.equals(success)) {
            return null;  // 失败不返回AI回复
        }
        
        // 提取不同类型的AI回复
        if (resultMap.containsKey("reasoningResult")) {
            // LLM推理结果
            return (String) resultMap.get("reasoningResult");
        } else if (resultMap.containsKey("summary")) {
            // 汇总结果
            return (String) resultMap.get("summary");
        } else if (resultMap.containsKey("retrievedDocs")) {
            // 知识库检索 - 返回文档摘要
            Integer docCount = (Integer) resultMap.get("retrievedDocs");
            return String.format("检索到 %d 个相关文档", docCount);
        } else if (resultMap.containsKey("result")) {
            // 通用结果
            Object res = resultMap.get("result");
            return res != null ? res.toString() : null;
        }
        
        return null;
    }
    
    /**
     * 生成会话ID
     */
    private String generateSessionId(Long userId) {
        return "session_" + userId + "_" + System.currentTimeMillis();
    }
    
    /**
     * 获取智能体关联的知识库ID列表
     */
    private List<Long> getAgentKnowledgeBaseIds(Long agentId) {
        try {
            if (agentId == null) {
                log.warn("智能体ID为空，无法查询知识库");
                return Collections.emptyList();
            }
            
            var relations = agentKbRelationDao.getByAgentId(agentId);
            if (relations == null || relations.isEmpty()) {
                log.info("智能体 {} 未关联任何知识库", agentId);
                return Collections.emptyList();
            }
            
            List<Long> kbIds = relations.stream()
                .map(relation -> relation.getKbId())
                .filter(kbId -> kbId != null)
                .toList();
            
            log.info("智能体 {} 关联了 {} 个知识库: {}", agentId, kbIds.size(), kbIds);
            return kbIds;
            
        } catch (Exception e) {
            log.error("查询智能体知识库失败: agentId={}", agentId, e);
            return Collections.emptyList();
        }
    }
    
    /**
     * 执行简单任务流式响应（不进行任务规划）
     */
    private void executeSimpleTaskStream(String query, Long agentId, Long userId, 
                                        org.springframework.web.servlet.mvc.method.annotation.SseEmitter emitter,
                                        String sessionId) {
        long startTime = System.currentTimeMillis();
        
        try {
            // 1. 发送开始事件
            Map<String, Object> startData = new HashMap<>();
            startData.put("message", "正在思考...");
            startData.put("progress", 10);
            startData.put("sessionId", sessionId);
            sendSseEvent(emitter, "thinking", startData);
            
            // 2. 直接调用LLM生成回答
            String finalResponse = simpleTaskHandler.generateDirectResponse(query, agentId, userId);
            
            // 3. 发送完成事件
            Map<String, Object> completeData = new HashMap<>();
            completeData.put("success", true);
            completeData.put("finalResponse", finalResponse);
            completeData.put("executionTimeMs", System.currentTimeMillis() - startTime);
            completeData.put("totalSteps", 0);  // 没有步骤
            completeData.put("progress", 100);
            completeData.put("sessionId", sessionId);
            
            sendSseEvent(emitter, "complete", completeData);
            
            emitter.complete();
            log.info("✅ 简单任务流式执行完成, sessionId: {}", sessionId);
            
            // 4. 保存对话日志
            try {
                saveConversationLog(agentId, query, userId, finalResponse, "", startTime, sessionId);
            } catch (Exception e) {
                log.error("保存对话日志失败", e);
            }
            
        } catch (Exception e) {
            log.error("简单任务流式执行失败", e);
            try {
                Map<String, Object> errorData = new HashMap<>();
                errorData.put("success", false);
                errorData.put("error", e.getMessage());
                errorData.put("sessionId", sessionId);
                
                sendSseEvent(emitter, "error", errorData);
                emitter.completeWithError(e);
            } catch (Exception ex) {
                log.error("发送错误事件失败", ex);
            }
        }
    }
    
    private void sendSseEvent(
            org.springframework.web.servlet.mvc.method.annotation.SseEmitter emitter,
            String eventName,
            Object data) {
        try {
            emitter.send(org.springframework.web.servlet.mvc.method.annotation.SseEmitter.event()
                .name(eventName)
                .data(data));
        } catch (Exception e) {
            log.error("发送SSE事件失败: {}", eventName, e);
        }
    }
    
    @Override
    public Flux<String> executeFlux(Long agentId, String query, Long userId, 
                                   Long workflowId, Map<String, Object> context) {
        return Flux.just("Flux流式执行暂未实现");
    }
    
    @Override
    public Long generateWorkflow(Long agentId, String taskDescription, Long userId) {
        throw new BusinessException(ErrorCode.SYSTEM_ERROR, "动态工作流生成功能开发中");
    }
    
    @Override
    public void learnAndOptimize(Long workflowId, DecisionExecutionResult executionResult, 
                                String userFeedback) {
        log.info("=== 开始学习和优化 ===");
        log.info("workflowId: {}, success: {}, feedback: {}", 
                workflowId, executionResult.getSuccess(), userFeedback);
        
        try {
            // 1. 记录执行经验到数据库
            ExecutionExperience experience = new ExecutionExperience(
                workflowId,
                "workflow_execution",
                executionResult.getSuccess(),
                extractRatingFromFeedback(userFeedback),
                userFeedback,
                executionResult.getExecutionTimeMs(),
                extractDecisionPath(executionResult),
                System.currentTimeMillis()
            );
            
            experienceDatabase.add(experience);
            log.info("已记录执行经验,当前经验总数: {}", experienceDatabase.size());
            
            // 2. 分析成功/失败模式
            analyzeExecutionPatterns();
            
            // 3. 更新决策规则权重
            updateDecisionRuleWeights(experience);
            
            // 4. 如果有用户反馈,提取优化建议
            if (userFeedback != null && !userFeedback.trim().isEmpty()) {
                optimizeBasedOnFeedback(workflowId, userFeedback, executionResult);
            }
            
            log.info("=== 学习和优化完成 ===");
            
        } catch (Exception e) {
            log.error("学习和优化失败", e);
        }
    }
    
    /**
     * 从反馈中提取评分
     */
    private Integer extractRatingFromFeedback(String feedback) {
        if (feedback == null || feedback.isEmpty()) {
            return null;
        }
        
        // 简单实现:检测关键词
        if (feedback.contains("很好") || feedback.contains("满意") || feedback.contains("5")) {
            return 5;
        } else if (feedback.contains("好") || feedback.contains("不错") || feedback.contains("4")) {
            return 4;
        } else if (feedback.contains("一般") || feedback.contains("3")) {
            return 3;
        } else if (feedback.contains("差") || feedback.contains("不好") || feedback.contains("2")) {
            return 2;
        } else if (feedback.contains("很差") || feedback.contains("1")) {
            return 1;
        }
        
        return null;
    }
    
    /**
     * 提取决策路径
     */
    private List<String> extractDecisionPath(DecisionExecutionResult result) {
        if (result.getDecisionPath() == null) {
            return Collections.emptyList();
        }
        
        return result.getDecisionPath().stream()
            .map(step -> step.getStepType() + ":" + step.getDescription())
            .toList();
    }
    
    /**
     * 分析执行模式
     */
    private void analyzeExecutionPatterns() {
        if (experienceDatabase.size() < 5) {
            log.debug("经验数据不足,暂不分析模式");
            return;
        }
        
        // 统计成功率
        long totalExecutions = experienceDatabase.size();
        long successfulExecutions = experienceDatabase.stream()
            .filter(exp -> Boolean.TRUE.equals(exp.getSuccess()))
            .count();
        
        double successRate = (double) successfulExecutions / totalExecutions;
        log.info("执行统计 - 总次数: {}, 成功: {}, 成功率: {:.2f}%", 
                totalExecutions, successfulExecutions, successRate * 100);
        
        // 分析低评分案例
        List<ExecutionExperience> lowRatingCases = experienceDatabase.stream()
            .filter(exp -> exp.getUserRating() != null && exp.getUserRating() <= 2)
            .toList();
        
        if (!lowRatingCases.isEmpty()) {
            log.warn("发现 {} 个低评分案例,需要优化", lowRatingCases.size());
            
            // 提取共同特征
            Map<String, Long> commonPatterns = lowRatingCases.stream()
                .flatMap(exp -> exp.getDecisionPath().stream())
                .collect(java.util.stream.Collectors.groupingBy(
                    path -> path, 
                    java.util.stream.Collectors.counting()
                ));
            
            commonPatterns.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(3)
                .forEach(entry -> 
                    log.warn("常见问题模式: {} (出现 {} 次)", entry.getKey(), entry.getValue())
                );
        }
    }
    
    /**
     * 更新决策规则权重
     */
    private void updateDecisionRuleWeights(ExecutionExperience experience) {
        // 基于执行结果调整规则权重
        if (Boolean.TRUE.equals(experience.getSuccess())) {
            // 成功的执行路径,增加权重
            for (String decision : experience.getDecisionPath()) {
                decisionRuleWeights.merge(decision, 0.1, Double::sum);
            }
        } else {
            // 失败的执行路径,降低权重
            for (String decision : experience.getDecisionPath()) {
                decisionRuleWeights.merge(decision, -0.1, Double::sum);
            }
        }
        
        log.debug("当前决策规则权重: {}", decisionRuleWeights);
    }
    
    /**
     * 基于反馈优化
     */
    private void optimizeBasedOnFeedback(Long workflowId, String feedback, 
                                        DecisionExecutionResult result) {
        log.info("基于用户反馈优化: {}", feedback);
        
        try {
            // 使用LLM分析反馈，生成优化建议
            OptimizationSuggestion suggestion = analyzeFeedbackWithLLM(feedback, result);
            
            if (suggestion != null) {
                log.info("LLM优化建议: type={}, suggestion={}", 
                    suggestion.getProblemType(), suggestion.getSuggestion());
                
                // 根据建议类型执行相应的优化操作
                applyOptimizationSuggestion(workflowId, suggestion);
            }
            
        } catch (Exception e) {
            log.error("LLM反馈分析失败，降级为关键词匹配", e);
            
            // 降级方案：基于关键词识别问题类型
            fallbackKeywordAnalysis(feedback);
        }
    }
    
    /**
     * 使用LLM分析用户反馈
     */
    private OptimizationSuggestion analyzeFeedbackWithLLM(String feedback, DecisionExecutionResult result) {
        try {
            // 获取默认模型
            var model = aiModelDao.getFirstChatModel();
            if (model == null) {
                log.warn("没有可用的ChatModel，无法进行LLM反馈分析");
                return null;
            }
            
            // 获取模型提供商
            var provider = modelProviderDao.getById(model.getProviderId());
            if (provider == null) {
                log.warn("模型提供商不存在: providerId={}", model.getProviderId());
                return null;
            }
            
            // 创建ChatClient
            ChatClient chatClient = aiModelSupport.createChatClient(model, provider);
            
            // 构建提示词
            String systemPrompt = """
                你是一个智能体优化专家。请分析用户对智能体执行的反馈，识别问题类型并给出优化建议。
                
                问题类型分类：
                1. performance: 性能问题（速度慢、耗时长、响应延迟等）
                2. accuracy: 准确性问题（结果错误、不准确、理解偏差等）
                3. usability: 用户体验问题（输出复杂、难懂、格式混乱等）
                4. tool_selection: 工具选择问题（调用了不合适的工具、缺少必要工具等）
                5. decision_logic: 决策逻辑问题（推理过程不合理、步骤冗余等）
                
                请以JSON格式返回分析结果：
                {
                  "problemType": "问题类型",
                  "confidence": 0.0-1.0,
                  "suggestion": "具体的优化建议（100字以内）",
                  "affectedSteps": ["受影响的步骤ID列表"]
                }
                """;
            
            String executionSummary = buildExecutionSummary(result);
            String userPrompt = String.format("""
                用户反馈：%s
                
                执行摘要：
                %s
                
                请分析上述反馈，给出优化建议。
                """, feedback, executionSummary);
            
            // 调用AI
            String response = chatClient.prompt()
                .system(systemPrompt)
                .user(userPrompt)
                .call()
                .content();
            
            if (StrUtil.isBlank(response)) {
                return null;
            }
            
            // 解析JSON响应
            return parseOptimizationSuggestion(response);
            
        } catch (Exception e) {
            log.error("LLM反馈分析异常", e);
            return null;
        }
    }
    
    /**
     * 构建执行摘要
     */
    private String buildExecutionSummary(DecisionExecutionResult result) {
        if (result == null || result.getDecisionPath() == null) {
            return "无执行记录";
        }
        
        StringBuilder summary = new StringBuilder();
        summary.append("总耗时: ").append(result.getExecutionTimeMs()).append("ms\n");
        summary.append("成功: ").append(result.getSuccess()).append("\n");
        summary.append("执行步骤:\n");
        
        for (int i = 0; i < result.getDecisionPath().size(); i++) {
            var step = result.getDecisionPath().get(i);
            summary.append(i + 1).append(". ")
                .append(step.getDescription())
                .append(" (").append(step.getStepType()).append(")")
                .append(" - ").append(step.getStatus())
                .append("\n");
        }
        
        return summary.toString();
    }
    
    /**
     * 解析优化建议JSON
     */
    private OptimizationSuggestion parseOptimizationSuggestion(String json) {
        try {
            // TODO: 使用Jackson解析JSON
            // 当前简化实现：返回null，依赖降级方案
            log.debug("LLM返回的优化建议JSON: {}", json.substring(0, Math.min(200, json.length())));
            return null;
            
        } catch (Exception e) {
            log.warn("优化建议JSON解析失败", e);
            return null;
        }
    }
    
    /**
     * 应用优化建议
     */
    private void applyOptimizationSuggestion(Long workflowId, OptimizationSuggestion suggestion) {
        switch (suggestion.getProblemType()) {
            case "performance":
                log.warn("检测到性能问题，建议: {}", suggestion.getSuggestion());
                // 可以缓存常用工具调用结果、优化并行执行策略等
                break;
                
            case "accuracy":
                log.warn("检测到准确性问题，建议: {}", suggestion.getSuggestion());
                // 可以调整模型参数、增加验证步骤等
                break;
                
            case "usability":
                log.warn("检测到用户体验问题，建议: {}", suggestion.getSuggestion());
                // 可以优化输出格式、简化语言等
                break;
                
            case "tool_selection":
                log.warn("检测到工具选择问题，建议: {}", suggestion.getSuggestion());
                // 可以调整工具推荐算法、更新工具元数据等
                break;
                
            case "decision_logic":
                log.warn("检测到决策逻辑问题，建议: {}", suggestion.getSuggestion());
                // 可以优化任务规划prompt、调整步骤顺序等
                break;
                
            default:
                log.warn("未知问题类型: {}", suggestion.getProblemType());
        }
    }
    
    /**
     * 降级方案：基于关键词的反馈分析
     */
    private void fallbackKeywordAnalysis(String feedback) {
        if (feedback.contains("慢") || feedback.contains("耗时") || feedback.contains("延迟")) {
            log.warn("检测到性能问题反馈，建议优化执行效率");
        }
        
        if (feedback.contains("错误") || feedback.contains("不准确") || feedback.contains("不对")) {
            log.warn("检测到准确性问题反馈，建议优化决策逻辑");
        }
        
        if (feedback.contains("复杂") || feedback.contains("难懂") || feedback.contains("看不懂")) {
            log.warn("检测到用户体验问题反馈，建议简化输出");
        }
        
        if (feedback.contains("工具") || feedback.contains("调用")) {
            log.warn("检测到工具相关问题反馈，建议优化工具选择");
        }
    }
    
    /**
     * 优化建议内部类
     */
    private static class OptimizationSuggestion {
        private String problemType;
        private Double confidence;
        private String suggestion;
        private List<String> affectedSteps;
        
        public String getProblemType() { return problemType; }
        public void setProblemType(String problemType) { this.problemType = problemType; }
        
        public Double getConfidence() { return confidence; }
        public void setConfidence(Double confidence) { this.confidence = confidence; }
        
        public String getSuggestion() { return suggestion; }
        public void setSuggestion(String suggestion) { this.suggestion = suggestion; }
        
        public List<String> getAffectedSteps() { return affectedSteps; }
        public void setAffectedSteps(List<String> affectedSteps) { this.affectedSteps = affectedSteps; }
    }
    
    @Override
    public TaskExecutionPlan planTask(Long agentId, String query, Long userId, Map<String, Object> context, String sessionId) {
        log.info("=== 开始AI驱动的任务规划 ===");
        
        long startTime = System.currentTimeMillis();
        String taskId = "task_" + agentId + "_" + System.currentTimeMillis();
        
        try {
            Agent agent = loadAgent(agentId, userId);
            List<com.esdllm.agentmesh.model.domain.Tools> availableTools = getAvailableTools(agentId, userId);
            
            log.info("智能体可用工具数: {}", availableTools.size());
            
            // 使用TaskPlanner生成任务计划
            List<TaskExecutionPlan.TaskStep> steps = taskPlanner.generateAiDrivenTaskPlan(
                query, agent, availableTools, context
            );
            
            long estimatedDuration = steps.stream()
                .mapToLong(step -> step.getEstimatedDurationMs() != null ? step.getEstimatedDurationMs() : 1000)
                .sum();
            
            // ✅ 构建扩展上下文，包含sessionId
            Map<String, Object> planContext = new HashMap<>();
            if (context != null) {
                planContext.putAll(context);
            }
            if (sessionId != null && !sessionId.isEmpty()) {
                planContext.put("sessionId", sessionId);
                log.info("任务规划关联会话ID: {}", sessionId);
            }
            
            TaskExecutionPlan plan = TaskExecutionPlan.builder()
                .taskId(taskId)
                .taskDescription(query)
                .steps(steps)
                .estimatedDurationMs(estimatedDuration)
                .requiresConfirmation(!steps.isEmpty())
                .agentId(agentId)
                .userId(userId)
                .createdAt(System.currentTimeMillis())
                .context(planContext)  // ✅ 设置context
                .build();
            
            taskPlanCache.put(taskId, plan);
            scheduleTaskPlanCleanup(taskId, 5 * 60 * 1000);
            
            log.info("✅ 任务计划已缓存: taskId={}, context.sessionId={}", taskId, planContext.get("sessionId"));
            
            log.info("=== AI任务规划完成 === taskId: {}, 步骤数: {}, 预估耗时: {}ms", 
                    taskId, steps.size(), estimatedDuration);
            
            return plan;
            
        } catch (Exception e) {
            log.error("任务规划失败", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "任务规划失败: " + e.getMessage());
        }
    }
    
    @Override
    public boolean isSimpleTask(Long agentId, String query, Long userId) {
        try {
            // 1. 意图识别
            IntentRecognitionResult intent = intentRecognitionService.recognizeIntentWithContext(
                query, agentId, userId
            );
            
            // 2. 评估任务复杂度
            boolean isComplex = evaluateTaskComplexity(intent, query);
            
            return !isComplex;  // 返回是否为简单任务
            
        } catch (Exception e) {
            log.warn("意图识别失败，默认为复杂任务", e);
            return false;  // 出错时保守处理，视为复杂任务
        }
    }
    
    @Override
    public DecisionExecutionResult executeSimpleTask(Long agentId, String query, Long userId, String sessionId) {
        log.info("=== 执行简单任务 === agentId: {}, sessionId: {}", agentId, sessionId);
        
        try {
            // 直接调用 SimpleTaskHandler
            DecisionExecutionResult result = simpleTaskHandler.executeSimpleTask(query, agentId, userId);
            
            // 保存对话日志
            if (Boolean.TRUE.equals(result.getSuccess()) && sessionId != null && !sessionId.isEmpty()) {
                try {
                    saveConversationLog(agentId, query, userId, 
                        result.getFinalResponse(), null, 
                        System.currentTimeMillis() - (result.getExecutionTimeMs() != null ? result.getExecutionTimeMs() : 0),
                        sessionId);
                    log.info("简单任务对话日志已保存到会话: {}", sessionId);
                } catch (Exception e) {
                    log.error("保存对话日志失败", e);
                }
            }
            
            return result;
            
        } catch (Exception e) {
            log.error("简单任务执行失败", e);
            
            DecisionExecutionResult result = new DecisionExecutionResult();
            result.setSuccess(false);
            result.setErrorMessage("执行失败: " + e.getMessage());
            result.setExecutionTimeMs(0L);
            
            return result;
        }
    }
    
    @Override
    public DecisionExecutionResult executePlannedTask(String taskId, List<String> confirmedSteps, Long userId, String sessionId) {
        log.info("=== 执行已确认的任务计划 ===");
        
        TaskExecutionPlan plan = taskPlanCache.get(taskId);
        if (plan == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "任务计划不存在或已过期");
        }
        
        List<TaskExecutionPlan.TaskStep> stepsToExecute = plan.getSteps();
        if (confirmedSteps != null && !confirmedSteps.isEmpty()) {
            stepsToExecute = plan.getSteps().stream()
                .filter(step -> confirmedSteps.contains(step.getStepId()))
                .toList();
        }
        
        log.info("将执行 {}/{} 个步骤", stepsToExecute.size(), plan.getSteps().size());
        
        return executeStepsSequentially(plan, stepsToExecute, userId, sessionId);
    }
    
    @Override
    public TaskExecutionPlan getTaskPlanFromCache(String taskId) {
        return taskPlanCache.get(taskId);
    }
    
    /**
     * 逐步执行任务步骤
     */
    private DecisionExecutionResult executeStepsSequentially(
            TaskExecutionPlan plan, 
            List<TaskExecutionPlan.TaskStep> stepsToExecute, 
            Long userId,
            String sessionId) {
        
        long startTime = System.currentTimeMillis();
        StringBuilder stepResults = new StringBuilder();
        Map<String, Object> executionContext = new HashMap<>();
        
        try {
            // ✅ 添加用户查询到上下文
            executionContext.put("query", plan.getTaskDescription());
            
            // ✅ 查询智能体关联的知识库ID，并添加到上下文
            List<Long> kbIds = getAgentKnowledgeBaseIds(plan.getAgentId());
            if (kbIds != null && !kbIds.isEmpty()) {
                executionContext.put("kbIds", kbIds);
                log.info("✅ 智能体关联了 {} 个知识库: {}", kbIds.size(), kbIds);
            } else {
                log.warn("⚠️ 智能体未关联任何知识库，知识库检索步骤将失败");
            }
            
            for (int i = 0; i < stepsToExecute.size(); i++) {
                TaskExecutionPlan.TaskStep step = stepsToExecute.get(i);
                
                log.info("执行步骤 {}/{}: {}", i + 1, stepsToExecute.size(), step.getDescription());
                
                Object stepResult = stepExecutor.executeSingleStep(step, executionContext, userId);
                
                stepResults.append("[步骤 ").append(i + 1).append("] ")
                    .append(step.getDescription()).append(": ")
                    .append(stepResult).append("\n");
                
                executionContext.put(step.getStepId(), stepResult);
                
                log.info("步骤 {}/{} 完成", i + 1, stepsToExecute.size());
            }
            
            String finalResponse = generateFinalResponseFromSteps(
                plan.getTaskDescription(), stepResults.toString(), executionContext
            );
            
            DecisionExecutionResult result = new DecisionExecutionResult();
            result.setSuccess(true);
            result.setFinalResponse(finalResponse);
            result.setExecutionTimeMs(System.currentTimeMillis() - startTime);
            result.setDecisionPath(buildDecisionPathFromSteps(stepsToExecute, executionContext));
            
            log.info("=== 任务计划执行完成 === 总耗时: {}ms", result.getExecutionTimeMs());
            
            // ✅ 保存对话日志到指定的sessionId
            if (sessionId != null && !sessionId.isEmpty()) {
                try {
                    saveConversationLog(plan.getAgentId(), plan.getTaskDescription(), userId, 
                        finalResponse, stepResults.toString(), startTime, sessionId);
                    log.info("对话日志已保存到会话: {}", sessionId);
                } catch (Exception e) {
                    log.error("保存对话日志失败", e);
                }
            }
            
            return result;
            
        } catch (Exception e) {
            log.error("任务计划执行失败", e);
            
            DecisionExecutionResult result = new DecisionExecutionResult();
            result.setSuccess(false);
            result.setErrorMessage("执行失败: " + e.getMessage());
            result.setExecutionTimeMs(System.currentTimeMillis() - startTime);
            
            return result;
        } finally {
            taskPlanCache.remove(plan.getTaskId());
        }
    }
    
    private String generateFinalResponseFromSteps(
            String taskDescription, 
            String stepResults, 
            Map<String, Object> context) {
        
        // ✅ 检测是否为简单任务（问候、闲聊等）
        if (isSimpleTask(taskDescription, context)) {
            log.info("检测到简单任务，直接返回AI回答，不显示步骤结构");
            return extractSimpleTaskResponse(context);
        }
        
        // ✅ 生成结构化的最终回答 - 每个步骤用AI生成口语化内容
        StringBuilder response = new StringBuilder();
        
        // ❌ 不再显示报告标题，直接从步骤内容开始
        
        // 按步骤顺序输出，只添加有有效内容的步骤
        int stepNumber = 1;
        for (Map.Entry<String, Object> entry : context.entrySet()) {
            if (!"query".equals(entry.getKey()) && !"kbIds".equals(entry.getKey())) {
                String stepId = entry.getKey();
                Object result = entry.getValue();
                
                // 获取步骤描述
                String stepDescription = getStepDescription(stepId, result);
                
                // 用AI生成口语化的步骤内容
                String friendlyContent = generateFriendlyStepContent(stepDescription, result);
                
                // ✅ 只有当有有效内容时才添加到报告中
                if (friendlyContent != null && !friendlyContent.isEmpty()) {
                    // 章节标题
                    response.append(String.format("## %d. %s\n\n", stepNumber++, stepDescription));
                    response.append(friendlyContent);
                    response.append("\n\n");
                    response.append("---\n\n");
                }
                // ❌ 没有有效信息的步骤直接跳过，不显示
            }
        }
        
        // 总结
        response.append("# 总结\n\n");
        response.append(generateSummaryFromSteps(context));
        
        return response.toString();
    }
    
    /**
     * 检测是否为简单任务（问候、闲聊等）
     */
    private boolean isSimpleTask(String taskDescription, Map<String, Object> context) {
        if (taskDescription == null || taskDescription.isEmpty()) {
            return false;
        }
        
        // 简单任务关键词
        String[] simpleKeywords = {
            "你好", "您好", "嗨", "hello", "hi", "hey",
            "再见", "拜拜", "bye", "goodbye",
            "谢谢", "感谢", "thanks", "thank you",
            "不客气", "没关系",
            "早上好", "晚上好", "下午好",
            "在吗", "在不在"
        };
        
        String lowerQuery = taskDescription.toLowerCase().trim();
        
        // 检查是否匹配简单关键词
        for (String keyword : simpleKeywords) {
            if (lowerQuery.contains(keyword.toLowerCase())) {
                // 进一步确认：查询长度应该较短（< 20字符）
                if (taskDescription.length() < 20) {
                    return true;
                }
            }
        }
        
        // 检查步骤数量：如果只有1-2个步骤，且都是LLM推理类型，可能是简单任务
        if (context != null && context.size() <= 2) {
            boolean allSimpleSteps = context.values().stream()
                .allMatch(result -> {
                    if (result instanceof Map) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> resultMap = (Map<String, Object>) result;
                        String stepType = (String) resultMap.get("stepType");
                        return "LLM_REASONING".equals(stepType) || "RESULT_SUMMARY".equals(stepType);
                    }
                    return false;
                });
            
            if (allSimpleSteps && taskDescription.length() < 30) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * 提取简单任务的回答（直接返回AI的回复，不包含步骤结构）
     */
    private String extractSimpleTaskResponse(Map<String, Object> context) {
        if (context == null || context.isEmpty()) {
            return "";
        }
        
        log.info("开始提取简单任务回答, context keys: {}", context.keySet());
        
        // 查找 LLM_REASONING 或 RESULT_SUMMARY 类型的步骤
        for (Map.Entry<String, Object> entry : context.entrySet()) {
            String key = entry.getKey();
            if ("query".equals(key) || "kbIds".equals(key)) {
                continue;
            }
            
            Object result = entry.getValue();
            log.info("处理步骤: {}, type: {}", key, result != null ? result.getClass().getSimpleName() : "null");
            
            if (result instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> resultMap = (Map<String, Object>) result;
                
                log.info("步骤详情: success={}, stepType={}", 
                    resultMap.get("success"), resultMap.get("stepType"));
                
                // 提取 AI 回复
                String aiResponse = null;
                if (resultMap.containsKey("reasoningResult")) {
                    aiResponse = (String) resultMap.get("reasoningResult");
                    log.info("找到 reasoningResult, 长度: {}", aiResponse != null ? aiResponse.length() : 0);
                } else if (resultMap.containsKey("summary")) {
                    aiResponse = (String) resultMap.get("summary");
                    log.info("找到 summary, 长度: {}", aiResponse != null ? aiResponse.length() : 0);
                }
                
                if (aiResponse != null && !aiResponse.isEmpty()) {
                    // 过滤技术日志
                    if (!aiResponse.contains("{success=") && !aiResponse.contains("演示模式")) {
                        log.info("✅ 提取到干净的AI回复");
                        return aiResponse;
                    } else {
                        log.warn("⚠️ 回复包含技术日志，尝试清理");
                        // 尝试从技术日志中提取有用内容
                        String cleaned = cleanTechnicalLog(aiResponse);
                        if (cleaned != null && !cleaned.isEmpty()) {
                            return cleaned;
                        }
                    }
                }
            }
        }
        
        log.warn("⚠️ 未能提取到有效回复");
        return "";
    }
    
    /**
     * 清理技术日志，提取有用的内容
     */
    private String cleanTechnicalLog(String technicalLog) {
        if (technicalLog == null || technicalLog.isEmpty()) {
            return null;
        }
        
        log.info("开始清理技术日志，原始内容: {}", technicalLog.substring(0, Math.min(200, technicalLog.length())));
        
        // 尝试1: 提取 "推理结论：" 后面的内容
        if (technicalLog.contains("推理结论：")) {
            int idx = technicalLog.indexOf("推理结论：");
            String conclusion = technicalLog.substring(idx + 5).trim();
            // 移除可能的结尾符号
            conclusion = conclusion.replaceAll("[{}\\]\\[]", "").trim();
            if (!conclusion.isEmpty()) {
                log.info("✅ 从'推理结论'提取: {}", conclusion);
                return conclusion;
            }
        }
        
        // 尝试2: 提取 "基于以下信息" 后面的 "推理结论："
        if (technicalLog.contains("基于以下信息进行推理分析：")) {
            int idx = technicalLog.indexOf("基于以下信息进行推理分析：");
            String afterIndex = technicalLog.substring(idx);
            if (afterIndex.contains("推理结论：")) {
                int conclusionIdx = afterIndex.indexOf("推理结论：");
                String conclusion = afterIndex.substring(conclusionIdx + 5).trim();
                conclusion = conclusion.replaceAll("[{}\\]\\[]", "").trim();
                if (!conclusion.isEmpty()) {
                    log.info("✅ 从'基于以下信息'提取: {}", conclusion);
                    return conclusion;
                }
            }
        }
        
        // 尝试3: 如果内容是 Map.toString() 格式，提取 stepDescription 或 description
        if (technicalLog.contains("stepDescription=")) {
            int startIdx = technicalLog.indexOf("stepDescription=");
            int endIdx = technicalLog.indexOf("}", startIdx);
            if (endIdx > startIdx) {
                String desc = technicalLog.substring(startIdx + 16, endIdx).trim();
                // 移除可能的逗号和其他字段
                if (desc.contains(",")) {
                    desc = desc.substring(0, desc.indexOf(",")).trim();
                }
                if (!desc.isEmpty()) {
                    log.info("✅ 从stepDescription提取: {}", desc);
                    return desc;
                }
            }
        }
        
        // 尝试4: 直接返回原始内容（如果不太长）
        if (technicalLog.length() < 100 && !technicalLog.contains("{success=")) {
            log.info("⚠️ 返回原始内容（较短）");
            return technicalLog.trim();
        }
        
        log.warn("❌ 无法清理技术日志");
        return null;
    }
    
    /**
     * 获取步骤描述
     */
    private String getStepDescription(String stepId, Object result) {
        if (result instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> resultMap = (Map<String, Object>) result;
            String desc = (String) resultMap.get("stepDescription");
            if (desc != null && !desc.isEmpty()) {
                return desc;
            }
        }
        return stepId.replace("step_", "步骤 ");
    }
    
    /**
     * 用AI生成口语化的步骤内容
     */
    private String generateFriendlyStepContent(String stepDescription, Object result) {
        if (result == null || !(result instanceof Map)) {
            return null;
        }
        
        @SuppressWarnings("unchecked")
        Map<String, Object> resultMap = (Map<String, Object>) result;
        
        Boolean success = (Boolean) resultMap.get("success");
        
        // ❌ 失败的情况 - 直接返回null，不显示任何提示
        if (Boolean.FALSE.equals(success)) {
            return null;
        }
        
        // 成功的情况 - 提取原始内容
        String rawContent = null;
        if (resultMap.containsKey("reasoningResult")) {
            rawContent = (String) resultMap.get("reasoningResult");
        } else if (resultMap.containsKey("summary")) {
            rawContent = (String) resultMap.get("summary");
        } else if (resultMap.containsKey("result")) {
            Object res = resultMap.get("result");
            if (res != null) {
                rawContent = res.toString();
            }
        }
        
        if (rawContent == null || rawContent.isEmpty()) {
            return null;
        }
        
        // 过滤掉技术日志
        if (rawContent.contains("{success=") || rawContent.contains("演示模式") || rawContent.contains("无条件表达式")) {
            return null; // 技术日志不提供有价值信息
        }
        
        // 如果内容已经很友好，直接返回
        if (!rawContent.contains("基于以下信息") && !rawContent.contains("推理结论：")) {
            return rawContent;
        }
        
        // 否则调用AI生成口语化版本
        try {
            return generateFriendlyVersionWithAI(stepDescription, rawContent);
        } catch (Exception e) {
            log.error("生成口语化内容失败", e);
            // 降级：提取关键信息
            if (rawContent.contains("推理结论：")) {
                int idx = rawContent.indexOf("推理结论：");
                return rawContent.substring(idx + 5).trim();
            }
            return rawContent;
        }
    }
    
    /**
     * 调用AI生成口语化版本
     */
    private String generateFriendlyVersionWithAI(String stepDescription, String rawContent) {
        var model = aiModelDao.getFirstChatModel();
        if (model == null) {
            return extractKeyInfo(rawContent);
        }
        
        var provider = modelProviderDao.getById(model.getProviderId());
        if (provider == null) {
            return extractKeyInfo(rawContent);
        }
        
        ChatClient chatClient = aiModelSupport.createChatClient(model, provider);
        
        String systemPrompt = """
            你是一个亲切、自然的助手。请将以下技术分析内容改写成口语化、易于理解的表达。
                    
            重要要求：
            - 像朋友聊天一样自然，不要机械化
            - 不要使用"根据分析"、"系统显示"、"推理结论"等术语
            - 不要提及"步骤"、"执行"等技术词汇
            - 直接给出有用的信息和建议
            - 简洁明了，突出重点
            - 长度控制在100-200字以内
                    
            示例对比：
            ❌ 坏例子："根据系统分析，用户当前处于单身状态，目标是建立长期关系"
            ✅ 好例子："看起来你现在是单身，想找一段认真的感情对吧？"
                    
            ❌ 坏例子："推理结论显示，追求期需要注意保持适度距离"
            ✅ 好例子："刚开始接触时，别太着急，给对方一些空间会更好"
            """;
        
        String userPrompt = String.format("""
            步骤主题：%s
            
            原始内容：
            %s
            
            请改写成口语化的表达：
            """, stepDescription, rawContent);
        
        String result = chatClient.prompt()
            .system(systemPrompt)
            .user(userPrompt)
            .call()
            .content();
        
        return result != null && !result.trim().isEmpty() ? result : extractKeyInfo(rawContent);
    }
    
    /**
     * 提取关键信息（降级方案）
     */
    private String extractKeyInfo(String rawContent) {
        if (rawContent.contains("推理结论：")) {
            int idx = rawContent.indexOf("推理结论：");
            return rawContent.substring(idx + 5).trim();
        }
        return rawContent;
    }
    
    /**
     * 提取完整的AI结论（不截断）- 用户友好版本
     */
    private String extractFullAiConclusion(String stepId, Object result) {
        if (result == null || !(result instanceof Map)) {
            return null;
        }
        
        @SuppressWarnings("unchecked")
        Map<String, Object> resultMap = (Map<String, Object>) result;
        
        Boolean success = (Boolean) resultMap.get("success");
        if (Boolean.FALSE.equals(success)) {
            // 失败时，只显示友好的错误信息
            String error = (String) resultMap.get("error");
            if (error != null && error.contains("未配置知识库")) {
                return "缺少相关知识库支持";
            }
            return null; // 失败的步骤不提供给AI
        }
        
        // 提取有价值的信息 - 只保留用户能理解的内容
        if (resultMap.containsKey("reasoningResult")) {
            // LLM推理结果 - 提取关键内容，去除技术细节
            String reasoning = (String) resultMap.get("reasoningResult");
            if (reasoning != null) {
                // 如果包含原始JSON对象，尝试提取有用的部分
                if (reasoning.contains("{success=")) {
                    // 这是技术日志，提取推理结论部分
                    int idx = reasoning.indexOf("推理结论：");
                    if (idx != -1) {
                        return reasoning.substring(idx + 5).trim();
                    }
                    return null; // 无法提取有效信息
                }
                return reasoning;
            }
        } else if (resultMap.containsKey("summary")) {
            // 汇总结果
            return (String) resultMap.get("summary");
        } else if (resultMap.containsKey("retrievedDocs")) {
            // 知识库检索
            Integer docCount = (Integer) resultMap.get("retrievedDocs");
            if (docCount != null && docCount > 0) {
                return String.format("找到%d个相关资料", docCount);
            }
            return null;
        } else if (resultMap.containsKey("result")) {
            // 通用结果 - 过滤掉技术日志
            Object res = resultMap.get("result");
            if (res != null) {
                String resultStr = res.toString();
                // 如果是演示模式的提示，转换为友好语言
                if (resultStr.contains("演示模式")) {
                    return null; // 演示模式不提供有价值信息
                }
                // 如果是无条件通过，简化显示
                if (resultStr.contains("无条件表达式")) {
                    return null; // 流程检查不需要告诉用户
                }
                return resultStr;
            }
        }
        
        return null;
    }
    
    /**
     * 从所有步骤生成总结（使用AI生成最终回答）
     */
    private String generateSummaryFromSteps(Map<String, Object> context) {
        try {
            // 收集所有步骤的原始数据 - 只收集有有效内容的步骤
            StringBuilder allStepData = new StringBuilder();
            int stepNumber = 1;
            
            for (Map.Entry<String, Object> entry : context.entrySet()) {
                if (!"query".equals(entry.getKey()) && !"kbIds".equals(entry.getKey())) {
                    String stepId = entry.getKey();
                    Object result = entry.getValue();
                    
                    if (result instanceof Map) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> resultMap = (Map<String, Object>) result;
                        Boolean success = (Boolean) resultMap.get("success");
                        String stepDesc = (String) resultMap.get("stepDescription");
                        
                        // ✅ 只处理成功的步骤
                        if (Boolean.TRUE.equals(success)) {
                            // 提取原始内容
                            String content = null;
                            if (resultMap.containsKey("reasoningResult")) {
                                content = (String) resultMap.get("reasoningResult");
                            } else if (resultMap.containsKey("summary")) {
                                content = (String) resultMap.get("summary");
                            }
                            
                            if (content != null && !content.isEmpty()) {
                                // 过滤技术日志
                                if (!content.contains("{success=") && !content.contains("演示模式") && !content.contains("无条件表达式")) {
                                    allStepData.append(String.format("**第%d步 %s**:\n%s\n\n", 
                                        stepNumber++, 
                                        stepDesc != null ? stepDesc : stepId,
                                        content));
                                }
                                // ❌ 如果内容是技术日志，直接跳过，不添加到总结中
                            }
                            // ❌ 如果没有内容，也跳过
                        }
                        // ❌ 失败的步骤不添加到总结中
                    }
                }
            }
            
            // 使用AI生成最终总结
            return generateAiSummary(allStepData.toString());
            
        } catch (Exception e) {
            log.error("生成AI总结失败，使用降级方案", e);
            return generateFallbackSummary(context);
        }
    }
    
    /**
     * 使用AI生成最终总结
     */
    private String generateAiSummary(String allStepData) {
        try {
            // 获取默认模型
            var model = aiModelDao.getFirstChatModel();
            if (model == null) {
                log.warn("没有可用的ChatModel，使用降级方案");
                return "*AI模型不可用，无法生成智能总结*";
            }
            
            // 获取模型提供商
            var provider = modelProviderDao.getById(model.getProviderId());
            if (provider == null) {
                log.warn("模型提供商不存在: providerId={}", model.getProviderId());
                return "*模型配置错误，无法生成智能总结*";
            }
            
            // 创建ChatClient
            ChatClient chatClient = aiModelSupport.createChatClient(model, provider);
            
            // 构建提示词
            String systemPrompt = """
                你是一个专业的智能助手，擅长将复杂的技术分析转化为通俗易懂的建议。
                
                请根据以下步骤的执行结果，生成一个综合性的最终回答。
                
                重要要求：
                1. **语言风格**：像朋友聊天一样自然，不要机械化
                2. **禁止术语**：不要使用"根据分析"、"系统显示"、"推理结论"、"步骤"等技术词汇
                3. **结构化输出**：使用Markdown格式，包含标题、列表、重点标注
                4. **实用建议**：给出具体、可操作的建议，不要太抽象
                5. **篇幅适中**：300-600字，重点突出
                6. **友好语气**：温暖、鼓励、有帮助性
                
                输出结构建议：
                - 开头：简要说明整体情况
                - 主体：分点列出关键洞察和建议
                - 结尾：鼓励和下一步行动
                
                示例对比：
                ❌ 坏例子："根据系统分析，用户在追求期需要注意..."
                ✅ 好例子："在刚开始接触的阶段，有几个小建议可能会帮到你..."
                """;
            
            String userPrompt = String.format("""
                以下是任务执行的各个步骤结果：
                
                %s
                
                请基于以上信息，生成一个通俗易懂、实用性强的综合建议和总结。
                记住：要像跟朋友聊天一样自然，不要用技术术语！
                """, allStepData);
            
            // 调用AI生成总结
            String aiSummary = chatClient.prompt()
                .system(systemPrompt)
                .user(userPrompt)
                .call()
                .content();
            
            if (aiSummary != null && !aiSummary.trim().isEmpty()) {
                return aiSummary;
            }
            
            return "*AI未能生成有效总结*";
            
        } catch (Exception e) {
            log.error("AI生成总结异常", e);
            return "*AI总结生成失败*";
        }
    }
    
    /**
     * 降级方案：生成简单总结
     */
    private String generateFallbackSummary(Map<String, Object> context) {
        StringBuilder summary = new StringBuilder();
        
        int successCount = 0;
        int failedCount = 0;
        List<String> keyFindings = new ArrayList<>();
        
        for (Map.Entry<String, Object> entry : context.entrySet()) {
            if (!"query".equals(entry.getKey()) && !"kbIds".equals(entry.getKey())) {
                Object result = entry.getValue();
                
                if (result instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> resultMap = (Map<String, Object>) result;
                    Boolean success = (Boolean) resultMap.get("success");
                    
                    if (Boolean.TRUE.equals(success)) {
                        successCount++;
                        
                        // 提取关键发现
                        if (resultMap.containsKey("reasoningResult")) {
                            String reasoning = (String) resultMap.get("reasoningResult");
                            if (reasoning != null && reasoning.length() > 50) {
                                keyFindings.add(reasoning.substring(0, Math.min(100, reasoning.length())) + "...");
                            }
                        } else if (resultMap.containsKey("retrievedDocs")) {
                            Integer docCount = (Integer) resultMap.get("retrievedDocs");
                            keyFindings.add(String.format("检索到 %d 个相关文档", docCount));
                        }
                    } else {
                        failedCount++;
                    }
                }
            }
        }
        
        summary.append(String.format("本次任务共执行 %d 个步骤，其中 %d 个成功，%d 个失败。\n\n", 
            successCount + failedCount, successCount, failedCount));
        
        if (!keyFindings.isEmpty()) {
            summary.append("**关键发现**:\n\n");
            for (int i = 0; i < keyFindings.size(); i++) {
                summary.append(String.format("%d. %s\n", i + 1, keyFindings.get(i)));
            }
            summary.append("\n");
        }
        
        summary.append("**建议**: 基于以上分析，建议您根据具体情况进行调整和优化。");
        
        return summary.toString();
    }
    
    /**
     * 格式化步骤结果（提取关键信息）
     */
    private String formatStepResult(String stepId, Object result) {
        if (result == null) {
            return "无结果";
        }
        
        if (result instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> resultMap = (Map<String, Object>) result;
            
            // 提取描述
            String description = (String) resultMap.get("stepDescription");
            if (description == null) {
                description = stepId;
            }
            
            // 检查是否成功
            Boolean success = (Boolean) resultMap.get("success");
            
            if (Boolean.FALSE.equals(success)) {
                // 失败：显示错误信息
                String error = (String) resultMap.get("error");
                return String.format("%s - ❌ 失败: %s", description, error != null ? error : "未知错误");
            } else {
                // 成功：提取关键结果
                if (resultMap.containsKey("reasoningResult")) {
                    // LLM推理结果
                    String reasoning = (String) resultMap.get("reasoningResult");
                    // 只取第一行或前100字符
                    if (reasoning != null && reasoning.length() > 100) {
                        reasoning = reasoning.substring(0, 100) + "...";
                    }
                    return String.format("%s - ✅ 推理完成", description);
                } else if (resultMap.containsKey("summary")) {
                    // 汇总结果
                    return String.format("%s - ✅ 汇总完成（共%d个步骤）", 
                        description, 
                        resultMap.getOrDefault("totalSteps", 0));
                } else if (resultMap.containsKey("retrievedDocs")) {
                    // 知识库检索
                    return String.format("%s - ✅ 检索到%d个文档", 
                        description,
                        resultMap.getOrDefault("retrievedDocs", 0));
                } else if (resultMap.containsKey("result")) {
                    // 通用结果
                    Object res = resultMap.get("result");
                    String resultStr = res != null ? res.toString() : "完成";
                    if (resultStr.length() > 50) {
                        resultStr = resultStr.substring(0, 50) + "...";
                    }
                    return String.format("%s - ✅ %s", description, resultStr);
                } else {
                    return String.format("%s - ✅ 完成", description);
                }
            }
        }
        
        return result.toString();
    }
    
    /**
     * 为前端格式化步骤结果（返回结构化数据而非字符串）
     */
    private Map<String, Object> formatStepResultForFrontend(String stepId, Object result) {
        if (result == null) {
            return Map.of(
                "status", "unknown",
                "message", "无结果"
            );
        }
        
        if (result instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> resultMap = (Map<String, Object>) result;
            
            Boolean success = (Boolean) resultMap.get("success");
            
            if (Boolean.FALSE.equals(success)) {
                // 失败
                String error = (String) resultMap.get("error");
                return Map.of(
                    "status", "failed",
                    "error", error != null ? error : "未知错误",
                    "rawResult", result
                );
            } else {
                // 成功 - 提取友好的消息
                String message = "";
                String detailType = "general";
                
                if (resultMap.containsKey("reasoningResult")) {
                    message = "✅ 推理分析完成";
                    detailType = "reasoning";
                } else if (resultMap.containsKey("summary")) {
                    Integer totalSteps = (Integer) resultMap.getOrDefault("totalSteps", 0);
                    message = String.format("✅ 汇总完成（共%d个步骤）", totalSteps);
                    detailType = "summary";
                } else if (resultMap.containsKey("retrievedDocs")) {
                    Integer docCount = (Integer) resultMap.getOrDefault("retrievedDocs", 0);
                    message = String.format("✅ 检索到 %d 个相关文档", docCount);
                    detailType = "retrieval";
                } else if (resultMap.containsKey("result")) {
                    Object res = resultMap.get("result");
                    String resultStr = res != null ? res.toString() : "完成";
                    if (resultStr.length() > 100) {
                        resultStr = resultStr.substring(0, 100) + "...";
                    }
                    message = "✅ " + resultStr;
                    detailType = "processing";
                } else {
                    message = "✅ 执行完成";
                }
                
                return Map.of(
                    "status", "success",
                    "message", message,
                    "detailType", detailType,
                    "rawResult", result  // 保留原始数据供需要时使用
                );
            }
        }
        
        return Map.of(
            "status", "unknown",
            "message", result.toString()
        );
    }
    
    private List<com.esdllm.agentmesh.model.dto.DecisionStep> buildDecisionPathFromSteps(
            List<TaskExecutionPlan.TaskStep> steps, 
            Map<String, Object> context) {
        
        List<com.esdllm.agentmesh.model.dto.DecisionStep> decisionPath = new ArrayList<>();
        for (TaskExecutionPlan.TaskStep step : steps) {
            var decisionStep = new com.esdllm.agentmesh.model.dto.DecisionStep();
            decisionStep.setStepId(step.getStepId());
            decisionStep.setStepType(step.getStepType());
            decisionStep.setDescription(step.getDescription());
            decisionStep.setResourceId(step.getResourceId());
            decisionStep.setInputData(step.getInputParams() instanceof Map ? 
                (Map<String, Object>) step.getInputParams() : Map.of("step", step));
            decisionStep.setOutputData(context.get(step.getStepId()));
            decisionStep.setStatus("COMPLETED");
            decisionStep.setDurationMs(step.getEstimatedDurationMs());
            decisionPath.add(decisionStep);
        }
        
        return decisionPath;
    }
    
    @Override
    public Long createWorkflowTemplate(WorkflowTemplate template) {
        log.info("创建工作流模板: {}", template.getTemplateName());
        
        Long templateId = nextTemplateId++;
        template.setTemplateId(templateId);
        template.setCreatedAt(System.currentTimeMillis());
        template.setUpdatedAt(System.currentTimeMillis());
        template.setUsageCount(0);
        
        if ("SEMI_CUSTOM".equals(template.getWorkflowMode())) {
            List<WorkflowTemplate.TemplateNode> allNodes = new ArrayList<>();
            if (template.getUserDefinedNodes() != null) {
                allNodes.addAll(template.getUserDefinedNodes());
            }
            if (template.getAiGeneratedNodes() != null) {
                allNodes.addAll(template.getAiGeneratedNodes());
            }
            template.setAllNodes(allNodes);
        } else if ("FULL_CUSTOM".equals(template.getWorkflowMode())) {
            template.setAllNodes(template.getUserDefinedNodes());
        }
        
        workflowTemplateCache.put(templateId, template);
        
        log.info("工作流模板创建成功,templateId: {}, mode: {}", 
                templateId, template.getWorkflowMode());
        
        return templateId;
    }
    
    @Override
    public WorkflowTemplate aiAssistWorkflow(Long agentId, String taskDescription, 
                                            List<WorkflowTemplate.TemplateNode> userDefinedNodes, 
                                            Long userId) {
        log.info("=== AI辅助生成工作流 ===");
        
        try {
            IntentRecognitionResult intent = intentRecognitionService.recognizeIntentWithContext(
                taskDescription, agentId, userId
            );
            
            List<com.esdllm.agentmesh.model.domain.Tools> matchedTools = Collections.emptyList();
            if (intent.getNeedToolCall()) {
                matchedTools = toolMatchingService.matchToolsByIntent(
                    intent.getIntentType(), taskDescription, userId
                );
            }
            
            List<WorkflowTemplate.TemplateNode> aiGeneratedNodes = generateAiNodes(
                intent, matchedTools, userDefinedNodes, taskDescription
            );
            
            WorkflowTemplate template = WorkflowTemplate.builder()
                .templateName("AI辅助: " + taskDescription.substring(0, Math.min(50, taskDescription.length())))
                .description(taskDescription)
                .workflowMode("SEMI_CUSTOM")
                .userDefinedNodes(userDefinedNodes != null ? userDefinedNodes : Collections.emptyList())
                .aiGeneratedNodes(aiGeneratedNodes)
                .agentId(agentId)
                .userId(userId)
                .isPublic(false)
                .build();
            
            Long templateId = createWorkflowTemplate(template);
            template.setTemplateId(templateId);
            
            log.info("=== AI辅助工作流生成完成 === 生成了 {} 个AI节点", aiGeneratedNodes.size());
            
            return template;
            
        } catch (Exception e) {
            log.error("AI辅助生成工作流失败", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "AI辅助生成失败: " + e.getMessage());
        }
    }
    
    @Override
    public List<WorkflowTemplate> getWorkflowTemplates(Long userId, String mode) {
        log.info("获取工作流模板列表,userId: {}, mode: {}", userId, mode);
        
        return workflowTemplateCache.values().stream()
            .filter(t -> t.getUserId().equals(userId) || Boolean.TRUE.equals(t.getIsPublic()))
            .filter(t -> mode == null || mode.equals(t.getWorkflowMode()))
            .sorted((a, b) -> Long.compare(b.getCreatedAt(), a.getCreatedAt()))
            .toList();
    }
    
    @Override
    public DecisionExecutionResult executeFromTemplate(Long templateId, 
                                                       Map<String, Object> inputParams, 
                                                       Long userId) {
        log.info("基于模板执行工作流,templateId: {}", templateId);
        
        WorkflowTemplate template = workflowTemplateCache.get(templateId);
        if (template == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "工作流模板不存在");
        }
        
        template.setUsageCount(template.getUsageCount() + 1);
        
        switch (template.getWorkflowMode()) {
            case "FULL_AUTO":
                return simpleTaskHandler.executeSimpleTask(template.getDescription(), template.getAgentId(), userId);
                
            case "SEMI_CUSTOM":
            case "FULL_CUSTOM":
                log.warn("自定义工作流执行暂未完全实现,使用简单任务处理器代替");
                return simpleTaskHandler.executeSimpleTask(template.getDescription(), template.getAgentId(), userId);
                
            default:
                throw new BusinessException(ErrorCode.PARAMS_ERROR, 
                    "不支持的工作流模式: " + template.getWorkflowMode());
        }
    }
    
    private List<WorkflowTemplate.TemplateNode> generateAiNodes(
            IntentRecognitionResult intent,
            List<com.esdllm.agentmesh.model.domain.Tools> matchedTools,
            List<WorkflowTemplate.TemplateNode> userDefinedNodes,
            String taskDescription) {
        
        List<WorkflowTemplate.TemplateNode> aiNodes = new ArrayList<>();
        int nodeNumber = 1;
        
        if (intent.getNeedToolCall() && !matchedTools.isEmpty()) {
            Set<Long> userDefinedToolIds = userDefinedNodes != null ? 
                userDefinedNodes.stream()
                    .filter(n -> n.getResourceId() != null)
                    .map(WorkflowTemplate.TemplateNode::getResourceId)
                    .collect(java.util.stream.Collectors.toSet()) :
                Collections.emptySet();
            
            for (var tool : matchedTools) {
                if (!userDefinedToolIds.contains(tool.getId())) {
                    aiNodes.add(WorkflowTemplate.TemplateNode.builder()
                        .nodeId("ai_node_" + nodeNumber++)
                        .nodeName("调用工具: " + tool.getDisplayName())
                        .nodeType("TOOL_CALL")
                        .resourceId(tool.getId())
                        .resourceName(tool.getDisplayName())
                        .description("AI自动补充的工具调用节点")
                        .isAiGenerated(true)
                        .isEditable(true)
                        .timeoutMs(5000L)
                        .errorStrategy("CONTINUE")
                        .build()
                    );
                }
            }
        }
        
        if (intent.getMatchedKbIds() != null && !intent.getMatchedKbIds().isEmpty()) {
            aiNodes.add(WorkflowTemplate.TemplateNode.builder()
                .nodeId("ai_node_" + nodeNumber++)
                .nodeName("检索知识库")
                .nodeType("KNOWLEDGE_RETRIEVAL")
                .description("AI自动补充的知识库检索节点")
                .isAiGenerated(true)
                .isEditable(false)
                .timeoutMs(3000L)
                .build()
            );
        }
        
        if (taskDescription.contains("如果") || taskDescription.contains("条件")) {
            aiNodes.add(WorkflowTemplate.TemplateNode.builder()
                .nodeId("ai_node_" + nodeNumber++)
                .nodeName("条件判断")
                .nodeType("CONDITION")
                .conditionExpression("${result} != null")
                .description("AI自动补充的条件判断节点")
                .isAiGenerated(true)
                .isEditable(true)
                .build()
            );
        }
        
        aiNodes.add(WorkflowTemplate.TemplateNode.builder()
            .nodeId("ai_node_" + nodeNumber)
            .nodeName("生成最终回答")
            .nodeType("RESPONSE_GENERATION")
            .description("AI自动补充的结果汇总节点")
            .isAiGenerated(true)
            .isEditable(false)
            .timeoutMs(3000L)
            .build()
        );
        
        return aiNodes;
    }
    
    // ========== 辅助方法 ==========
    
    private Agent loadAgent(Long agentId, Long userId) {
        Agent agent = agentDao.getById(agentId);
        if (agent == null) {
            throw new BusinessException(ErrorCode.AGENT_NOT_FOUND, "智能体不存在");
        }
        
        // 检查权限:所有者或已发布的智能体(status=1)
        boolean isOwner = agent.getUserId().equals(userId);
        boolean isPublished = agent.getStatus() != null && agent.getStatus() == 1;
        
        if (!isOwner && !isPublished) {
            throw new BusinessException(ErrorCode.NO_AUTH, "无权限使用该智能体");
        }
        
        return agent;
    }

    @Override
    public Map<String, Object> enrichContextWithMemories(Long userId, Long agentId,
                                                         String query, Map<String, Object> context) {
        Map<String, Object> enrichedContext = context != null ? new HashMap<>(context) : new HashMap<>();
        
        try {
            List<String> memoryTypes = Arrays.asList("USER_PREFERENCE", "PROJECT_CONTEXT", "DECISION_LOGIC");
            var memories = memoryService.retrieveMemories(userId, agentId, query, memoryTypes, 5);
            
            if (!memories.isEmpty()) {
                log.info("检索到 {} 条相关记忆", memories.size());
                
                List<Map<String, Object>> memoryList = new ArrayList<>();
                for (var memory : memories) {
                    Map<String, Object> memMap = new HashMap<>();
                    memMap.put("type", memory.getMemoryType());
                    memMap.put("content", memory.getMemoryValue());
                    memMap.put("confidence", memory.getConfidenceScore());
                    memMap.put("usageCount", memory.getUsageCount());
                    memoryList.add(memMap);
                    memoryService.recordMemoryAccess(memory.getId());
                }
                
                enrichedContext.put("_long_term_memories", memoryList);
            }
            
            try {
                var userProfile = memoryService.getUserProfile(userId);
                if (!userProfile.isEmpty()) {
                    enrichedContext.put("_user_profile", userProfile);
                }
            } catch (Exception e) {
                log.warn("获取用户画像失败", e);
            }
            
        } catch (Exception e) {
            log.error("记忆检索失败,使用原始上下文", e);
        }
        
        return enrichedContext;
    }
    
    private DecisionExecutionResult executeWithWorkflow(Long workflowId, 
                                                        Map<String, Object> context,
                                                        Long userId, Agent agent) {
        DecisionExecutionResult result = new DecisionExecutionResult();
        result.setSuccess(false);
        result.setErrorMessage("工作流执行功能开发中");
        return result;
    }
    
    private List<com.esdllm.agentmesh.model.domain.Tools> getAvailableTools(Long agentId, Long userId) {
        log.debug("获取智能体 {} 的可用工具", agentId);
        return Collections.emptyList();
    }
    
    private void scheduleTaskPlanCleanup(String taskId, long delayMs) {
        CompletableFuture.delayedExecutor(delayMs, java.util.concurrent.TimeUnit.MILLISECONDS)
            .execute(() -> {
                taskPlanCache.remove(taskId);
                log.debug("任务计划已清理: {}", taskId);
            });
    }
    
    @Override
    public Object executeCollaboratively(Long mainAgentId, String query, Long userId, Map<String, Object> context) {
        log.info("=== 统一引擎调用多智能体协同 ===");
        return agentOrchestrator.executeCollaboratively(mainAgentId, query, userId, context);
    }
    
    /**
     * 保存对话日志
     */
    private void saveConversationLog(Long agentId, String query, Long userId, 
                                    String finalResponse, String decisionPath, long startTime, String sessionId) {
        try {
            log.info("=== 准备保存对话日志 === agentId: {}, userId: {}, sessionId: {}", agentId, userId, sessionId);
            
            // 构建决策步骤列表
            List<com.esdllm.agentmesh.model.dto.DecisionStep> steps = new java.util.ArrayList<>();
            
            // 创建执行结果对象
            DecisionExecutionResult result = new DecisionExecutionResult();
            result.setSuccess(true);
            result.setFinalResponse(finalResponse);
            result.setExecutionTimeMs(System.currentTimeMillis() - startTime);
            result.setDecisionPath(steps);
            
            // 保存到数据库（使用传入的sessionId）
            conversationLogService.logConversationWithIntent(
                userId, agentId, sessionId, query, null, result
            );
            
            log.info("✅ 对话日志保存成功, sessionId: {}, query: {}", sessionId, query.substring(0, Math.min(50, query.length())));
            
        } catch (Exception e) {
            log.error("❌ 保存对话日志失败, sessionId: {}", sessionId, e);
        }
    }
}
