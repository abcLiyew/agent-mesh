package com.esdllm.agentmesh.service.unified.impl;

import com.esdllm.agentmesh.model.domain.Agent;
import com.esdllm.agentmesh.model.domain.AiModel;
import com.esdllm.agentmesh.model.domain.ModelProvider;
import com.esdllm.agentmesh.model.dto.DecisionExecutionResult;
import com.esdllm.agentmesh.repository.dao.AgentDao;
import com.esdllm.agentmesh.repository.dao.AiModelDao;
import com.esdllm.agentmesh.repository.dao.ModelProviderDao;
import com.esdllm.agentmesh.service.agent.support.AiModelSupport;
import com.esdllm.agentmesh.service.rag.RagRetrievalService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 简单任务处理器
 * 职责:处理简单任务(闲聊、简单问答),直接调用LLM生成回答
 */
@Component
@Slf4j
public class SimpleTaskHandler {
    
    @Resource
    private AgentDao agentDao;
    
    @Resource
    private AiModelDao aiModelDao;
    
    @Resource
    private AiModelSupport aiModelSupport;
    
    @Resource
    private ModelProviderDao modelProviderDao;
    
    @Resource()
    private RagRetrievalService ragRetrievalService;
    
    /**
     * 执行简单任务 - 直接调用LLM生成回答
     * 
     * @param query 用户查询
     * @param agentId 智能体ID
     * @param userId 用户ID
     * @return 执行结果
     */
    public DecisionExecutionResult executeSimpleTask(String query, Long agentId, Long userId) {
        log.info("=== 执行简单任务 === query: {}", query);
        
        long startTime = System.currentTimeMillis();
        
        try {
            String response = generateDirectResponse(query, agentId, userId);
            
            long executionTime = System.currentTimeMillis() - startTime;
            
            // 5. 构建结果
            DecisionExecutionResult result = new DecisionExecutionResult();
            result.setSuccess(true);
            result.setFinalResponse(response);
            result.setExecutionTimeMs(executionTime);
            
            log.info("=== 简单任务执行完成 === 耗时: {}ms", executionTime);
            
            return result;
            
        } catch (Exception e) {
            log.error("简单任务执行失败", e);
            
            DecisionExecutionResult result = new DecisionExecutionResult();
            result.setSuccess(false);
            result.setErrorMessage("执行失败: " + e.getMessage());
            result.setExecutionTimeMs(System.currentTimeMillis() - startTime);
            
            return result;
        }
    }
    
    /**
     * 直接生成回复（用于流式响应）
     */
    public String generateDirectResponse(String query, Long agentId, Long userId) {
        log.info("=== 直接生成简单任务回复 === query: {}, agentId: {}", query, agentId);
        
        try {
            // 1. 获取智能体配置
            Agent agent = null;
            if (agentId != null) {
                agent = agentDao.getById(agentId);
                if (agent == null) {
                    log.warn("智能体不存在，agentId: {}，将使用默认配置", agentId);
                }
            }
            
            // 2. 获取聊天模型（优先使用智能体配置的模型）
            AiModel aiModel = getChatModel(agent, userId);
            
            if (aiModel == null) {
                throw new RuntimeException("未找到可用的聊天模型");
            }
            
            // 3. 获取模型提供商
            ModelProvider provider = modelProviderDao.getById(aiModel.getProviderId());
            if (provider == null) {
                throw new RuntimeException("模型提供商不存在");
            }
            
            // 4. 创建ChatClient
            var chatModel = aiModelSupport.createChatModel(aiModel, provider);
            ChatClient chatClient = ChatClient.builder(chatModel).build();
            
            // 5. RAG检索（如果智能体有关联的知识库）
            String ragContext = "";
            if (agent != null && agent.getId() != null) {
                ragContext = retrieveKnowledgeContext(agent.getId(), query);
            }
            
            // 6. 构建系统Prompt（使用智能体的配置 + RAG上下文）
            String systemPrompt = buildSystemPrompt(agent, query, ragContext);
            
            // 7. 调用LLM
            String response = chatClient.prompt(new Prompt(query))
                .system(systemPrompt)
                .call()
                .content();
            
            log.info("✅ LLM回复生成成功，长度: {}", response != null ? response.length() : 0);
            
            return response != null ? response : "抱歉，我暂时无法回答这个问题。";
            
        } catch (Exception e) {
            log.error("生成回复失败", e);
            return "抱歉，处理您的请求时出现了错误。";
        }
    }
    
    /**
     * 获取聊天模型（优先使用智能体配置的模型）
     */
    private AiModel getChatModel(Agent agent, Long userId) {
        // 如果智能体配置了response_model_id，优先使用
        if (agent != null && agent.getResponseModelId() != null) {
            AiModel model = aiModelDao.getById(agent.getResponseModelId());
            if (model != null && Boolean.TRUE.equals(model.getIsActive()) && model.getIsDelete() == 0) {
                log.debug("使用智能体配置的回复模型: {}", model.getModelName());
                return model;
            }
        }
        
        // 否则使用用户的默认聊天模型
        log.debug("使用用户默认聊天模型");
        return aiModelDao.getDefaultChatModel(userId);
    }
    
    /**
     * 检索知识库上下文
     */
    private String retrieveKnowledgeContext(Long agentId, String query) {
        if (ragRetrievalService == null) {
            log.debug("RAG服务未配置，跳过知识库检索");
            return "";
        }
        
        try {
            // TODO: 这里需要根据 agentId 查询关联的知识库ID列表
            // 暂时假设有一个方法可以获取
            // List<Long> kbIds = getAgentKnowledgeBaseIds(agentId);
            
            // 临时方案：先不检索，后续完善
            log.debug("知识库检索功能待完善，agentId: {}", agentId);
            return "";
            
        } catch (Exception e) {
            log.warn("知识库检索失败，继续执行", e);
            return "";
        }
    }
    
    /**
     * 构建系统Prompt（使用智能体的配置）
     */
    private String buildSystemPrompt(Agent agent, String query, String ragContext) {
        StringBuilder prompt = new StringBuilder();
        
        // 1. 使用智能体的 system_prompt（如果有）
        if (agent != null && agent.getSystemPrompt() != null && !agent.getSystemPrompt().isEmpty()) {
            prompt.append(agent.getSystemPrompt());
            prompt.append("\n\n");
        } else {
            // 默认系统提示词
            prompt.append("你是一个智能助手，请简洁、准确地回答用户的问题。\n\n");
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
        
        // 4. 添加RAG检索到的知识（如果有）
        if (ragContext != null && !ragContext.isEmpty()) {
            prompt.append("参考信息：\n");
            prompt.append(ragContext);
            prompt.append("\n\n请结合上述参考信息回答问题。如果参考信息与问题无关，请忽略。\n\n");
        }
        
        // 5. 添加通用要求
        prompt.append("""
            回答要求：
            1. 如果是闲聊，友好回应
            2. 如果是简单问题，直接给出答案
            3. 保持回答简洁明了
            4. 不要编造不确定的信息
            5. 根据你的角色定位和背景信息来回答问题
            """);
        
        return prompt.toString();
    }
}
