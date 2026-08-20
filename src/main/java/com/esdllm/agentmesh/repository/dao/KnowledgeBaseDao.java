package com.esdllm.agentmesh.repository.dao;

import com.esdllm.agentmesh.model.domain.KnowledgeBase;
import com.baomidou.mybatisplus.extension.service.IService;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import java.util.List;

/**
* @author LiYehe
* @description 针对表【knowledge_base(知识库主表)】的数据库操作 Service
* @createDate 2026-03-10
*/
public interface KnowledgeBaseDao extends IService<KnowledgeBase> {

    /**
     * 分页获取用户的知识库列表
     * @param userId 用户 ID
     * @param page 页码
     * @param pageSize 每页数量
     * @return 知识库分页数据
     */
    Page<KnowledgeBase> getMyKnowledgeBasesPage(Long userId, int page, int pageSize);
    
    /**
     * 获取用户的所有知识库列表（不分页）
     * @param userId 用户 ID
     * @return 知识库列表
     */
    List<KnowledgeBase> getUserKnowledgeBases(Long userId);
}
