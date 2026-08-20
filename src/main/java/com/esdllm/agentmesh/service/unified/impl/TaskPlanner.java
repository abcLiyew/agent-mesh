package com.esdllm.agentmesh.service.unified.impl;

import com.esdllm.agentmesh.common.ErrorCode;
import com.esdllm.agentmesh.exception.BusinessException;
import com.esdllm.agentmesh.model.domain.Agent;
import com.esdllm.agentmesh.model.domain.AiModel;
import com.esdllm.agentmesh.model.domain.ModelProvider;
import com.esdllm.agentmesh.model.domain.Tools;
import com.esdllm.agentmesh.model.dto.IntentRecognitionResult;
import com.esdllm.agentmesh.model.dto.TaskExecutionPlan;
import com.esdllm.agentmesh.repository.dao.AiModelDao;
import com.esdllm.agentmesh.repository.dao.ModelProviderDao;
import com.esdllm.agentmesh.service.agent.support.AiModelSupport;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * AI驱动的任务规划器
 * 负责将复杂任务拆解为可执行的步骤清单(参考"龙虾"架构)
 */
@Component
@Slf4j
public class TaskPlanner {
    
    private final AiModelDao aiModelDao;
    private final ModelProviderDao modelProviderDao;
    private final ObjectMapper objectMapper;
    private final AiModelSupport aiModelSupport;
    
    public TaskPlanner(AiModelDao aiModelDao, ModelProviderDao modelProviderDao, 
                      ObjectMapper objectMapper, AiModelSupport aiModelSupport) {
        this.aiModelDao = aiModelDao;
        this.modelProviderDao = modelProviderDao;
        this.objectMapper = objectMapper;
        this.aiModelSupport = aiModelSupport;
    }
    
    /**
     * 使用AI生成结构化的任务计划
     */
    public List<TaskExecutionPlan.TaskStep> generateAiDrivenTaskPlan(
            String query, 
            Agent agent, 
            List<Tools> availableTools,
            Map<String, Object> context) {
        
        log.info("=== 使用AI生成任务计划 ===");
        log.info("query: {}, availableTools: {}", query, availableTools.size());
        
        try {
            // 获取决策模型
            AiModel decisionModel = getDecisionModel(agent);
            if (decisionModel == null) {
                log.warn("未找到决策模型,降级为规则规划");
                return generateRuleBasedTaskPlan(query, agent, availableTools);
            }
            
            // 构建工具描述
            String toolsDescription = buildToolsDescriptionForPlanning(availableTools);
            
            // 构建系统Prompt（使用智能体配置）
            String systemPrompt = buildSystemPrompt(agent, toolsDescription);
            String userPrompt = "用户任务:" + query;
            
            // 调用LLM生成任务计划
            ChatModel chatModel = createChatModel(decisionModel, agent.getUserId());
            ChatClient chatClient = ChatClient.builder(chatModel).build();
            
            String response = chatClient.prompt(new org.springframework.ai.chat.prompt.Prompt(userPrompt))
                .system(systemPrompt)
                .call()
                .content();
            
            if (response == null || response.trim().isEmpty()) {
                log.warn("LLM返回空响应,降级为规则规划");
                return generateRuleBasedTaskPlan(query, agent, availableTools);
            }
            
            log.info("LLM任务规划响应:{}", response);
            
            // 解析JSON响应
            List<TaskExecutionPlan.TaskStep> steps = parseTaskPlanFromJson(response, agent, availableTools);
            
            if (steps == null || steps.isEmpty()) {
                log.warn("解析任务计划失败,降级为规则规划");
                return generateRuleBasedTaskPlan(query, agent, availableTools);
            }
            
            log.info("=== AI任务规划成功 === 共 {} 个步骤", steps.size());
            return steps;
            
        } catch (Exception e) {
            log.error("AI任务规划失败,降级为规则规划", e);
            return generateRuleBasedTaskPlan(query, agent, availableTools);
        }
    }
    
