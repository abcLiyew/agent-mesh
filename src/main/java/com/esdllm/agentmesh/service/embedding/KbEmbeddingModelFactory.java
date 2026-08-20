package com.esdllm.agentmesh.service.embedding;

import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import com.alibaba.cloud.ai.dashscope.embedding.DashScopeEmbeddingModel;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import io.micrometer.observation.ObservationRegistry;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.ollama.OllamaEmbeddingModel;
import org.springframework.ai.ollama.api.OllamaApi;
import org.springframework.ai.ollama.api.OllamaEmbeddingOptions;
import org.springframework.ai.ollama.management.ModelManagementOptions;
import org.springframework.ai.openai.OpenAiEmbeddingModel;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.stereotype.Component;
import org.springframework.util.MultiValueMap;

import java.util.concurrent.TimeUnit;

@Component
public class KbEmbeddingModelFactory {

    public record KbEmbeddingConfig(
            String baseUrl,
            String provider,
            String apiKey,
            String modelName,
            MultiValueMap<String, String> headers
    ) {}

    private final LoadingCache<KbEmbeddingConfig, EmbeddingModel> modelCache = Caffeine.newBuilder()
            .maximumSize(1000)
            .expireAfterAccess(60, TimeUnit.MINUTES)
            .build(this::createEmbeddingModel);

    public EmbeddingModel getEmbeddingModel(KbEmbeddingConfig config) {
        return modelCache.get(config);
    }

    public void invalidateCache(KbEmbeddingConfig config) {
        modelCache.invalidate(config);
    }

    public void invalidateAll() {
        modelCache.invalidateAll();
    }

    /**
     * 创建 OpenAI 兼容的嵌入模型
     */
    public EmbeddingModel createOpenAiModel(String apiKey, String modelName) {
        return createOpenAiModel(apiKey, modelName, "https://api.openai.com");
    }

    /**
     * 创建 OpenAI 兼容的嵌入模型（自定义 baseUrl）
     */
    public EmbeddingModel createOpenAiModel(String apiKey, String modelName, String baseUrl) {
        KbEmbeddingConfig config = new KbEmbeddingConfig(
                baseUrl,
                "OPENAI",
                apiKey,
                modelName,
                null
        );
        return getEmbeddingModel(config);
    }

    /**
     * 创建通义千问嵌入模型
     */
    public EmbeddingModel createQwenModel(String apiKey, String modelName) {
        return createQwenModel(apiKey, modelName, "https://dashscope.aliyuncs.com");
    }

    /**
     * 创建通义千问嵌入模型（自定义 baseUrl）
     */
    public EmbeddingModel createQwenModel(String apiKey, String modelName, String baseUrl) {
        KbEmbeddingConfig config = new KbEmbeddingConfig(
                baseUrl,
                "QWEN",
                apiKey,
                modelName,
                null
        );
        return getEmbeddingModel(config);
    }

    /**
     * 创建 Ollama 嵌入模型（本地默认地址）
     */
    public EmbeddingModel createOllamaModel(String modelName) {
        return createOllamaModel(modelName, "http://localhost:11434");
    }

    /**
     * 创建 Ollama 嵌入模型（自定义地址）
     */
    public EmbeddingModel createOllamaModel(String modelName, String baseUrl) {
        KbEmbeddingConfig config = new KbEmbeddingConfig(
                baseUrl,
                "OLLAMA",
                null,
                modelName,
                null
        );
        return getEmbeddingModel(config);
    }

    /**
     * 创建自定义提供商的嵌入模型
     */
    public EmbeddingModel createCustomModel(String provider, String apiKey, String modelName, String baseUrl) {
        KbEmbeddingConfig config = new KbEmbeddingConfig(
                baseUrl,
                provider,
                apiKey,
                modelName,
                null
        );
        return getEmbeddingModel(config);
    }

    /**
     * 创建带自定义请求头的嵌入模型
     */
    public EmbeddingModel createModelWithHeaders(KbEmbeddingConfig config, MultiValueMap<String, String> headers) {
        KbEmbeddingConfig newConfig = new KbEmbeddingConfig(
                config.baseUrl(),
                config.provider(),
                config.apiKey(),
                config.modelName(),
                headers
        );
        return getEmbeddingModel(newConfig);
    }

    public EmbeddingModel createEmbeddingModel(KbEmbeddingConfig config) {
        return switch (config.provider().toUpperCase()) {
            case "OPENAI" -> createOpenAiEmbeddingModel(config);
            case "QWEN" -> createQwenEmbeddingModel(config);
            case "OLLAMA" -> createOllamaEmbeddingModel(config);
            default -> throw new IllegalArgumentException(
                    "Unsupported provider: " + config.provider() +
                            ". Supported providers are: OPENAI, QWEN, OLLAMA"
            );
        };
    }


    private EmbeddingModel createOpenAiEmbeddingModel(KbEmbeddingConfig config) {
        OpenAiApi api = OpenAiApi.builder()
                .apiKey(config.apiKey())
                .build();

        return new OpenAiEmbeddingModel(api);
    }

    private EmbeddingModel createQwenEmbeddingModel(KbEmbeddingConfig config) {
        DashScopeApi api = DashScopeApi.builder()
                .apiKey(config.apiKey())
                .build();

        return new DashScopeEmbeddingModel(api);
    }

    private EmbeddingModel createOllamaEmbeddingModel(KbEmbeddingConfig config) {
        String baseUrl = config.baseUrl() != null ? config.baseUrl() : "http://localhost:11434";
        String modelName = config.modelName() != null ? config.modelName() : "nomic-embed-text";

        OllamaApi ollamaApi = OllamaApi.builder()
                .baseUrl(baseUrl)
                .build();

        return new OllamaEmbeddingModel(
                ollamaApi,
                OllamaEmbeddingOptions.builder()
                        .model(modelName)
                        .build(),
                ObservationRegistry.NOOP,
                ModelManagementOptions.builder().build()
        );
    }
}

