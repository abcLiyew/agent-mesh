package com.esdllm.agentmesh.service.rag.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.esdllm.agentmesh.config.FileUploadProperties;
import com.esdllm.agentmesh.model.domain.KnowledgeBaseDocument;
import com.esdllm.agentmesh.repository.mapper.KnowledgeBaseDocumentMapper;
import com.esdllm.agentmesh.service.rag.RagRetrievalService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

/**
 * RAG检索服务实现
 * 使用Spring AI的VectorStore进行向量检索
 */
@Service
@Slf4j
public class RagRetrievalServiceImpl implements RagRetrievalService {
    
    @Resource
    private KnowledgeBaseDocumentMapper documentMapper;
    
    @Autowired(required = false)
    private EmbeddingModel embeddingModel;
    
    @Autowired(required = false)
    private VectorStore vectorStore;
    
    @Resource
    private FileUploadProperties fileUploadProperties;
    
    // 默认配置
    private static final int DEFAULT_CHUNK_SIZE = 500; // 文档分块大小
    private static final int DEFAULT_CHUNK_OVERLAP = 50; // 分块重叠
    
    @Override
    public List<RetrievedDocument> retrieveFromKnowledgeBases(
            List<Long> kbIds, 
            String query, 
            int topK,
            double similarityThreshold) {
        
        if (vectorStore == null || embeddingModel == null) {
            log.warn("VectorStore或EmbeddingModel未配置，返回空结果");
            return Collections.emptyList();
        }
        
        if (StrUtil.isBlank(query) || kbIds == null || kbIds.isEmpty()) {
            return Collections.emptyList();
        }
        
        try {
            log.info("开始RAG检索，kbIds: {}, query: {}", kbIds, query);
            
            // 1. 构建过滤条件
            Map<String, Object> filterExpression = new HashMap<>();
            filterExpression.put("kbIds", kbIds);
            
            // 2. 执行向量检索
            SearchRequest searchRequest = SearchRequest.builder()
                .query(query)
                .topK(topK)
                .similarityThreshold(similarityThreshold)
                .filterExpression(filterExpression.toString())
                .build();
            
            List<Document> documents = vectorStore.similaritySearch(searchRequest);
            
            if (documents.isEmpty()) {
                log.info("未检索到相关文档");
                return Collections.emptyList();
            }
            
            // 3. 转换为检索结果
            List<RetrievedDocument> results = documents.stream()
                .map(doc -> convertToRetrievedDocument(doc))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
            
            log.info("RAG检索完成，返回 {} 个文档片段", results.size());
            return results;
            
        } catch (Exception e) {
            log.error("RAG检索失败", e);
            return Collections.emptyList();
        }
    }
    
    @Override
    public void embedAndStoreDocument(Long documentId, String content) {
        if (vectorStore == null || embeddingModel == null) {
            log.warn("VectorStore或EmbeddingModel未配置，跳过向量生成");
            return;
        }
        
        if (StrUtil.isBlank(content)) {
            log.warn("文档内容为空，documentId: {}", documentId);
            return;
        }
        
        try {
            log.info("开始为文档生成向量嵌入，documentId: {}", documentId);
            
            // 1. 文档分块
            List<String> chunks = splitDocumentIntoChunks(content);
            
            // 2. 为每个分块创建Document对象
            List<Document> documents = new ArrayList<>();
            for (int i = 0; i < chunks.size(); i++) {
                String chunk = chunks.get(i);
                
                Map<String, Object> metadata = new HashMap<>();
                metadata.put("documentId", documentId);
                metadata.put("chunkIndex", i);
                metadata.put("totalChunks", chunks.size());
                metadata.put("timestamp", System.currentTimeMillis());
                
                Document doc = new Document(chunk, metadata);
                documents.add(doc);
            }
            
            // 3. 存储到向量数据库
            vectorStore.add(documents);
            
            log.info("文档向量嵌入生成成功，documentId: {}, 分块数: {}", documentId, chunks.size());
            
        } catch (Exception e) {
            log.error("文档向量嵌入生成失败，documentId: {}", documentId, e);
        }
    }
    
