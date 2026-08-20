package com.esdllm.agentmesh.service.impl;

import cn.hutool.core.util.StrUtil;
import com.esdllm.agentmesh.common.ErrorCode;
import com.esdllm.agentmesh.exception.BusinessException;
import com.esdllm.agentmesh.model.domain.AiModel;
import com.esdllm.agentmesh.model.domain.KnowledgeBase;
import com.esdllm.agentmesh.model.domain.ModelProvider;
import com.esdllm.agentmesh.repository.dao.AiModelDao;
import com.esdllm.agentmesh.repository.dao.KnowledgeBaseDao;
import com.esdllm.agentmesh.repository.dao.ModelProviderDao;
import com.esdllm.agentmesh.model.dto.VectorSearchResult;
import com.esdllm.agentmesh.service.VectorSearchService;
import com.esdllm.agentmesh.util.EncryptionUtil;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.document.MetadataMode;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.ollama.OllamaEmbeddingModel;
import org.springframework.ai.ollama.api.OllamaApi;
import org.springframework.ai.openai.OpenAiEmbeddingModel;
import org.springframework.ai.openai.OpenAiEmbeddingOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.ai.vectorstore.pgvector.PgVectorStore;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 向量检索服务实现类
 * 基于Spring AI PgVectorStore 实现语义搜索
 */
@Service
@Slf4j
public class VectorSearchServiceImpl implements VectorSearchService {

    @Resource
    private JdbcTemplate jdbcTemplate;

    @Resource
    private KnowledgeBaseDao knowledgeBaseDao;

    @Resource
    private AiModelDao aiModelDao;

    @Resource
    private ModelProviderDao modelProviderDao;

    @Resource
    private RestClient.Builder customRestClientBuilder;

    @Override
    public List<VectorSearchResult> search(String query, Long kbId, int topK, Double threshold) {
        if (StrUtil.isBlank(query)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "查询文本不能为空");
        }
        