    /**
     * 基于规则生成任务计划(降级方案)
     */
    public List<TaskExecutionPlan.TaskStep> generateRuleBasedTaskPlan(
            String query, 
            Agent agent, 
            List<Tools> availableTools) {
        
        List<TaskExecutionPlan.TaskStep> steps = new ArrayList<>();
        int stepNumber = 1;
        
        query = query.toLowerCase();
        
        // 1. 数据查询类任务
        if (query.contains("查询") || query.contains("查找") || query.contains("获取")) {
            List<Tools> matchedTools = findMatchingTools(query, availableTools);
            
            if (!matchedTools.isEmpty()) {
                for (Tools tool : matchedTools) {
                    steps.add(buildToolCallStep(stepNumber++, tool));
                }
            } else {
                steps.add(buildGenericQueryStep(stepNumber++));
            }
        }
        
        // 2. 数据处理类任务
        if (query.contains("处理") || query.contains("分析") || query.contains("计算")) {
            steps.add(TaskExecutionPlan.TaskStep.builder()
                .stepId("step_" + stepNumber)
                .stepNumber(stepNumber)
                .description("处理和分析数据")
                .stepType("DATA_PROCESSING")
                .estimatedDurationMs(3000L)
                .isRequired(true)
                .dependencies(getPreviousStepIds(steps))
                .build()
            );
            stepNumber++;
        }
        
        // 3. 条件判断类任务
        if (query.contains("如果") || query.contains("判断") || query.contains("检查")) {
            steps.add(TaskExecutionPlan.TaskStep.builder()
                .stepId("step_" + stepNumber)
                .stepNumber(stepNumber)
                .description("检查条件是否满足")
                .stepType("CONDITION_CHECK")
                .estimatedDurationMs(500L)
                .isRequired(true)
                .dependencies(getPreviousStepIds(steps))
                .build()
            );
            stepNumber++;
        }
        
        // 4. API调用类任务
        if (query.contains("api") || query.contains("接口") || query.contains("请求")) {
            steps.add(TaskExecutionPlan.TaskStep.builder()
                .stepId("step_" + stepNumber)
                .stepNumber(stepNumber)
                .description("调用外部API接口")
                .stepType("API_CALL")
                .estimatedDurationMs(2000L)
                .isRequired(true)
                .dependencies(getPreviousStepIds(steps))
                .build()
            );
            stepNumber++;
        }
        
        // 5. 知识库检索
        if (query.contains("知识") || query.contains("文档") || query.contains("资料")) {
            steps.add(TaskExecutionPlan.TaskStep.builder()
                .stepId("step_" + stepNumber)
                .stepNumber(stepNumber)
                .description("检索相关知识库")
                .stepType("KNOWLEDGE_RETRIEVAL")
                .estimatedDurationMs(1000L)
                .isRequired(false)
                .dependencies(getPreviousStepIds(steps))
                .build()
            );
            stepNumber++;
        }
        
        // 6. 最后一步:汇总结果
        steps.add(TaskExecutionPlan.TaskStep.builder()
            .stepId("step_" + stepNumber)
            .stepNumber(stepNumber)
            .description("汇总所有结果并生成最终回答")
            .stepType("RESULT_SUMMARY")
            .estimatedDurationMs(2000L)
            .isRequired(true)
            .dependencies(getPreviousStepIds(steps))
            .build()
        );
        
        log.info("基于规则生成了 {} 个步骤", steps.size());
        return steps;
    }
    
