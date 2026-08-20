package com.esdllm.agentmesh.service.agent.impl;

import cn.hutool.core.util.StrUtil;
import com.esdllm.agentmesh.common.ErrorCode;
import com.esdllm.agentmesh.exception.BusinessException;
import com.esdllm.agentmesh.model.domain.*;
import com.esdllm.agentmesh.model.dto.IntentRecognitionResult;
import com.esdllm.agentmesh.model.dto.tool.ToolSchemaConfig;
import com.esdllm.agentmesh.repository.dao.AgentDao;
import com.esdllm.agentmesh.repository.dao.AgentKbRelationDao;
import com.esdllm.agentmesh.repository.dao.AiModelDao;
import com.esdllm.agentmesh.repository.dao.KnowledgeBaseDao;
import com.esdllm.agentmesh.repository.dao.ModelProviderDao;
import com.esdllm.agentmesh.repository.dao.ToolsDao;
import com.esdllm.agentmesh.service.IntentRecognitionService;
import com.esdllm.agentmesh.util.EncryptionUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.ollama.api.OllamaApi;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 意图识别服务实现类
 * 使用 LLM 进行意图识别和参数提取
 */
@Service
@Slf4j
public class IntentRecognitionServiceImpl implements IntentRecognitionService {

    @Resource
    private AiModelDao aiModelDao;

    @Resource
    private ModelProviderDao modelProviderDao;

    @Resource
    private ToolsDao toolsDao;

    @Resource
    private AgentDao agentDao;

    @Resource
    private KnowledgeBaseDao knowledgeBaseDao;
    
    @Resource
    private AgentKbRelationDao agentKbRelationDao;

    @Resource
    private ObjectMapper objectMapper;

    @Resource
    private RestClient.Builder customRestClientBuilder;

    /**
     * 意图样本缓存（用于简单匹配优化）
     */
    private final Map<String, String> sampleCache = new ConcurrentHashMap<>();

    @Override
    public IntentRecognitionResult recognizeIntent(String query, Long userId) {
        if (StrUtil.isBlank(query)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户问题不能为空");
        }

        log.info("开始识别意图，query: {}", query);

        // 1. 尝试基于关键词的简单匹配（快速路径）
        IntentRecognitionResult simpleResult= simpleMatch(query);
        if (simpleResult != null) {
            log.info("关键词匹配成功，intent: {}", simpleResult.getIntentType());
            return simpleResult;
        }

        // 2. 使用 LLM 进行深度意图识别
        IntentRecognitionResult llmResult= llmRecognize(query, userId);

        log.info("LLM 识别成功，intent: {}, confidence: {}",
                llmResult.getIntentType(), llmResult.getConfidence());

        return llmResult;
    }

    /**
     * 带智能体上下文的意图识别
     */
    public IntentRecognitionResult recognizeIntentWithContext(String query, Long agentId, Long userId) {
        if (StrUtil.isBlank(query)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户问题不能为空");
        }

        log.info("开始识别意图（带智能体上下文），agentId: {}, query: {}", agentId, query);

        // 1. 获取智能体信息
        Agent agent = agentDao.getById(agentId);
        if (agent == null) {
            throw new BusinessException(ErrorCode.AGENT_NOT_FOUND, "智能体不存在");
        }

        // 2. 获取智能体的工具配置
        List<Tools> agentTools = getAgentTools(agent, userId);

        // 3. 获取智能体关联的知识库
        List<KnowledgeBase> agentKbs = getAgentKnowledgeBases(agentId, userId);

        // 4. 使用 LLM 进行深度意图识别（带上下文）
        IntentRecognitionResult result = llmRecognizeWithContext(query, agent, agentTools, agentKbs, userId);

        log.info("意图识别成功，intent: {}, confidence: {}, tools: {}, kbs: {}",
                result.getIntentType(), result.getConfidence(),
                result.getMatchedToolIds() != null ? result.getMatchedToolIds().size() : 0,
                result.getMatchedKbIds() != null ? result.getMatchedKbIds().size() : 0);

        return result;
    }

    @Override
    public List<IntentRecognitionResult> batchRecognize(List<String> queries, Long userId) {
        return queries.stream()
                .map(query -> recognizeIntent(query, userId))
                .toList();
    }

