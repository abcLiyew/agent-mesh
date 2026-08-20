package com.esdllm.agentmesh.service;

import com.esdllm.agentmesh.model.domain.KnowledgeBaseDocument;

import java.util.List;

/**
 * 知识库文档服务接口
 */
public interface KnowledgeBaseDocumentService {
    
    /**
     * 创建文档
     * @param document 文档信息
     * @param kbId 知识库 ID
     * @return 文档 ID
     */
    Long createDocument(KnowledgeBaseDocument document, Long kbId);
    
    /**
     * 更新文档
     * @param document 文档信息
     * @return 是否成功
     */
    Boolean updateDocument(KnowledgeBaseDocument document);
    
    /**
     * 删除文档
     * @param docId 文档 ID
     * @return 是否成功
     */
    Boolean deleteDocument(Long docId);
    
    /**
     * 获取知识库的文档列表
     * @param kbId 知识库 ID
     * @param page 页码
     * @param pageSize 每页数量
     * @return 文档列表
     */
    List<KnowledgeBaseDocument> getDocumentsByKb(Long kbId, int page, int pageSize);
    
    /**
     * 根据 ID 获取文档
     * @param docId 文档 ID
     * @return 文档信息
     */
    KnowledgeBaseDocument getDocumentById(Long docId);
    
    /**
     * 更新文档处理状态
     * @param docId 文档 ID
     * @param status 状态
     * @param chunkCount 分块数量
     * @param vectorIds 向量 ID 列表
     * @return 是否成功
     */
    Boolean updateDocumentStatus(Long docId, Integer status, Integer chunkCount, Object vectorIds);
}
