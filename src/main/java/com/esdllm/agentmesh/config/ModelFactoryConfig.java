package com.esdllm.agentmesh.config;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.ollama.api.OllamaApi;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

@Configuration
@Slf4j
public class ModelFactoryConfig {

    @Value("${model.provider.base-url:}")
    private String baseUrl;

    @Value("${model.provider.api-key:}")
    private String apiKey;

    @Value("${model.provider.type:}")
    private String providerType;
    @Resource
    private RestClient.Builder customRestClientBuilder;

    /**
     * 创建通用的 ChatModel Bean
     * 支持 OpenAI 兼容接口（包括 DashScope、Ollama 等）
     */
    @Bean
    public ChatModel chatModel() {
        // 如果没有指定 providerType，根据 baseUrl 自动判断
        String effectiveProviderType = providerType;
        if (!StringUtils.hasText(effectiveProviderType)) {
            if (StringUtils.hasText(baseUrl) && baseUrl.contains("ollama")) {
                effectiveProviderType = "ollama";
            } else {
                effectiveProviderType = "openai";
            }
        }

        // 如果没有配置 baseUrl，使用默认 Ollama 地址
        if (!StringUtils.hasText(baseUrl)) {
            log.warn("未配置 model.provider.base-url，将使用默认 Ollama 地址");
            baseUrl = "http://localhost:11434";
            effectiveProviderType = "ollama";
        }

        if ("ollama".equalsIgnoreCase(effectiveProviderType)) {
            // Ollama 模式（不需要 apiKey）

            OllamaApi ollamaApi = OllamaApi.builder()
                    .baseUrl(baseUrl)
                    .build();
            return OllamaChatModel.builder().ollamaApi(ollamaApi).build();
        } else {
            // OpenAI 兼容模式（包括 DashScope、自定义 OpenAI 接口等）
            // 如果是 OpenAI 兼容模式但没有 apiKey，降级到 Ollama 模式
            if (!StringUtils.hasText(apiKey)) {
                log.warn("未配置 model.provider.api-key，降级到 Ollama 模式");

                OllamaApi ollamaApi = OllamaApi.builder()
                        .baseUrl(baseUrl)
                        .build();
                return OllamaChatModel.builder().ollamaApi(ollamaApi).build();
            }

            OpenAiApi openAiApi = OpenAiApi.builder()
                    .baseUrl(baseUrl)
                    .apiKey(apiKey)
                    .restClientBuilder(customRestClientBuilder)
                    .build();

            return OpenAiChatModel.builder()
                    .openAiApi(openAiApi)
                    .defaultOptions(OpenAiChatOptions.builder().build())
                    .build();
        }
    }
}