    @Override
    public void addTrainingSample(String query, String intentType, Long userId) {
        sampleCache.put(query.toLowerCase(), intentType);
        log.info("添加训练样本，query: {}, intent: {}", query, intentType);
    }

    /**
     * 简单的关键词匹配（快速路径）
     */
    private IntentRecognitionResult simpleMatch(String query) {
        String lowerQuery= query.toLowerCase();

        // 产品查询关键词
        if (lowerQuery.contains("产品") || lowerQuery.contains("商品") ||
                lowerQuery.contains("价格") || lowerQuery.contains("库存")) {
            IntentRecognitionResult result = new IntentRecognitionResult();
            result.setIntentType("PRODUCT_QUERY");
            result.setConfidence(new BigDecimal("0.7"));
            result.setOriginalQuery(query);
            result.setNeedToolCall(true);

            // 自动匹配产品相关工具
            List<Tools> productTools= toolsDao.getProductTools();
            if (!productTools.isEmpty()) {
                result.setMatchedToolIds(productTools.stream()
                        .map(Tools::getId)
                        .toList());
            }

            return result;
        }

        // 订单查询关键词
        if (lowerQuery.contains("订单") || lowerQuery.contains("物流") ||
                lowerQuery.contains("发货")) {
            IntentRecognitionResult result = new IntentRecognitionResult();
            result.setIntentType("ORDER_QUERY");
            result.setConfidence(new BigDecimal("0.7"));
            result.setOriginalQuery(query);
            result.setNeedToolCall(true);
            return result;
        }

        // 闲聊判断
        if (lowerQuery.contains("你好") || lowerQuery.contains("谢谢") ||
                lowerQuery.contains("再见")) {
            IntentRecognitionResult result = new IntentRecognitionResult();
            result.setIntentType("CHAT");
            result.setConfidence(new BigDecimal("0.9"));
            result.setOriginalQuery(query);
            result.setNeedToolCall(false);
            return result;
        }

        return null;
    }

    /**
     * 使用 LLM 进行深度意图识别
     */
    private IntentRecognitionResult llmRecognize(String query, Long userId) {
        try {
            // 获取默认的智能模型
            AiModel defaultModel= aiModelDao.getDefaultChatModel(userId);
            if (defaultModel == null) {
                log.warn("未找到默认聊天模型，使用简单匹配");
                IntentRecognitionResult result = new IntentRecognitionResult();
                result.setIntentType("UNKNOWN");
                result.setConfidence(new BigDecimal("0.5"));
                result.setOriginalQuery(query);
                return result;
            }

            // 构建系统 Prompt
            String systemPrompt= """
                你是一个专业的意图识别助手。请分析用户问题，完成以下任务：
             
             1. 判断意图类型（从以下选择）：
                   - PRODUCT_QUERY: 产品/商品相关查询
                   - ORDER_QUERY: 订单/物流相关查询
                   - KNOWLEDGE_QA: 知识问答
                   - TOOL_CALL: 需要调用工具
                   - AGENT_CALL: 需要调用其他智能体
                   - CHAT: 闲聊对话
                   - UNKNOWN: 未知意图
             
               2. 提取关键参数（如产品名、订单号等）
             
                3. 判断是否需要调用外部工具
             
                请以 JSON 格式返回，格式如下：
                {
                  "intent_type": "意图类型",
                  "confidence": 0.0-1.0,
                  "parameters": {},
                  "need_tool_call": true/false
                }
             """;

            String userPrompt= "用户问题：" + query;

            // 调用 LLM 获取意图识别结果
            ChatModel chatModel = createChatModel(defaultModel, userId);
            ChatClient chatClient = ChatClient.builder(chatModel).build();

            String response = chatClient.prompt(new Prompt(userPrompt))
                    .system(systemPrompt)
                    .call()
                    .content();

            if (response == null || response.trim().isEmpty()) {
                log.warn("LLM 返回空响应，使用默认结果");
                IntentRecognitionResult result = new IntentRecognitionResult();
                result.setIntentType("UNKNOWN");
                result.setConfidence(new BigDecimal("0.3"));
                result.setOriginalQuery(query);
                return result;
            }

            log.info("LLM 意图识别响应：{}", response);

            // 解析 JSON 响应
            return parseJsonResponse(response, query);

        } catch (Exception e) {
            log.error("LLM 意图识别失败", e);
            IntentRecognitionResult result = new IntentRecognitionResult();
            result.setIntentType("UNKNOWN");
            result.setConfidence(new BigDecimal("0.3"));
            result.setOriginalQuery(query);
            result.setNeedToolCall(false);
            return result;
        }
    }