    @Override
    public void batchEmbedDocuments(Long kbId) {
        if (vectorStore == null || embeddingModel == null) {
            log.warn("VectorStore或EmbeddingModel未配置，跳过量处理");
            return;
        }
        
        try {
            log.info("开始批量处理知识库文档，kbId: {}", kbId);
            
            // 1. 查询知识库下的所有文档
            LambdaQueryWrapper<KnowledgeBaseDocument> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(KnowledgeBaseDocument::getKbId, kbId)
                   .eq(KnowledgeBaseDocument::getStatus, 1)
                   .eq(KnowledgeBaseDocument::getIsDelete, 0);
            
            List<KnowledgeBaseDocument> documents = documentMapper.selectList(wrapper);
            
            if (documents.isEmpty()) {
                log.info("知识库下没有文档，kbId: {}", kbId);
                return;
            }
            
            log.info("找到 {} 个文档待处理", documents.size());
            
            // 2. 逐个处理文档
            int successCount = 0;
            for (KnowledgeBaseDocument doc : documents) {
                try {
                    // 从文件系统中读取文档内容
                    String content = readDocumentContent(doc);
                    
                    if (StrUtil.isNotBlank(content)) {
                        embedAndStoreDocument(doc.getId(), content);
                        successCount++;
                    } else {
                        log.warn("文档内容为空，跳过，documentId: {}", doc.getId());
                    }
                } catch (Exception e) {
                    log.error("处理文档失败，documentId: {}", doc.getId(), e);
                }
            }
            
            log.info("批量处理完成，成功: {}/{}", successCount, documents.size());
            
        } catch (Exception e) {
            log.error("批量处理知识库文档失败，kbId: {}", kbId, e);
        }
    }
    
    // ========== 私有方法 ==========
    
    /**
     * 将Spring AI Document转换为检索结果
     */
    private RetrievedDocument convertToRetrievedDocument(Document doc) {
        if (doc == null || StrUtil.isBlank(doc.getText())) {
            return null;
        }
        
        Map<String, Object> metadata = doc.getMetadata();
        
        return RetrievedDocument.builder()
            .documentId(getLongFromMetadata(metadata, "documentId"))
            .knowledgeBaseId(getLongFromMetadata(metadata, "kbId"))
            .title(getStringFromMetadata(metadata, "title", "未知文档"))
            .content(doc.getText())
            .similarityScore(doc.getScore())
            .metadata(metadata)
            .chunkIndex(getIntFromMetadata(metadata, "chunkIndex", 0))
            .build();
    }
    
    /**
     * 文档分块
     */
    private List<String> splitDocumentIntoChunks(String content) {
        List<String> chunks = new ArrayList<>();
        
        if (StrUtil.isBlank(content)) {
            return chunks;
        }
        
        // 简单按段落分割（实际应该使用更智能的分块策略）
        String[] paragraphs = content.split("\n\n+");
        
        StringBuilder currentChunk = new StringBuilder();
        for (String paragraph : paragraphs) {
            if (currentChunk.length() + paragraph.length() > DEFAULT_CHUNK_SIZE) {
                // 当前块已满，保存并开始新块
                if (currentChunk.length() > 0) {
                    chunks.add(currentChunk.toString());
                    currentChunk = new StringBuilder();
                }
            }
            
            if (currentChunk.length() > 0) {
                currentChunk.append("\n\n");
            }
            currentChunk.append(paragraph);
        }
        
        // 添加最后一个块
        if (currentChunk.length() > 0) {
            chunks.add(currentChunk.toString());
        }
        
        log.debug("文档分块完成，总长度: {}, 分块数: {}", content.length(), chunks.size());
        return chunks;
    }
    
    /**
     * 从元数据中获取Long值
     */
    private Long getLongFromMetadata(Map<String, Object> metadata, String key) {
        Object value = metadata.get(key);
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        return null;
    }
    
    /**
     * 从元数据中获取String值
     */
    private String getStringFromMetadata(Map<String, Object> metadata, String key, String defaultValue) {
        Object value = metadata.get(key);
        return value != null ? value.toString() : defaultValue;
    }
    
    /**
     * 从元数据中获取Integer值
     */
    private Integer getIntFromMetadata(Map<String, Object> metadata, String key, int defaultValue) {
        Object value = metadata.get(key);
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        return defaultValue;
    }
    
    /**
     * 从文件系统读取文档内容
     */
    private String readDocumentContent(KnowledgeBaseDocument doc) {
        try {
            // 从sourceUrl或metadata中获取文件路径
            String filePath = doc.getSourceUrl();
            
            if (StrUtil.isBlank(filePath)) {
                // 尝试从metadata中获取
                if (doc.getMetadataJson() instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> metadata = (Map<String, Object>) doc.getMetadataJson();
                    filePath = (String) metadata.get("filePath");
                }
            }
            
            if (StrUtil.isBlank(filePath)) {
                log.warn("文档未配置文件路径，documentId: {}", doc.getId());
                return null;
            }
            
            // 构建完整文件路径
            Path fullPath;
            if (Paths.get(filePath).isAbsolute()) {
                fullPath = Paths.get(filePath);
            } else {
                // 相对路径，基于uploadDir
                fullPath = Paths.get(fileUploadProperties.getNormalizedUploadDir(), filePath);
            }
            
            // 读取文件内容
            if (!Files.exists(fullPath)) {
                log.warn("文件不存在: {}", fullPath);
                return null;
            }
            
            String content = Files.readString(fullPath);
            log.debug("成功读取文档内容，documentId: {}, 长度: {}", doc.getId(), content.length());
            return content;
            
        } catch (IOException e) {
            log.error("读取文档内容失败，documentId: {}", doc.getId(), e);
            return null;
        }
    }
}