        if (kbId == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "知识库 ID 不能为空");
        }

        log.info("开始向量检索，query: {}, kbId: {}, topK: {}, threshold: {}", query, kbId, topK, threshold);

        try {
            // 1. 获取知识库信息
            KnowledgeBase kb = knowledgeBaseDao.getById(kbId);
            if (kb == null) {
                throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "知识库不存在");
            }

            // 2. 获取嵌入模型
            AiModel embeddingModel = aiModelDao.getById(kb.getEmbeddingModelId());
            if (embeddingModel == null) {
                throw new BusinessException(ErrorCode.MODEL_NOT_FOUND, "嵌入模型不存在");
            }
            
            // 3. 获取模型提供商配置
            ModelProvider provider = modelProviderDao.getById(embeddingModel.getProviderId());
            if (provider == null) {
                throw new BusinessException(ErrorCode.PROVIDER_NOT_FOUND, 
                    "模型提供商不存在：" + embeddingModel.getProviderId());
            }

            // 4. 创建 EmbeddingModel
            EmbeddingModel model = createEmbeddingModel(embeddingModel, provider);

            // 5. 构建并初始化 PgVectorStore
            PgVectorStore vectorStore = buildPgVectorStore(kb, model);

            // 6. 构建搜索请求
            SearchRequest searchRequest = SearchRequest.builder()
                    .query(query)
                    .topK(topK)
                    .similarityThreshold(threshold != null ? threshold : 0.0)
                    .filterExpression(buildFilterExpression(kbId))
                    .build();

            // 7. 执行相似度搜索
            List<Document> documents = vectorStore.similaritySearch(searchRequest);

            log.info("向量检索完成，找到 {} 个结果", documents.size());

            // 8. 转换为 VectorSearchResult
            return documents.stream()
                    .map(doc -> new VectorSearchResult(
                            doc.getId(),
                            doc.getText(),  // 使用 getText() 方法
                            calculateSimilarity(doc.getMetadata(), query),
                            doc.getMetadata()
                    ))
                    .collect(Collectors.toList());

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("向量检索失败", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "向量检索失败：" + e.getMessage());
        }
    }

    @Override
    public List<VectorSearchResult> batchSearch(String query, List<Long> kbIds, int topK, Double threshold) {
        if (kbIds == null || kbIds.isEmpty()) {
            return new ArrayList<>();
        }

        List<VectorSearchResult> allResults = new ArrayList<>();
        
        for (Long kbId : kbIds) {
            try {
                List<VectorSearchResult> results = search(query, kbId, topK, threshold);
                allResults.addAll(results);
            } catch (Exception e) {
                log.error("知识库 {} 检索失败", kbId, e);
                // 继续检索其他知识库
            }
        }
        
        // 按相似度排序并截取前 topK 个
        return allResults.stream()
                        .sorted((r1, r2) -> Double.compare(r2.similarity(), r1.similarity()))
                        .limit(topK)
                        .toList();
    }

    @Override
    public List<String> storeDocuments(Long kbId, List<String> chunks, Long embeddingModelId) {
        if (kbId == null || chunks == null || chunks.isEmpty() || embeddingModelId == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数不能为空");
        }

        log.info("开始批量存储文档到向量数据库，kbId: {}, chunks: {}, embeddingModelId: {}", 
                kbId, chunks.size(), embeddingModelId);

        try {
            // 1. 获取知识库信息
            KnowledgeBase kb = knowledgeBaseDao.getById(kbId);
            if (kb == null) {
                throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "知识库不存在");
            }

            // 2. 获取嵌入模型
            AiModel embeddingModel = aiModelDao.getById(embeddingModelId);
            if (embeddingModel == null) {
                throw new BusinessException(ErrorCode.MODEL_NOT_FOUND, "嵌入模型不存在");
            }

            // 3. 获取模型提供商配置
            ModelProvider provider = modelProviderDao.getById(embeddingModel.getProviderId());
            if (provider == null) {
                throw new BusinessException(ErrorCode.PROVIDER_NOT_FOUND, 
                    "模型提供商不存在：" + embeddingModel.getProviderId());
            }

            // 4. 创建 EmbeddingModel
            EmbeddingModel model = createEmbeddingModel(embeddingModel, provider);

            // 5. 构建并初始化 PgVectorStore
            PgVectorStore vectorStore = buildPgVectorStore(kb, model);

            // 6. 将文本块转换为 Document 对象
            List<Document> documents = new ArrayList<>();
            for (int i = 0; i < chunks.size(); i++) {
                String chunk = chunks.get(i);
                if (StrUtil.isNotBlank(chunk)) {
                    Map<String, Object> metadata = new HashMap<>();
                    metadata.put("document_id", kbId.toString());
                    metadata.put("chunk_index", i);
                    metadata.put("created_at", System.currentTimeMillis());
                    
                    Document doc = new Document(
                        java.util.UUID.randomUUID().toString(),
                        chunk,
                        metadata
                    );
                    documents.add(doc);
                }
            }

            if (documents.isEmpty()) {
                log.warn("没有有效的文本块需要存储");
                return Collections.emptyList();
            }

            // 7. 分批批量添加到向量数据库（避免单次请求过大导致超时）
            int batchSize = 10; // 每批最多 10 个文档
            List<String> allVectorIds = new ArrayList<>();
            int totalBatches = (int) Math.ceil((double) documents.size() / batchSize);
            
            for (int i = 0; i < totalBatches; i++) {
                int fromIndex = i * batchSize;
                int toIndex = Math.min(fromIndex + batchSize, documents.size());
                List<Document> batchDocs = documents.subList(fromIndex, toIndex);
                
                log.info("正在添加第 {}/{} 批文档，共 {} 个", i + 1, totalBatches, batchDocs.size());
                vectorStore.add(batchDocs);
                
                List<String> batchVectorIds = batchDocs.stream()
                        .map(Document::getId)
                        .toList();
                allVectorIds.addAll(batchVectorIds);
                
                log.info("第 {} 批添加成功", i + 1);
            }

            // 8. 返回生成的文档 ID 列表
            log.info("文档向量化存储完成，成功存储 {} 个向量", allVectorIds.size());
            return allVectorIds;

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("批量存储文档到向量数据库失败，kbId: {}", kbId, e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "向量存储失败：" + e.getMessage());
        }
    }

    @Override
    public float[] embed(String text, Long embeddingModelId) {
        if (StrUtil.isBlank(text)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "文本不能为空");
        }

        try {
            // 获取嵌入模型配置
            AiModel embeddingModel = aiModelDao.getById(embeddingModelId);
            if (embeddingModel == null) {
                throw new BusinessException(ErrorCode.MODEL_NOT_FOUND, "嵌入模型不存在：" + embeddingModelId);
            }
            
            // 获取模型提供商配置
            ModelProvider provider = modelProviderDao.getById(embeddingModel.getProviderId());
            if (provider == null) {
                throw new BusinessException(ErrorCode.PROVIDER_NOT_FOUND, 
                    "模型提供商不存在：" + embeddingModel.getProviderId());
            }
            
            log.info("使用嵌入模型生成向量，model: {}, provider: {}", 
                embeddingModel.getModelName(), provider.getProviderName());
            
            // 创建 EmbeddingModel
            EmbeddingModel model = createEmbeddingModel(embeddingModel, provider);
            
            // 调用 Embedding API
            float[] vector = model.embed(text);
            
            log.info("向量生成成功，维度：{}", vector.length);
            return vector;
            
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("生成向量失败", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "生成向量失败：" + e.getMessage());
        }
    }

    /**
     * 构建 PgVectorStore 实例
     */
    private PgVectorStore buildPgVectorStore(KnowledgeBase kb, EmbeddingModel embeddingModel) {
        // 动态获取嵌入模型的维度
        int dimensions = getEmbeddingDimensions(embeddingModel);
        
        log.info("构建 PgVectorStore，table: {}, dimensions: {}", 
            kb.getVectorStoreTable(), dimensions);
        
        // 使用 Builder 模式创建 PgVectorStore，传入必需的参数
        return PgVectorStore.builder(jdbcTemplate, embeddingModel)
            .dimensions(dimensions)
            .schemaName("public")
            .vectorTableName(kb.getVectorStoreTable())
            .initializeSchema(true) // 启用自动初始化表结构
            .build();
    }

    /**
     * 获取嵌入模型的维度
     */
    private int getEmbeddingDimensions(EmbeddingModel model) {
        // 通过实际调用获取维度
        try {
            float[] testVector = model.embed("test");
            int dimensions = testVector.length;
            log.info("检测到嵌入模型维度：{}", dimensions);
            return dimensions;
        } catch (Exception e) {
            log.warn("无法通过测试获取维度，使用默认值 768", e);
            return 768;
        }
    }

    /**
     * 构建过滤表达式
     */
    private Filter.Expression buildFilterExpression(Long kbId) {
        // 如果需要按知识库 ID 过滤，可以添加过滤条件
        // 目前表名已经区分了不同知识库，所以可能不需要额外过滤
        return null;
    }

    /**
     * 计算相似度分数（从 metadata 中提取或计算）
     */
    private double calculateSimilarity(Map<String, Object> metadata, String query) {
        // 如果 metadata 中包含相似度分数，直接返回
        if (metadata.containsKey("similarity")) {
            return ((Number) metadata.get("similarity")).doubleValue();
        }
        if (metadata.containsKey("score")) {
            return ((Number) metadata.get("score")).doubleValue();
        }
        if (metadata.containsKey("distance")) {
            // 距离转换为相似度
            double distance = ((Number) metadata.get("distance")).doubleValue();
            return 1.0 - distance;
        }
        // 默认返回 1.0（实际应该由向量数据库计算）
        return 1.0;
    }

    /**
     * 根据模型配置创建 EmbeddingModel
     */
    private EmbeddingModel createEmbeddingModel(AiModel aiModel, ModelProvider provider) {
        String providerCode = provider.getProviderCode();
        String baseUrl = provider.getBaseUrl();
        String apiKey = decryptApiKey(provider.getApiKeyEncrypted());
        
        log.info("创建 EmbeddingModel，provider: {}, model: {}", 
            provider.getProviderName(), aiModel.getModelName());
        
        if ("ollama".equalsIgnoreCase(providerCode)) {
            // Ollama 模式
            OllamaApi ollamaApi = OllamaApi.builder()
                .restClientBuilder(customRestClientBuilder)
                .baseUrl(baseUrl)
                .build();
            
            return OllamaEmbeddingModel.builder()
                .ollamaApi(ollamaApi)
                .build();
                
        } else {
            // OpenAI 兼容模式
            OpenAiApi openAiApi = OpenAiApi.builder().restClientBuilder(customRestClientBuilder)
                .baseUrl(baseUrl)
                .apiKey(apiKey)
                .build();
            
            OpenAiEmbeddingOptions options = OpenAiEmbeddingOptions.builder()
                .model(aiModel.getModelName())
                .build();
            
            return new OpenAiEmbeddingModel(openAiApi, MetadataMode.EMBED, options);
        }
    }
    
    /**
     * 解密 API Key
     * @param encryptedKey 加密的 API Key
     * @return 解密后的 API Key
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
}