    /**
     * 使用 LLM 进行深度意图识别（带智能体上下文）
     */
    private IntentRecognitionResult llmRecognizeWithContext(
            String query,
            Agent agent,
            List<Tools> availableTools,
            List<KnowledgeBase> availableKbs,
            Long userId
    ) {
        try {
            // 优先使用智能体的决策模型
            AiModel decisionModel = null;
            if (agent.getDecisionModelId() != null) {
                decisionModel = aiModelDao.getById(agent.getDecisionModelId());
            }

            // 如果没有决策模型，使用回复模型
            if (decisionModel == null && agent.getResponseModelId() != null) {
                decisionModel = aiModelDao.getById(agent.getResponseModelId());
            }

            // 如果都没有，使用默认模型
            if (decisionModel == null) {
                decisionModel = aiModelDao.getDefaultChatModel(userId);
            }

            if (decisionModel == null) {
                log.warn("未找到可用模型，使用简单匹配");
                IntentRecognitionResult result = new IntentRecognitionResult();
                result.setIntentType("UNKNOWN");
                result.setConfidence(new BigDecimal("0.5"));
                result.setOriginalQuery(query);
                return result;
            }

            // 构建工具列表描述
            String toolsDescription = buildToolsDescription(availableTools);

            // 构建知识库列表描述
            String kbsDescription = buildKnowledgeBasesDescription(availableKbs);

            // 构建增强的 Prompt
            String systemPrompt= """
                你是一个专业的意图识别助手。请分析用户问题，完成以下任务：
                
                1. 判断意图类型（从以下选择）：
                   - PRODUCT_QUERY: 产品/商品相关查询
                   - ORDER_QUERY: 订单/物流相关查询
                   - KNOWLEDGE_QA: 知识问答（需要检索知识库）
                   - TOOL_CALL: 需要调用工具
                   - AGENT_CALL: 需要调用其他智能体
                   - CHAT: 闲聊对话
                   - UNKNOWN: 未知意图
                
                2. 提取关键参数（如产品名、订单号等）
                
                3. 根据可用工具和知识库，判断需要调用哪些资源：
                   - 如果需要调用工具，从可用工具列表中选择合适的工具 ID
                   - 如果需要检索知识，从可用知识库中选择合适的知识库 ID
                
                可用工具列表：
                %s
                
                可用知识库列表：
                %s
                
                请以 JSON 格式返回，格式如下：
                {
                  "intent_type": "意图类型",
                  "confidence": 0.0-1.0,
                  "parameters": {},
                  "need_tool_call": true/false,
                  "matched_tool_ids": [1, 2],
                  "matched_kb_ids": [3]
                }
                """.formatted(
                    StrUtil.isNotBlank(toolsDescription) ? toolsDescription : "无可用工具",
                    StrUtil.isNotBlank(kbsDescription) ? kbsDescription : "无可用知识库"
            );

            String userPrompt= "用户问题：" + query;

            // 调用 LLM 获取意图识别结果
            ChatModel chatModel = createChatModel(decisionModel, userId);
            ChatClient chatClient = ChatClient.builder(chatModel).build();

            String response = chatClient.prompt(new Prompt(userPrompt))
                    .system(systemPrompt)
                    .call()
                    .content();

            if (response == null || response.trim().isEmpty()) {
                log.warn("LLM 返回空响应（带上下文），使用默认结果");
                IntentRecognitionResult result = new IntentRecognitionResult();
                result.setIntentType("UNKNOWN");
                result.setConfidence(new BigDecimal("0.3"));
                result.setOriginalQuery(query);
                return result;
            }

            log.info("LLM 意图识别响应（带上下文）: {}", response);

            // 解析 JSON 响应
            return parseJsonResponseWithContext(response, query);

        } catch (Exception e) {
            log.error("LLM 意图识别失败（带上下文）", e);
            IntentRecognitionResult result = new IntentRecognitionResult();
            result.setIntentType("UNKNOWN");
            result.setConfidence(new BigDecimal("0.3"));
            result.setOriginalQuery(query);
            result.setNeedToolCall(false);
            return result;
        }
    }

