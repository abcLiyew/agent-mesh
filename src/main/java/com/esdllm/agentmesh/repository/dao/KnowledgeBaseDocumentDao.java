package com.esdllm.agentmesh.repository.dao;

import com.esdllm.agentmesh.model.domain.KnowledgeBaseDocument;
import com.baomidou.mybatisplus.extension.service.IService;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

/**
* @author LiYehe
* @description 针对表【knowledge_base_document(知识库文档表)】的数据库操作 Service
* @createDate 2026-03-10
*/
public interface KnowledgeBaseDocumentDao extends IService<KnowledgeBaseDocument> {

    /**
     * 分页获取知识库的文档列表
     * @param kbId 知识库 ID
     * @param page 页码
     * @param pageSize 每页数量
     * @return 文档分页数据
     */
    Page<KnowledgeBaseDocument> getDocumentsByKbPage(Long kbId, int page, int pageSize);
}
