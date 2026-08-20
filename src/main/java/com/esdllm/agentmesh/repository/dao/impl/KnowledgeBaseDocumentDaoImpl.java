package com.esdllm.agentmesh.repository.dao.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.esdllm.agentmesh.model.domain.KnowledgeBaseDocument;
import com.esdllm.agentmesh.repository.dao.KnowledgeBaseDocumentDao;
import com.esdllm.agentmesh.repository.mapper.KnowledgeBaseDocumentMapper;
import org.springframework.stereotype.Service;

/**
* @author LiYehe
* @description 针对表【knowledge_base_document(知识库文档表)】的数据库操作 Service 实现
* @createDate 2026-03-10
*/
@Service
public class KnowledgeBaseDocumentDaoImpl extends ServiceImpl<KnowledgeBaseDocumentMapper, KnowledgeBaseDocument>
    implements KnowledgeBaseDocumentDao {

    @Override
    public Page<KnowledgeBaseDocument> getDocumentsByKbPage(Long kbId, int page, int pageSize) {
        Page<KnowledgeBaseDocument> docPage = new Page<>(page, pageSize);
        LambdaQueryWrapper<KnowledgeBaseDocument> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(KnowledgeBaseDocument::getKbId, kbId)
                   .eq(KnowledgeBaseDocument::getIsDelete, 0)
                   .orderByDesc(KnowledgeBaseDocument::getCreatedAt);
        
        return this.page(docPage, queryWrapper);
    }
}
