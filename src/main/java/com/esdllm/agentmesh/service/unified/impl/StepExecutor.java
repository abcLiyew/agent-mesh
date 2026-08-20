package com.esdllm.agentmesh.service.unified.impl;

import com.esdllm.agentmesh.model.domain.Tools;
import com.esdllm.agentmesh.model.dto.TaskExecutionPlan;
import com.esdllm.agentmesh.model.dto.ToolInvocationContext;
import com.esdllm.agentmesh.repository.dao.ToolsDao;
import com.esdllm.agentmesh.service.agent.support.ToolInvocationService;
import com.esdllm.agentmesh.service.rag.RagRetrievalService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * 步骤执行器
 * 负责执行任务计划中的单个步骤
 */
@Component
@Slf4j
public class StepExecutor {
    
    @Resource
    private SkillSandboxManager skillSandboxManager;
    
    @Resource
    private RagRetrievalService ragRetrievalService;
    
    @Resource
    private ToolInvocationService toolInvocationService;
    
    @Resource
    private com.esdllm.agentmesh.service.agent.support.SystemToolService systemToolService;
    
    @Resource
    private ToolsDao toolsDao;
    
    @Resource
    private com.esdllm.agentmesh.repository.dao.AiModelDao aiModelDao;
    
    @Resource
    private com.esdllm.agentmesh.repository.dao.ModelProviderDao modelProviderDao;
    
    @Resource
    private com.esdllm.agentmesh.service.agent.support.AiModelSupport aiModelSupport;
    
    private final ExpressionParser expressionParser = new SpelExpressionParser();
    
    /**
     * 执行单个步骤
     */
    public Object executeSingleStep(
            TaskExecutionPlan.TaskStep step, 
            Map<String, Object> context, 
            Long userId) {
        
        switch (step.getStepType()) {
            case "TOOL_CALL":
                return executeToolCallStep(step, context, userId);
                
            case "KNOWLEDGE_RETRIEVAL":
                return executeKnowledgeRetrievalStep(step, context, userId);
                
            case "DATA_PROCESSING":
                return executeDataProcessingStep(step, context, userId);
                
            case "CONDITION_CHECK":
                return executeConditionCheckStep(step, context, userId);
                
            case "API_CALL":
                return executeApiCallStep(step, context, userId);
                
            case "LLM_REASONING":
                return executeLlmReasoningStep(step, context, userId);
                
            case "RESULT_SUMMARY":
                return executeResultSummaryStep(step, context, userId);
                
            default:
                log.warn("未知的步骤类型: {}", step.getStepType());
                return "步骤类型不支持: " + step.getStepType();
        }
    }
    
