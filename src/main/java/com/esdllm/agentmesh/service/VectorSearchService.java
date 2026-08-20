package com.esdllm.agentmesh.service;

import com.esdllm.agentmesh.model.dto.VectorSearchResult;

import java.util.List;


/**
 * 向量检索服务接口
 */
public interface VectorSearchService {

    /**
     * 语义搜索
     * @param query 查询文本
     * @param kbId 知识库 ID
     * @param topK 返回的最大结果数
     * @param threshold 相似度阈值
     * @return 检索结果列表
     */
    List<VectorSearchResult> search(String query, Long kbId, int topK, Double threshold);
    
    /**
     * 批量语义搜索（多个知识库）
     * @param query 查询文本
     * @param kbIds 知识库 ID 列表
     * @param topK 每个知识库返回的最大结果数
     * @param threshold 相似度阈值
     * @return 检索结果列表
     */
    List<VectorSearchResult> batchSearch(String query, List<Long> kbIds, int topK, Double threshold);
    
    /**
     * 将文本转换为向量
     * @param text 文本
     * @param embeddingModelId 嵌入模型 ID
     * @return 向量数组
     */
    float[] embed(String text, Long embeddingModelId);
    
    /**
     * 批量存储文档到向量数据库
     * @param kbId 知识库 ID
     * @param chunks 文本块列表
     * @param embeddingModelId 嵌入模型 ID
     * @return 向量 ID 列表
     */
    List<String> storeDocuments(Long kbId, List<String> chunks, Long embeddingModelId);
}