    /**
     * 构建系统Prompt（使用智能体配置）
     */
    private String buildSystemPrompt(Agent agent, String toolsDescription) {
        StringBuilder prompt = new StringBuilder();
            
        // 1. 使用智能体的 system_prompt（如果有）
        if (agent != null && agent.getSystemPrompt() != null && !agent.getSystemPrompt().isEmpty()) {
            prompt.append(agent.getSystemPrompt());
            prompt.append("\n\n");
        }
            
        // 2. 添加智能体的角色定义（如果有）
        if (agent != null && agent.getRoleDefinition() != null && !agent.getRoleDefinition().isEmpty()) {
            prompt.append("你的角色定位：");
            prompt.append(agent.getRoleDefinition());
            prompt.append("\n\n");
        }
            
        // 3. 添加智能体描述作为背景信息（如果有）
        if (agent != null && agent.getDescription() != null && !agent.getDescription().isEmpty()) {
            prompt.append("关于你的背景信息：");
            prompt.append(agent.getDescription());
            prompt.append("\n\n");
        }
            
        // 4. 添加任务规划的专业指令
        prompt.append("""
            你是一个专业的任务规划助手，参考“龙虾”架构进行任务拆解。
                
            你的职责是：
            1. 分析用户任务的复杂度
            2. 将复杂任务拆解为可执行的子任务清单
            3. 为每个子任务指定执行类型和资源
                
            可用的工具列表：
            %s
                
            任务拆解原则：
            - 原子性：每个步骤应该是一个独立、可执行的操作
            - 有序性：明确步骤之间的依赖关系
            - 可验证：每个步骤的结果应该是可验证的
                
            步骤类型说明：
            - TOOL_CALL: 调用工具获取数据或执行操作
            - KNOWLEDGE_RETRIEVAL: 从知识库检索相关信息
            - DATA_PROCESSING: 处理和分析数据
            - CONDITION_CHECK: 检查条件是否满足
            - API_CALL: 调用外部API接口
            - LLM_REASONING: 使用LLM进行推理分析
            - RESULT_SUMMARY: 汇总结果并生成最终回答
                
            请以JSON数组格式返回任务步骤列表，格式如下：
            [
              {
                "stepNumber": 1,
                "description": "步骤描述",
                "stepType": "TOOL_CALL",
                "resourceId": 工具ID(如果是工具调用),
                "resourceName": "资源名称",
                "estimatedDurationMs": 预估耗时(毫秒),
                "isRequired": true,
                "dependencies": []
              }
            ]
                
            注意：
            - dependencies 填写之前步骤的 stepNumber 数组，如 [1, 2]
            - 如果没有依赖，dependencies 为空数组 []
            - estimatedDurationMs 根据任务复杂度合理估算
            """.formatted(toolsDescription));
            
        return prompt.toString();
    }
    
    /**
     * 获取智能体的决策模型
     */
    private AiModel getDecisionModel(Agent agent) {
        // 1. 优先使用智能体的决策模型
        if (agent.getDecisionModelId() != null) {
            AiModel model = aiModelDao.getById(agent.getDecisionModelId());
            if (model != null) {
                log.debug("使用智能体决策模型: modelId={}", model.getId());
                return model;
            }
        }
        
        // 2. 其次使用智能体的回复模型
        if (agent.getResponseModelId() != null) {
            AiModel model = aiModelDao.getById(agent.getResponseModelId());
            if (model != null) {
                log.debug("使用智能体回复模型: modelId={}", model.getId());
                return model;
            }
        }
        
        // 3. 再次使用用户默认模型
        AiModel userDefaultModel = aiModelDao.getDefaultChatModel(agent.getUserId());
        if (userDefaultModel != null) {
            log.debug("使用用户默认模型: modelId={}", userDefaultModel.getId());
            return userDefaultModel;
        }
        
        // 4. 最后使用数据库中第一个CHAT模型
        AiModel firstModel = aiModelDao.getFirstChatModel();
        if (firstModel != null) {
            log.debug("使用数据库第一个模型: modelId={}", firstModel.getId());
            return firstModel;
        }
        
        log.warn("未找到任何可用模型");
        return null;
    }
    
