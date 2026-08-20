package com.esdllm.agentmesh.repository.dao.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.esdllm.agentmesh.model.domain.KnowledgeBase;
import com.esdllm.agentmesh.repository.dao.KnowledgeBaseDao;
import com.esdllm.agentmesh.repository.mapper.KnowledgeBaseMapper;
import org.springframework.stereotype.Service;

import java.util.List;

/**
* @author LiYehe
* @description 针对表【knowledge_base(知识库主表)】的数据库操作 Service 实现
* @createDate 2026-03-10
*/
@Service
public class KnowledgeBaseDaoImpl extends ServiceImpl<KnowledgeBaseMapper, KnowledgeBase>
    implements KnowledgeBaseDao {

    @Override
    public Page<KnowledgeBase> getMyKnowledgeBasesPage(Long userId, int page, int pageSize) {
        Page<KnowledgeBase> kbPage = new Page<>(page, pageSize);
        LambdaQueryWrapper<KnowledgeBase> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(KnowledgeBase::getUserId, userId)
                   .eq(KnowledgeBase::getIsDelete, 0)
                   .orderByDesc(KnowledgeBase::getCreatedAt);
        
        return this.page(kbPage, queryWrapper);
    }
    
    @Override
    public List<KnowledgeBase> getUserKnowledgeBases(Long userId) {
        LambdaQueryWrapper<KnowledgeBase> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(KnowledgeBase::getUserId, userId)
                   .eq(KnowledgeBase::getIsDelete, 0)
                   .orderByDesc(KnowledgeBase::getCreatedAt);
        
        return this.list(queryWrapper);
    }
}
