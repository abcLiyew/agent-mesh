package com.esdllm.agentmesh.service.rag;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * RAG检索服务接口
 * 负责从知识库中检索相关文档片段
 */
public interface RagRetrievalService {
    
    /**
     * 从指定知识库检索相关文档
     * 
     * @param kbIds 知识库ID列表
     * @param query 查询文本
     * @param topK 返回结果数量
     * @param similarityThreshold 相似度阈值(0-1)
     * @return 检索结果列表
     */
    List<RetrievedDocument> retrieveFromKnowledgeBases(
        List<Long> kbIds, 
        String query, 
        int topK,
        double similarityThreshold
    );
    
    /**
     * 为文档生成向量嵌入并存储
     * 
     * @param documentId 文档ID
     * @param content 文档内容
     */
    void embedAndStoreDocument(Long documentId, String content);
    
    /**
     * 批量生成文档向量嵌入
     * 
     * @param kbId 知识库ID
     */
    void batchEmbedDocuments(Long kbId);
    
    // ========== 内部类 ==========
    
    /**
     * 检索到的文档片段
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    class RetrievedDocument {
        /**
         * 文档ID
         */
        private Long documentId;
        
        /**
         * 知识库ID
         */
        private Long knowledgeBaseId;
        
        /**
         * 文档标题
         */
        private String title;
        
        /**
         * 文档片段内容
         */
        private String content;
        
        /**
         * 相似度得分(0-1)
         */
        private Double similarityScore;
        
        /**
         * 元数据(页码、章节等)
         */
        private Map<String, Object> metadata;
        
        /**
         * 在原文中的位置
         */
        private Integer chunkIndex;
    }
}