    /**
     * 构建工具描述文本
     */
    private String buildToolsDescription(List<Tools> tools) {
        if (tools.isEmpty()) {
            return "";
        }

        return tools.stream()
                .map(tool -> String.format("- ID: %d, 名称：%s, 描述：%s",
                        tool.getId(),
                        tool.getDisplayName(),
                        tool.getDescription()))
                .collect(Collectors.joining("\n"));
    }

    /**
     * 构建知识库描述文本
     */
    private String buildKnowledgeBasesDescription(List<KnowledgeBase> kbs) {
        if (kbs.isEmpty()) {
            return "";
        }

        return kbs.stream()
                .map(kb -> String.format("- ID: %d, 名称：%s, 描述：%s",
                        kb.getId(),
                        kb.getName(),
                        kb.getDescription()))
                .collect(Collectors.joining("\n"));
    }

    /**
     * 解析 LLM 返回的 JSON 响应
     */
    private IntentRecognitionResult parseJsonResponse(String jsonResponse, String originalQuery) {
        try {
            // 清理响应文本（去除 Markdown 代码块标记）
            String cleanJson= jsonResponse.replaceAll("```json *","").replace("```\\s*", "")
                    .trim();

            JsonNode rootNode = objectMapper.readTree(cleanJson);

            IntentRecognitionResult result = new IntentRecognitionResult();
            result.setIntentType(rootNode.path("intent_type").asText("UNKNOWN"));
            result.setConfidence(new BigDecimal(String.valueOf(
                    rootNode.path("confidence").asDouble(0.5)
            )));
            result.setNeedToolCall(rootNode.path("need_tool_call").asBoolean(false));
            result.setOriginalQuery(originalQuery);

            // 解析参数
            if (rootNode.has("parameters")) {
                Map<String, Object> parameters = objectMapper.convertValue(
                        rootNode.get("parameters"),
                        new com.fasterxml.jackson.core.type.TypeReference<>() {
                        }
                );
                result.setParameters(parameters);
            }

            log.info("意图识别成功：type={}, confidence={}, needToolCall={}",
                    result.getIntentType(), result.getConfidence(), result.getNeedToolCall());

            return result;

        } catch (Exception e) {
            log.error("解析意图识别响应失败", e);
            IntentRecognitionResult result = new IntentRecognitionResult();
            result.setIntentType("UNKNOWN");
            result.setConfidence(new BigDecimal("0.3"));
            result.setOriginalQuery(originalQuery);
            result.setNeedToolCall(false);
            return result;
        }
    }

