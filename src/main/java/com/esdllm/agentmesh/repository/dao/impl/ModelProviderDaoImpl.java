package com.esdllm.agentmesh.repository.dao.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.esdllm.agentmesh.model.domain.ModelProvider;
import com.esdllm.agentmesh.repository.dao.ModelProviderDao;
import com.esdllm.agentmesh.repository.mapper.ModelProviderMapper;
import org.springframework.stereotype.Service;

import java.util.List;

/**
* @author LiYehe
* @description 针对表【model_provider(模型提供商配置表：存储用户配置的 LLM 服务商信息 (如 OpenAI, Azure, Ollama 等))】的数据库操作Service实现
* @createDate 2026-03-09 13:34:39
*/
@Service
public class ModelProviderDaoImpl extends ServiceImpl<ModelProviderMapper, ModelProvider>
    implements ModelProviderDao {

    @Override
    public ModelProvider getModelProviderById(Long id) {
        return getById(id);
    }

    @Override
    public List<ModelProvider> getModelProviderListByUserId(Long userId) {
        LambdaQueryWrapper<ModelProvider> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ModelProvider::getUserId, userId);
        return list(queryWrapper);
    }

    @Override
    public List<ModelProvider> publicList() {
        // 查询所有启用的提供商(status=1)
        LambdaQueryWrapper<ModelProvider> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ModelProvider::getStatus, 1)
                .eq(ModelProvider::getIsDelete, 0);
        return list(queryWrapper);
    }
}