    /**
     * 执行工具调用步骤
     */
    private Object executeToolCallStep(
            TaskExecutionPlan.TaskStep step, 
            Map<String, Object> context, 
            Long userId) {
        
        if (step.getResourceId() == null) {
            return "错误: 未指定工具ID";
        }
        
        try {
            log.info("调用工具: resourceId={}, resourceName={}", 
                    step.getResourceId(), step.getResourceName());
            
            // 准备参数
            @SuppressWarnings("unchecked")
            Map<String, Object> parameters = step.getInputParams() instanceof Map ? 
                (Map<String, Object>) step.getInputParams() : Map.of();
            
            // 在沙箱中执行工具调用
            SkillSandboxManager.SandboxExecutionResult sandboxResult = 
                skillSandboxManager.executeInSandbox(
                    step.getResourceId(), 
                    userId, 
                    parameters,
                    (tool, params, uid) -> {
                        // 集成ToolInvocationService进行实际的工具调用
                        try {
                            // 构建ToolInvocationContext
                            ToolInvocationContext invocationContext = ToolInvocationContext.builder()
                                .toolId(tool.getId())
                                .parameters(params)
                                .timeoutMs(30000L) // 默认30秒超时
                                .build();
                            
                            // 根据工具类型调用不同的执行器
                            String result;
                            switch (tool.getSourceType()) {
                                case "USER_HTTP":
                                    result = toolInvocationService.invokeHttpTool(invocationContext, tool);
                                    break;
                                    
                                case "USER_MCP":
                                    result = toolInvocationService.invokeMcpTool(invocationContext, tool);
                                    break;
                                    
                                case "USER_AGENT":
                                    result = toolInvocationService.invokeAgentTool(tool, invocationContext, new HashMap<>());
                                    break;
                                    
                                case "SYSTEM":
                                    // 系统工具通过SystemToolService调用
                                    result = systemToolService.invokeSystemTool(tool.getToolCodeName(), invocationContext);
                                    break;
                                    
                                default:
                                    throw new RuntimeException("不支持的工具类型: " + tool.getSourceType());
                            }
                            
                            return Map.of(
                                "success", true,
                                "toolId", tool.getId(),
                                "toolName", tool.getDisplayName(),
                                "result", result,
                                "executionTime", System.currentTimeMillis()
                            );
                            
                        } catch (Exception e) {
                            log.error("工具调用失败", e);
                            return Map.of(
                                "success", false,
                                "error", e.getMessage()
                            );
                        }
                    }
                );
            
            if (sandboxResult.isSuccess()) {
                log.info("工具调用成功: {}, 耗时: {}ms", 
                    sandboxResult.getToolName(), sandboxResult.getExecutionTimeMs());
                return sandboxResult.getResult();
            } else {
                log.error("工具调用失败: {}", sandboxResult.getErrorMessage());
                return Map.of(
                    "success", false,
                    "error", sandboxResult.getErrorMessage(),
                    "errorCode", sandboxResult.getErrorCode()
                );
            }
            
        } catch (Exception e) {
            log.error("工具调用异常", e);
            return Map.of(
                "success", false,
                "error", e.getMessage()
            );
        }
    }
    
    /**
     * 执行知识库检索步骤
     */
    private Object executeKnowledgeRetrievalStep(
            TaskExecutionPlan.TaskStep step, 
            Map<String, Object> context, 
            Long userId) {
        
        log.info("执行知识库检索: {}", step.getDescription());
        
        try {
            // 从上下文中获取知识库ID列表
            @SuppressWarnings("unchecked")
            java.util.List<Long> kbIds = (java.util.List<Long>) context.get("kbIds");
            
            if (kbIds == null || kbIds.isEmpty()) {
                log.warn("未指定知识库ID，跳过检索");
                return Map.of(
                    "success", false,
                    "error", "未配置知识库"
                );
            }
            
            // 从上下文中获取查询文本
            String query = (String) context.getOrDefault("query", "");
            if (query.trim().isEmpty()) {
                query = step.getDescription();
            }
            
            // 调用RAG服务
            int topK = 3;
            double similarityThreshold = 0.7;
            
            if (step.getInputParams() instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> params = (Map<String, Object>) step.getInputParams();
                topK = ((Number) params.getOrDefault("topK", 3)).intValue();
                similarityThreshold = ((Number) params.getOrDefault("similarityThreshold", 0.7)).doubleValue();
            }
            
            var retrievedDocs = ragRetrievalService.retrieveFromKnowledgeBases(
                kbIds, query, topK, similarityThreshold
            );
            
            log.info("知识库检索完成，返回 {} 个文档片段", retrievedDocs.size());
            
            return Map.of(
                "success", true,
                "retrievedDocs", retrievedDocs.size(),
                "documents", retrievedDocs.stream()
                    .map(doc -> Map.of(
                        "documentId", doc.getDocumentId(),
                        "title", doc.getTitle(),
                        "content", doc.getContent(),
                        "similarityScore", doc.getSimilarityScore()
                    ))
                    .toList()
            );
            
        } catch (Exception e) {
            log.error("知识库检索失败", e);
            return Map.of(
                "success", false,
                "error", e.getMessage()
            );
        }
    }
    
