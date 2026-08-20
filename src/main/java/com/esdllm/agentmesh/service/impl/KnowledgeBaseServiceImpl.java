package com.esdllm.agentmesh.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.esdllm.agentmesh.common.ErrorCode;
import com.esdllm.agentmesh.exception.BusinessException;
import com.esdllm.agentmesh.model.domain.KnowledgeBase;
import com.esdllm.agentmesh.model.domain.KnowledgeBaseDocument;
import com.esdllm.agentmesh.repository.dao.KnowledgeBaseDao;
import com.esdllm.agentmesh.repository.dao.KnowledgeBaseDocumentDao;
import com.esdllm.agentmesh.service.KnowledgeBaseService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;

/**
 * 知识库服务实现类
 */
@Service
@Slf4j
public class KnowledgeBaseServiceImpl implements KnowledgeBaseService {

    @Resource
    private KnowledgeBaseDao knowledgeBaseDao;

    @Resource
    private KnowledgeBaseDocumentDao knowledgeBaseDocumentDao;

    @Transactional(rollbackFor = Exception.class)
    @Override
    public Long createKnowledgeBase(KnowledgeBase knowledgeBase, Long userId) {
        // 1. 基础参数校验
        validateBasicParams(knowledgeBase);

        // 2. 设置归属用户和默认值
        knowledgeBase.setUserId(userId);
        knowledgeBase.setStatus(1);
        
        // 设置默认分块参数
        if (knowledgeBase.getChunkSize() == null) {
            knowledgeBase.setChunkSize(500);
        }
        if (knowledgeBase.getChunkOverlap() == null) {
            knowledgeBase.setChunkOverlap(50);
        }

        // 3. 保存到数据库（MyBatis-Plus 会自动填充 isDelete=0）
        boolean saved = knowledgeBaseDao.save(knowledgeBase);
        if (!saved) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "创建知识库失败");
        }

      log.info("创建知识库成功，kbId: {}, userId: {}", knowledgeBase.getId(), userId);
        return knowledgeBase.getId();
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public Boolean updateKnowledgeBase(KnowledgeBase knowledgeBase, Long userId) {
        // 1. 基础参数校验
        validateBasicParams(knowledgeBase);

        // 2. 查询知识库是否存在且属于当前用户
        KnowledgeBase existingKb = knowledgeBaseDao.getById(knowledgeBase.getId());
        if (existingKb == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "知识库不存在");
        }

        if (!existingKb.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.NO_AUTH, "无权限修改该知识库");
        }

        // 3. 更新知识库信息（MyBatis-Plus 会自动填充 updated_at）
        knowledgeBase.setUserId(userId);
        
        boolean updated = knowledgeBaseDao.updateById(knowledgeBase);
        if (!updated) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "更新知识库失败");
        }

      log.info("更新知识库成功，kbId: {}, userId: {}", knowledgeBase.getId(), userId);
        return true;
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public Boolean deleteKnowledgeBase(Long kbId, Long userId) {
        // 1. 查询知识库是否存在
        KnowledgeBase existingKb = knowledgeBaseDao.getById(kbId);
        if (existingKb == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "知识库不存在");
        }

        // 2. 验证权限
        if (!existingKb.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.NO_AUTH, "无权限删除该知识库");
        }

        // 3. 使用 MyBatis-Plus 的逻辑删除（自动设置 is_delete=1）
        boolean deleted = knowledgeBaseDao.removeById(kbId);
        if (!deleted) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "删除知识库失败");
        }

      log.info("删除知识库成功，kbId: {}, userId: {}", kbId, userId);
        return true;
    }

    @Override
    public List<KnowledgeBase> getMyKnowledgeBases(Long userId, int page, int pageSize) {
        if (userId == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户 ID 不能为空");
        }

        // DAO 层已经实现了分页查询和权限过滤
        var resultPage = knowledgeBaseDao.getMyKnowledgeBasesPage(userId, page, pageSize);
        return resultPage.getRecords();
    }

    @Override
    public KnowledgeBase getKnowledgeBaseById(Long kbId, Long userId) {
        if (kbId == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "知识库 ID 不能为空");
        }

        KnowledgeBase kb = knowledgeBaseDao.getById(kbId);
        if (kb == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "知识库不存在");
        }

        // 验证权限
        if (!kb.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.NO_AUTH, "无权限查看该知识库");
        }

        return kb;
    }

    @Override
    public Page<KnowledgeBase> getKnowledgeBasesPage(int page, int pageSize) {
        // 查询所有知识库（排除已删除的）
        return knowledgeBaseDao.lambdaQuery()
                .eq(KnowledgeBase::getIsDelete, 0)
                .orderByDesc(KnowledgeBase::getCreatedAt)
                .page(new Page<>(page, pageSize));
    }

    @Override
    public Page<KnowledgeBaseDocument> getDocumentsPage(Long kbId, int page, int pageSize) {
        if (kbId == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "知识库 ID 不能为空");
        }

        // 使用 DAO 层的分页查询方法
        return knowledgeBaseDocumentDao.getDocumentsByKbPage(kbId, page, pageSize);
    }

    /**
     * 验证基础参数
     */
    private void validateBasicParams(KnowledgeBase knowledgeBase) {
        // 知识库名称不能为空
        if (StrUtil.isBlank(knowledgeBase.getName())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "知识库名称不能为空");
        }

        // 知识库名称长度限制
        if (knowledgeBase.getName().length() < 2 || knowledgeBase.getName().length() > 200) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "知识库名称长度应在 2-200 个字符之间");
        }

        // 向量存储类型不能为空
        if (StrUtil.isBlank(knowledgeBase.getVectorStoreType())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "向量存储类型不能为空");
        }

        // 验证向量存储类型枚举值
        List<String> validTypes = Arrays.asList("DASHSCOPE", "OLLAMA", "OPENAI");
        if (!validTypes.contains(knowledgeBase.getVectorStoreType().toUpperCase())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, 
                "无效的向量存储类型：" + knowledgeBase.getVectorStoreType() + "，有效值为：DASHSCOPE, OLLAMA, OPENAI");
        }

        // 向量存储表名不能为空
        if (StrUtil.isBlank(knowledgeBase.getVectorStoreTable())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "向量存储表名不能为空");
        }

        // 分块大小校验
        if (knowledgeBase.getChunkSize() != null && (knowledgeBase.getChunkSize() <= 0 || knowledgeBase.getChunkSize() > 2000)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "分块大小应在 1-2000 之间");
        }

        // 分块重叠校验
        if (knowledgeBase.getChunkOverlap() != null && (knowledgeBase.getChunkOverlap() < 0 || knowledgeBase.getChunkOverlap() >= knowledgeBase.getChunkSize())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "分块重叠必须小于分块大小");
        }
    }
}