    /**
     * 构建工具描述文本
     */
    private String buildToolsDescriptionForPlanning(List<Tools> tools) {
        if (tools == null || tools.isEmpty()) {
            return "无可用工具";
        }
        
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < tools.size(); i++) {
            Tools tool = tools.get(i);
            sb.append(String.format("%d. [ID:%d] %s - %s\n", 
                i + 1, 
                tool.getId(), 
                tool.getDisplayName(),
                tool.getDescription() != null ? tool.getDescription() : "无描述"
            ));
        }
        return sb.toString();
    }
    
    /**
     * 解析LLM返回的任务计划JSON
     */
    private List<TaskExecutionPlan.TaskStep> parseTaskPlanFromJson(
            String jsonResponse, 
            Agent agent,
            List<Tools> availableTools) {
        
        try {
            String cleanJson = jsonResponse.trim();
            if (cleanJson.startsWith("```")) {
                cleanJson = cleanJson.replaceAll("```json\\s*", "").replaceAll("```\\s*$", "").trim();
            }
            
            var rootNode = objectMapper.readTree(cleanJson);
            
            if (!rootNode.isArray()) {
                log.warn("LLM返回的不是JSON数组");
                return null;
            }
            
            List<TaskExecutionPlan.TaskStep> steps = new ArrayList<>();
            
            for (var node : rootNode) {
                try {
                    int stepNumber = node.has("stepNumber") ? node.get("stepNumber").asInt() : steps.size() + 1;
                    String description = node.has("description") ? node.get("description").asText() : "未命名步骤";
                    String stepType = node.has("stepType") ? node.get("stepType").asText() : "TOOL_CALL";
                    Long resourceId = node.has("resourceId") && !node.get("resourceId").isNull() ? 
                        node.get("resourceId").asLong() : null;
                    String resourceName = node.has("resourceName") ? node.get("resourceName").asText() : null;
                    long estimatedDurationMs = node.has("estimatedDurationMs") ? 
                        node.get("estimatedDurationMs").asLong() : 2000L;
                    boolean isRequired = node.has("isRequired") ? node.get("isRequired").asBoolean() : true;
                    
                    List<String> dependencies = new ArrayList<>();
                    if (node.has("dependencies") && node.get("dependencies").isArray()) {
                        for (var depNode : node.get("dependencies")) {
                            dependencies.add("step_" + depNode.asInt());
                        }
                    }
                    
                    steps.add(TaskExecutionPlan.TaskStep.builder()
                        .stepId("step_" + stepNumber)
                        .stepNumber(stepNumber)
                        .description(description)
                        .stepType(stepType)
                        .resourceId(resourceId)
                        .resourceName(resourceName)
                        .estimatedDurationMs(estimatedDurationMs)
                        .isRequired(isRequired)
                        .dependencies(dependencies)
                        .build()
                    );
                    
                } catch (Exception e) {
                    log.warn("解析单个步骤失败,跳过", e);
                }
            }
            
            return steps.isEmpty() ? null : steps;
            
        } catch (Exception e) {
            log.error("解析任务计划JSON失败", e);
            return null;
        }
    }
    
    /**
     * 创建 ChatModel 实例
     */
    private ChatModel createChatModel(AiModel model, Long userId) {
        // 获取模型提供商信息
        ModelProvider provider = modelProviderDao.getById(model.getProviderId());
        if (provider == null) {
            log.warn("模型提供商不存在: providerId={}", model.getProviderId());
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "模型配置错误");
        }
        
        log.debug("创建ChatModel: modelId={}, providerCode={}, modelName={}", 
                model.getId(), provider.getProviderCode(), model.getModelName());
        
        // 使用AiModelSupport根据provider类型创建不同的ChatModel
        return aiModelSupport.createChatModel(model, provider);
    }
    
    // ========== 辅助方法 ==========
    
    private List<Tools> findMatchingTools(String query, List<Tools> availableTools) {
        return availableTools.stream()
            .filter(tool -> {
                String toolName = tool.getDisplayName().toLowerCase();
                String toolCode = tool.getToolCodeName().toLowerCase();
                return query.contains(toolName) || query.contains(toolCode);
            })
            .toList();
    }
    
    private List<String> getPreviousStepIds(List<TaskExecutionPlan.TaskStep> steps) {
        return steps.stream()
            .map(TaskExecutionPlan.TaskStep::getStepId)
            .toList();
    }
    
    private TaskExecutionPlan.TaskStep buildToolCallStep(int stepNumber, Tools tool) {
        return TaskExecutionPlan.TaskStep.builder()
            .stepId("step_" + stepNumber)
            .stepNumber(stepNumber)
            .description("调用 " + tool.getDisplayName() + " 获取数据")
            .stepType("TOOL_CALL")
            .resourceId(tool.getId())
            .resourceName(tool.getDisplayName())
            .estimatedDurationMs(1500L)
            .isRequired(true)
            .dependencies(Collections.emptyList())
            .build();
    }
    
    private TaskExecutionPlan.TaskStep buildGenericQueryStep(int stepNumber) {
        return TaskExecutionPlan.TaskStep.builder()
            .stepId("step_" + stepNumber)
            .stepNumber(stepNumber)
            .description("执行数据查询")
            .stepType("DATA_QUERY")
            .estimatedDurationMs(2000L)
            .isRequired(true)
            .dependencies(Collections.emptyList())
            .build();
    }
}