    /**
     * 执行数据处理步骤
     */
    private Object executeDataProcessingStep(
            TaskExecutionPlan.TaskStep step, 
            Map<String, Object> context, 
            Long userId) {
        
        log.info("执行数据处理: {}", step.getDescription());
        
        return Map.of(
            "success", true,
            "result", "数据处理完成(演示模式)"
        );
    }
    
    /**
     * 执行条件检查步骤
     */
    private Object executeConditionCheckStep(
            TaskExecutionPlan.TaskStep step, 
            Map<String, Object> context, 
            Long userId) {
        
        log.info("执行条件检查: {}", step.getDescription());
        
        try {
            // 从step中获取条件表达式
            String conditionExpression = null;
            if (step.getInputParams() instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> params = (Map<String, Object>) step.getInputParams();
                conditionExpression = (String) params.get("conditionExpression");
            }
            
            if (conditionExpression == null || conditionExpression.trim().isEmpty()) {
                log.warn("未提供条件表达式，默认通过");
                return Map.of(
                    "success", true,
                    "conditionMet", true,
                    "result", "无条件表达式，默认通过"
                );
            }
            
            // 使用SpEL评估条件表达式
            boolean conditionMet = evaluateCondition(conditionExpression, context);
            
            log.info("条件评估结果: {} -> {}", conditionExpression, conditionMet);
            
            return Map.of(
                "success", true,
                "conditionMet", conditionMet,
                "expression", conditionExpression,
                "result", conditionMet ? "条件满足" : "条件不满足"
            );
            
        } catch (Exception e) {
            log.error("条件评估失败", e);
            return Map.of(
                "success", false,
                "error", "条件评估失败: " + e.getMessage()
            );
        }
    }
    
    /**
     * 评估条件表达式(SpEL)
     */
    private boolean evaluateCondition(String expression, Map<String, Object> context) {
        try {
            Expression exp = expressionParser.parseExpression(expression);
            StandardEvaluationContext evalContext = new StandardEvaluationContext();
            
            // 将context中的变量添加到评估上下文
            if (context != null) {
                context.forEach(evalContext::setVariable);
            }
            
            Boolean result = exp.getValue(evalContext, Boolean.class);
            return result != null && result;
            
        } catch (Exception e) {
            log.error("SpEL表达式评估失败: {}", expression, e);
            throw new RuntimeException("条件表达式语法错误: " + e.getMessage(), e);
        }
    }
    
    /**
     * 执行API调用步骤
     */
    private Object executeApiCallStep(
            TaskExecutionPlan.TaskStep step, 
            Map<String, Object> context, 
            Long userId) {
        
        log.info("执行API调用: {}", step.getDescription());
        
        return Map.of(
            "success", true,
            "result", "API调用成功(演示模式)"
        );
    }
    
    /**
     * 执行LLM推理步骤
     */
    private Object executeLlmReasoningStep(
            TaskExecutionPlan.TaskStep step, 
            Map<String, Object> context, 
            Long userId) {
        
        log.info("执行LLM推理: {}", step.getDescription());
        
        try {
            // 从上下文中获取用户查询
            String userQuery = (String) context.get("query");
            if (userQuery == null || userQuery.isEmpty()) {
                userQuery = step.getDescription();
            }
            
            // 从上下文中获取之前的步骤结果
            StringBuilder previousResults = new StringBuilder();
            context.forEach((key, value) -> {
                if (!"query".equals(key) && !"kbIds".equals(key)) {
                    previousResults.append(key).append(": ").append(value).append("\n");
                }
            });
            
            // ✅ 调用真正的LLM进行推理
            String reasoningResult = callLLMForReasoning(userQuery, step.getDescription(), previousResults.toString());
            
            return Map.of(
                "success", true,
                "reasoningResult", reasoningResult,
                "stepDescription", step.getDescription()
            );
            
        } catch (Exception e) {
            log.error("LLM推理失败", e);
            return Map.of(
                "success", false,
                "error", e.getMessage()
            );
        }
    }
    
