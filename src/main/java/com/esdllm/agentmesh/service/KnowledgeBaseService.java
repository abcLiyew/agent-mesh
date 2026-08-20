package com.esdllm.agentmesh.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.esdllm.agentmesh.model.domain.KnowledgeBase;

import java.util.List;

/**
 * 知识库服务接口
 */
public interface KnowledgeBaseService {
    
    /**
     * 创建知识库
     * @param knowledgeBase 知识库信息
     * @param userId 用户 ID
     * @return 知识库 ID
     */
    Long createKnowledgeBase(KnowledgeBase knowledgeBase, Long userId);
    
    /**
     * 更新知识库
     * @param knowledgeBase 知识库信息
     * @param userId 用户 ID
     * @return 是否成功
     */
    Boolean updateKnowledgeBase(KnowledgeBase knowledgeBase, Long userId);
    
    /**
     * 删除知识库
     * @param kbId 知识库 ID
     * @param userId 用户 ID
     * @return 是否成功
     */
    Boolean deleteKnowledgeBase(Long kbId, Long userId);
    
    /**
     * 获取用户的知识库列表
     * @param userId 用户 ID
     * @param page 页码
     * @param pageSize 每页数量
     * @return 知识库列表
     */
    List<KnowledgeBase> getMyKnowledgeBases(Long userId, int page, int pageSize);
    
    /**
     * 根据 ID 获取知识库
     * @param kbId 知识库 ID
     * @param userId 用户 ID
     * @return 知识库信息
     */
    KnowledgeBase getKnowledgeBaseById(Long kbId, Long userId);
    
    /**
     * 获取所有知识库分页列表（管理员功能）
     * @param page 页码
     * @param pageSize 每页数量
     * @return 知识库分页数据
     */
    Page<KnowledgeBase> getKnowledgeBasesPage(int page, int pageSize);
    
    /**
     * 获取知识库文档分页列表（管理员功能）
     * @param kbId 知识库 ID
     * @param page 页码
     * @param pageSize 每页数量
     * @return 文档分页数据
     */
    Page<com.esdllm.agentmesh.model.domain.KnowledgeBaseDocument> getDocumentsPage(Long kbId, int page, int pageSize);
}