    /**
     * 解析 LLM 返回的 JSON 响应（带上下文）
     */
    private IntentRecognitionResult parseJsonResponseWithContext(String jsonResponse, String originalQuery) {
        try {
            // 清理响应文本（去除 Markdown 代码块标记）
            String cleanJson= jsonResponse.replaceAll("```json *","").replace("```\\s*", "")
                    .trim();

            JsonNode rootNode = objectMapper.readTree(cleanJson);

            IntentRecognitionResult result = new IntentRecognitionResult();
            result.setIntentType(rootNode.path("intent_type").asText("UNKNOWN"));
            result.setConfidence(new BigDecimal(String.valueOf(
                    rootNode.path("confidence").asDouble(0.5)
            )));
            result.setNeedToolCall(rootNode.path("need_tool_call").asBoolean(false));
            result.setOriginalQuery(originalQuery);

            // 解析参数
            if (rootNode.has("parameters")) {
                Map<String, Object> parameters = objectMapper.convertValue(
                        rootNode.get("parameters"),
                        new com.fasterxml.jackson.core.type.TypeReference<>() {
                        }
                );
                result.setParameters(parameters);
            }

            // 解析匹配的工具 ID 列表
            if (rootNode.has("matched_tool_ids")) {
                JsonNode toolIdsNode = rootNode.get("matched_tool_ids");
                if (toolIdsNode.isArray()) {
                    List<Long> toolIds = new ArrayList<>();
                    for (JsonNode idNode : toolIdsNode) {
                        toolIds.add(idNode.asLong());
                    }
                    result.setMatchedToolIds(toolIds);
                }
            }

            // 解析匹配的知识库 ID 列表
            if (rootNode.has("matched_kb_ids")) {
                JsonNode kbIdsNode = rootNode.get("matched_kb_ids");
                if (kbIdsNode.isArray()) {
                    List<Long> kbIds = new ArrayList<>();
                    for (JsonNode idNode : kbIdsNode) {
                        kbIds.add(idNode.asLong());
                    }
                    result.setMatchedKbIds(kbIds);
                }
            }

            log.info("意图识别成功（带上下文）: type={}, confidence={}, tools={}, kbs={}",
                    result.getIntentType(), result.getConfidence(),
                    result.getMatchedToolIds(), result.getMatchedKbIds());

            return result;

        } catch (Exception e) {
            log.error("解析意图识别响应失败（带上下文）", e);
            IntentRecognitionResult result = new IntentRecognitionResult();
            result.setIntentType("UNKNOWN");
            result.setConfidence(new BigDecimal("0.3"));
            result.setOriginalQuery(originalQuery);
            result.setNeedToolCall(false);
            return result;
        }
    }

    /**
     * 获取智能体可用的工具列表
     */
    private List<Tools> getAgentTools(Agent agent, Long userId) {

        // 1. 获取系统工具
        List<Tools> systemTools= toolsDao.getSystemTools();
        List<Tools> allTools = new ArrayList<>(systemTools);

        // 2. 获取用户自定义工具
        List<Tools> userTools= toolsDao.getUserTools(userId);
        allTools.addAll(userTools);

        // 3. 如果智能体有 toolSchemaJson 配置，从中提取工具
        if (agent.getIsToolEnabled() && agent.getToolSchemaJson() != null) {
            try {
                ToolSchemaConfig config = objectMapper.convertValue(
                    agent.getToolSchemaJson(), 
                    ToolSchemaConfig.class
                );
                if (config.getTools() != null) {
                    // 这里可以根据 config.getTools() 中的工具名称过滤 allTools
                    // 或者加载特定的工具配置
                    log.info("智能体启用了工具配置，工具数量：{}", config.getTools().size());
                }
            } catch (Exception e) {
                log.warn("解析智能体工具配置失败", e);
            }
        }

        // 4. 只返回启用的工具
        return allTools.stream()
                .filter(Tools::getIsEnabled)
                .filter(tool -> tool.getIsDelete() == 0)
                .toList();
    }

    /**
     * 获取智能体关联的知识库列表
     */
    private List<KnowledgeBase> getAgentKnowledgeBases(Long agentId, Long userId) {
        // 通过 AgentKbRelation 查询关联的知识库
        List<AgentKbRelation> relations = agentKbRelationDao.getByAgentId(agentId);
        
        if (relations.isEmpty()) {
            log.warn("智能体 {} 未关联任何知识库", agentId);
            return Collections.emptyList();
        }
        
        // 提取知识库 ID 列表
        List<Long> kbIds = relations.stream()
                .map(AgentKbRelation::getKbId)
                .filter(Objects::nonNull)
                .toList();
        
        if (kbIds.isEmpty()) {
            return Collections.emptyList();
        }
        
        // 批量查询知识库信息
        List<KnowledgeBase> knowledgeBases = knowledgeBaseDao.listByIds(kbIds);
        
        // 过滤：只返回已发布且未删除的知识库
        return knowledgeBases.stream()
                .filter(kb -> kb.getStatus() == 1 && kb.getIsDelete() == 0)
                .sorted((kb1, kb2) -> {
                    // 按照关联关系中的排序顺序排序
                    int order1 = relations.stream()
                            .filter(r -> r.getKbId().equals(kb1.getId()))
                            .findFirst()
                            .map(AgentKbRelation::getSortOrder)
                            .orElse(Integer.MAX_VALUE);
                    int order2 = relations.stream()
                            .filter(r -> r.getKbId().equals(kb2.getId()))
                            .findFirst()
                            .map(AgentKbRelation::getSortOrder)
                            .orElse(Integer.MAX_VALUE);
                    return Integer.compare(order1, order2);
                })
                .toList();
    }