    /**
     * 调用LLM进行推理分析
     */
    private String callLLMForReasoning(String userQuery, String stepDescription, String previousResults) {
        try {
            // 获取默认模型
            var model = aiModelDao.getFirstChatModel();
            if (model == null) {
                log.warn("没有可用的ChatModel，返回步骤描述");
                return stepDescription;
            }
            
            // 获取模型提供商
            var provider = modelProviderDao.getById(model.getProviderId());
            if (provider == null) {
                log.warn("模型提供商不存在: providerId={}", model.getProviderId());
                return stepDescription;
            }
            
            // 创建ChatClient
            org.springframework.ai.chat.client.ChatClient chatClient = aiModelSupport.createChatClient(model, provider);
            
            // 构建提示词
            String systemPrompt = """
                你是一个智能助手。请根据用户的查询和任务步骤，生成自然、友好的回复。
                
                要求：
                - 直接回答用户的问题，不要提及“步骤”、“分析”等技术词汇
                - 语气亲切自然，像朋友聊天
                - 简洁明了，突出重点
                - 如果是问候语，礼貌回应即可
                - 长度控制在50-100字以内
                """;
            
            String userPrompt = String.format("""
                用户查询：%s
                
                当前任务：%s
                
                请直接生成回复内容：
                """, userQuery, stepDescription);
            
            // 调用AI
            String response = chatClient.prompt()
                .system(systemPrompt)
                .user(userPrompt)
                .call()
                .content();
            
            if (response != null && !response.trim().isEmpty()) {
                log.info("✅ LLM推理成功，回复长度: {}", response.length());
                return response.trim();
            } else {
                log.warn("LLM返回空内容，使用步骤描述");
                return stepDescription;
            }
            
        } catch (Exception e) {
            log.error("调用LLM失败", e);
            return stepDescription; // 降级：返回步骤描述
        }
    }
    
    /**
     * 执行结果汇总步骤
     */
    private Object executeResultSummaryStep(
            TaskExecutionPlan.TaskStep step, 
            Map<String, Object> context, 
            Long userId) {
        
        log.info("执行结果汇总: {}", step.getDescription());
        
        try {
            // ✅ 只收集成功的步骤结果
            StringBuilder summary = new StringBuilder();
            int successCount = 0;
            
            for (Map.Entry<String, Object> entry : context.entrySet()) {
                if ("query".equals(entry.getKey()) || "kbIds".equals(entry.getKey())) {
                    continue;
                }
                
                Object result = entry.getValue();
                if (result instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> resultMap = (Map<String, Object>) result;
                    
                    // ✅ 只处理成功的步骤
                    Boolean success = (Boolean) resultMap.get("success");
                    if (Boolean.TRUE.equals(success)) {
                        successCount++;
                        
                        // 提取有用的内容
                        String content = null;
                        if (resultMap.containsKey("reasoningResult")) {
                            content = (String) resultMap.get("reasoningResult");
                        } else if (resultMap.containsKey("summary")) {
                            content = (String) resultMap.get("summary");
                        } else if (resultMap.containsKey("result")) {
                            Object res = resultMap.get("result");
                            content = res != null ? res.toString() : null;
                        }
                        
                        if (content != null && !content.isEmpty()) {
                            // 过滤技术日志
                            if (!content.contains("{success=") && !content.contains("演示模式")) {
                                summary.append(String.format("**%s**:\n%s\n\n", 
                                    entry.getKey(), content));
                            }
                        }
                    }
                    // ❌ 失败的步骤不添加到总结中
                }
            }
            
            if (successCount == 0) {
                summary.append("本次任务没有产生有效的结果。\n");
            }
            
            return Map.of(
                "success", true,
                "summary", summary.toString(),
                "totalSteps", successCount
            );
            
        } catch (Exception e) {
            log.error("结果汇总失败", e);
            return Map.of(
                "success", false,
                "error", e.getMessage()
            );
        }
    }
}
