package com.esdllm.agentmesh.service.agent.support;

import com.esdllm.agentmesh.model.domain.Agent;
import com.esdllm.agentmesh.model.domain.AiModel;
import com.esdllm.agentmesh.model.domain.ModelProvider;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.ollama.api.OllamaApi;
import org.springframework.ai.ollama.api.OllamaChatOptions;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import reactor.core.publisher.Flux;

/**
 * AI 模型调用辅助类：封装 ChatModel 创建和 Options 构建
 */
@Component
@Slf4j
public class AiModelSupport {
    @Resource
    private RestClient.Builder customRestClientBuilder;

    /**
     * 创建 ChatModel 实例
     */
    public ChatModel createChatModel(AiModel aiModel, ModelProvider provider) {
        String providerCode = provider.getProviderCode();
        String baseUrl = provider.getBaseUrl();
        String apiKey = provider.getApiKeyEncrypted();

        log.info("创建 ChatModel - providerCode: {}, baseUrl: {}, modelName: {}", 
                providerCode, baseUrl, aiModel.getModelName());

        if ("ollama".equalsIgnoreCase(providerCode)) {
            if (baseUrl == null || baseUrl.trim().isEmpty()) {
                log.warn("数据库中的 base_url 为空，使用默认地址：http://localhost:11434");
                baseUrl = "http://localhost:11434";
            }
            

            // 使用自定义的 WebClient Builder 创建 OllamaApi
            OllamaApi ollamaApi = OllamaApi.builder()
                    .baseUrl(baseUrl)
                    .restClientBuilder(customRestClientBuilder)
                    .build();
            
            return OllamaChatModel.builder()
                    .ollamaApi(ollamaApi)
                    .defaultOptions(OllamaChatOptions.builder()
                            .model(aiModel.getModelName())
                            .temperature(0.7)
                            .build())
                    .build();
        } else {
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
    }
    
    /**
     * 创建 ChatClient 实例（便捷方法）
     */
    public ChatClient createChatClient(AiModel aiModel, ModelProvider provider) {
        ChatModel chatModel = createChatModel(aiModel, provider);
        return ChatClient.builder(chatModel).build();
    }

    /**
     * 构建 ChatOptions
     */
    public ChatOptions buildModelOptions(AiModel aiModel) {
        OpenAiChatOptions.Builder optionsBuilder = OpenAiChatOptions.builder()
                .model(aiModel.getModelName())
                .temperature(0.7);
        
        if (aiModel.getMaxTokens() != null) {
            optionsBuilder.maxTokens(aiModel.getMaxTokens());
        }
        
        return optionsBuilder.build();
    }

    /**
     * 调用 LLM 生成回答（非流式）
     */
    public String callChatModel(ChatModel chatModel, String query, String toolResponse, Agent agent) {
        ChatClient chatClient = ChatClient.builder(chatModel).build();
        String promptText = buildPrompt(query, toolResponse, agent);

        return chatClient.prompt()
                .user(promptText)
                .options(buildModelOptionsForCall())
                .call()
                .content();
    }

    /**
     * 调用 LLM 生成回答（流式 Flux）
     */
    public Flux<String> streamChatModel(ChatModel chatModel, String query, String toolResponse, Agent agent) {
        ChatClient chatClient = ChatClient.builder(chatModel).build();
        String promptText = buildPrompt(query, toolResponse, agent);

        return chatClient.prompt()
                .user(promptText)
                .options(buildModelOptionsForCall())
                .stream()
                .content();
    }

    /**
     * 估算 token 数量（粗略估计）
     */
    public int estimateTokenCount(String text) {
        if (text == null || text.isEmpty()) {
            return 0;
        }
        return text.length() / 4;
    }

    // ==================== 私有辅助方法 ====================

    private String buildPrompt(String query, String toolResponse, Agent agent) {
        StringBuilder prompt = new StringBuilder();

        if (agent.getSystemPrompt() != null && !agent.getSystemPrompt().isEmpty()) {
            prompt.append("系统指令：").append(agent.getSystemPrompt()).append("\n\n");
        }

        if (toolResponse != null && !toolResponse.isEmpty()) {
            prompt.append("以下是查询到的信息：\n");
            prompt.append(toolResponse).append("\n\n");
            prompt.append("请基于以上信息，用专业、友好的语气回答用户的问题。\n\n");
        }

        prompt.append("用户问题：").append(query);

        return prompt.toString();
    }

    private ChatOptions buildModelOptionsForCall() {
        // 构建默认的 ChatOptions，避免返回 null
        return OpenAiChatOptions.builder()
                .temperature(0.7)
                .build();
    }
}