    /**
     * 根据 AiModel 动态创建 ChatModel
     * 支持 OpenAI 兼容接口（包括 DashScope、Ollama 等）
     */
    private ChatModel createChatModel(AiModel aiModel, Long userId) {
        try {
            // 获取模型对应的提供商配置
            ModelProvider provider= modelProviderDao.getById(aiModel.getProviderId());
            if (provider == null) {
                throw new BusinessException(ErrorCode.PROVIDER_NOT_FOUND,
                        "模型提供商不存在：providerId=" + aiModel.getProviderId());
            }

            // 检查提供商状态
            if (provider.getStatus() != 1) {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR,
                        "模型提供商已禁用：" + provider.getProviderName());
            }

            // 解密 API Key
            String apiKey = decryptApiKey(provider.getApiKeyEncrypted());
            String baseUrl = provider.getBaseUrl();
            String providerCode = provider.getProviderCode();

            log.info("创建 ChatModel，provider: {}, model: {}, baseUrl: {}",
                    provider.getProviderName(), aiModel.getModelName(), baseUrl);

            // 根据提供商类型创建对应的 ChatModel
            if ("ollama".equalsIgnoreCase(providerCode)) {
                // Ollama 模式 - 使用系统默认的 ChatModel Bean
                // 注意：Ollama 通常不需要复杂的配置，模型名称可以在调用时指定
                OllamaApi ollamaApi = OllamaApi.builder()
                        .baseUrl(baseUrl)
                        .restClientBuilder(customRestClientBuilder)
                        .build();
                return OllamaChatModel.builder().ollamaApi(ollamaApi).build();

            }  else {
                // OpenAI 兼容模式（包括 DashScope、自定义 OpenAI 接口等）
                OpenAiApi openAiApi = OpenAiApi.builder()
                        .baseUrl(baseUrl)
                        .apiKey(apiKey)
                        .restClientBuilder(customRestClientBuilder)
                        .build();

                OpenAiChatOptions options = OpenAiChatOptions.builder()
                        .model(aiModel.getModelName())
                        .temperature(0.7)
                        .maxTokens(aiModel.getMaxTokens())
                        .build();

                return OpenAiChatModel.builder()
                        .openAiApi(openAiApi)
                        .defaultOptions(options)
                        .build();
            }

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("创建 ChatModel 失败", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR,
                    "创建 ChatModel 失败：" + e.getMessage());
        }
    }
    
    /**
     * 解密 API Key
     * 生产环境建议使用更安全的加密方式（如 AES-256）
     */
    private String decryptApiKey(String encryptedKey) {
        if (encryptedKey == null || encryptedKey.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "API Key 不能为空");
        }
        
        // 使用 AES 解密
        try {
            // 如果是 Base64 编码，尝试使用 AES 解密
            if (EncryptionUtil.isBase64(encryptedKey)) {
                String decrypted = EncryptionUtil.decrypt(encryptedKey);
                log.debug("API Key 使用 AES 解密成功");
                return decrypted;
            } else {
                // 如果不是 Base64 编码，可能是明文（仅开发环境）
                log.warn("API Key 未加密，建议在生产环境中使用加密存储");
                return encryptedKey;
            }
        } catch (Exception e) {
            log.error("API Key 解密失败，将尝试使用原文", e);
            // 如果解密失败，返回原文（兼容未加密的情况）
            return encryptedKey;
        }
    }
    
    /**
     * 加密 API Key（用于保存时）
     */
    public String encryptApiKey(String plainApiKey) {
        if (plainApiKey == null || plainApiKey.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "API Key 不能为空");
        }
        
        try {
            String encrypted = EncryptionUtil.encrypt(plainApiKey);
            log.debug("API Key 加密成功");
            return encrypted;
        } catch (Exception e) {
            log.error("API Key 加密失败", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "API Key 加密失败：" + e.getMessage());
        }
    }
}
