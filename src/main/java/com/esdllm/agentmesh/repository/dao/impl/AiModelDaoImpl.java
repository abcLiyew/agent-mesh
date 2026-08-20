package com.esdllm.agentmesh.repository.dao.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.esdllm.agentmesh.model.domain.AiModel;
import com.esdllm.agentmesh.repository.dao.AiModelDao;
import com.esdllm.agentmesh.repository.mapper.AiModelMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @author LiYehe
 * @description 针对表【ai_model(模型实例表：存储具体可用的模型列表 (如 gpt-4, qwen-turbo)，关联到具体的 Provider)】的数据库操作 Service 实现
 * @createDate 2026-03-09 13:34:38
 */
@Service
public class AiModelDaoImpl extends ServiceImpl<AiModelMapper, AiModel>
        implements AiModelDao {
    @Resource
    private  AiModelMapper aiModelMapper;

    @Override
    public AiModel getDefaultChatModel(Long userId) {
        if (userId == null) {
            return null;
        }

        // 优先使用用户自己的模型，如果没有则使用公共模型(user_id=1)
        LambdaQueryWrapper<AiModel> wrapper = new LambdaQueryWrapper<>();
        wrapper.and(w -> w.eq(AiModel::getUserId, userId)
                         .or()
                         .eq(AiModel::getUserId, 1))  // 公共模型
                .eq(AiModel::getModelType, "CHAT")
                .eq(AiModel::getIsActive, true)
                .eq(AiModel::getIsDelete, 0)
                .orderByDesc(AiModel::getUserId)  // 优先用户自己的模型
                .orderByDesc(AiModel::getCreatedAt)
                .last("LIMIT 1");

        return this.getOne(wrapper);
    }

    @Override
    public List<AiModel> getUserModels(Long userId) {
        // 返回用户自己的模型 + 公共模型(user_id=1)
        LambdaQueryWrapper<AiModel> wrapper = new LambdaQueryWrapper<>();
        wrapper.and(w -> w.eq(AiModel::getUserId, userId)
                         .or()
                         .eq(AiModel::getUserId, 1))  // 公共模型
               .eq(AiModel::getIsActive, true)
               .eq(AiModel::getIsDelete, 0)
               .orderByDesc(AiModel::getUserId)  // 优先用户自己的模型
               .orderByDesc(AiModel::getCreatedAt);
        return aiModelMapper.selectList(wrapper);
    }

    @Override
    public AiModel getByModelName(String modelName) {
        if (modelName == null || modelName.isEmpty()) {
            return null;
        }
        
        LambdaQueryWrapper<AiModel> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AiModel::getModelName, modelName)
               .last("LIMIT 1");
        
        return this.getOne(wrapper);
    }

    @Override
    public List<AiModel> getActiveModels() {
        LambdaQueryWrapper<AiModel> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AiModel::getIsActive, true);
        return aiModelMapper.selectList(wrapper);
    }
    
    @Override
    public AiModel getFirstChatModel() {
        LambdaQueryWrapper<AiModel> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AiModel::getModelType, "CHAT")
               .eq(AiModel::getIsActive, true)
               .orderByAsc(AiModel::getId)
               .last("LIMIT 1");
        
        return this.getOne(wrapper);
    }
}
