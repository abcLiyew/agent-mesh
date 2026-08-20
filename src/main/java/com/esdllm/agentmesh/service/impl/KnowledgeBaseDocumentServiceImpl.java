package com.esdllm.agentmesh.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.esdllm.agentmesh.common.ErrorCode;
import com.esdllm.agentmesh.exception.BusinessException;
import com.esdllm.agentmesh.model.domain.KnowledgeBaseDocument;
import com.esdllm.agentmesh.repository.dao.KnowledgeBaseDocumentDao;
import com.esdllm.agentmesh.service.KnowledgeBaseDocumentService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;

/**
 * 知识库文档服务实现类
 */
@Service
@Slf4j
public class KnowledgeBaseDocumentServiceImpl implements KnowledgeBaseDocumentService {

    @Resource
    private KnowledgeBaseDocumentDao knowledgeBaseDocumentDao;

    @Transactional(rollbackFor = Exception.class)
    @Override
    public Long createDocument(KnowledgeBaseDocument document, Long kbId) {
        // 1. 基础参数校验
        validateBasicParams(document);

        // 2. 设置知识库 ID 和默认值
        document.setKbId(kbId);
        document.setIsDelete(0);
        document.setStatus(0); // 初始状态为处理中
        document.setChunkCount(0);
        document.setCreatedAt(new Date());
        document.setUpdatedAt(new Date());

        // 3. 保存到数据库
        boolean saved = knowledgeBaseDocumentDao.save(document);
        if (!saved) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "创建文档失败");
        }

      log.info("创建文档成功，docId: {}, kbId: {}", document.getId(), kbId);
        return document.getId();
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public Boolean updateDocument(KnowledgeBaseDocument document) {
        // 1. 基础参数校验
        validateBasicParams(document);

        // 2. 查询文档是否存在
        KnowledgeBaseDocument existingDoc = knowledgeBaseDocumentDao.getById(document.getId());
        if (existingDoc == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "文档不存在");
        }

        // 3. 更新文档信息
        document.setUpdatedAt(new Date());
        
        boolean updated = knowledgeBaseDocumentDao.updateById(document);
        if (!updated) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "更新文档失败");
        }

      log.info("更新文档成功，docId: {}", document.getId());
        return true;
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public Boolean deleteDocument(Long docId) {
        // 1. 查询文档是否存在
        KnowledgeBaseDocument existingDoc = knowledgeBaseDocumentDao.getById(docId);
        if (existingDoc == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "文档不存在");
        }

        // 2. 使用 MyBatis-Plus 的逻辑删除
        boolean deleted = knowledgeBaseDocumentDao.removeById(docId);
        if (!deleted) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "删除文档失败");
        }

      log.info("删除文档成功，docId: {}", docId);
        return true;
    }

    @Override
    public List<KnowledgeBaseDocument> getDocumentsByKb(Long kbId, int page, int pageSize) {
        if (kbId == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "知识库 ID 不能为空");
        }

        Page<KnowledgeBaseDocument> resultPage = knowledgeBaseDocumentDao.getDocumentsByKbPage(kbId, page, pageSize);
        return resultPage.getRecords();
    }

    @Override
    public KnowledgeBaseDocument getDocumentById(Long docId) {
        if (docId == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "文档 ID 不能为空");
        }

        KnowledgeBaseDocument document = knowledgeBaseDocumentDao.getById(docId);
        if (document == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "文档不存在");
        }

        return document;
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public Boolean updateDocumentStatus(Long docId, Integer status, Integer chunkCount, Object vectorIds) {
        if (docId == null || status == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数不能为空");
        }

        KnowledgeBaseDocument document = new KnowledgeBaseDocument();
        document.setId(docId);
        document.setStatus(status);
        document.setChunkCount(chunkCount);
        
        // 将 Object 类型的 vectorIds 转换为 List<String>
        if (vectorIds != null) {
            @SuppressWarnings("unchecked")
            List<String> vectorIdList = (vectorIds instanceof List) 
                ? (List<String>) vectorIds 
                : Collections.singletonList(String.valueOf(vectorIds));
            document.setVectorIds(vectorIdList);
        }
        
        document.setUpdatedAt(new Date());
        
        boolean updated = knowledgeBaseDocumentDao.updateById(document);
        if (!updated) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "更新文档状态失败");
        }

      log.info("更新文档状态成功，docId: {}, status: {}, chunkCount: {}", docId, status, chunkCount);
        return true;
    }

    /**
     * 验证基础参数
     */
    private void validateBasicParams(KnowledgeBaseDocument document) {
        // 文档名称不能为空
        if (StrUtil.isBlank(document.getDocName())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "文档名称不能为空");
        }

        // 文档名称长度限制
        if (document.getDocName().length() < 2 || document.getDocName().length() > 200) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "文档名称长度应在 2-200 个字符之间");
        }

        // 文档类型不能为空
        if (StrUtil.isBlank(document.getDocType())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "文档类型不能为空");
        }

        // 验证文档类型枚举值
        List<String> validTypes = Arrays.asList("TEXT", "PDF", "WORD", "EXCEL", "MARKDOWN", "URL");
        if (!validTypes.contains(document.getDocType().toUpperCase())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, 
                "无效的文档类型：" + document.getDocType() + "，有效值为：TEXT, PDF, WORD, EXCEL, MARKDOWN, URL");
        }
    }
}
