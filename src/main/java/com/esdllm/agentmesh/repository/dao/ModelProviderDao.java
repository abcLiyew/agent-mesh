package com.esdllm.agentmesh.repository.dao;

import com.esdllm.agentmesh.model.domain.ModelProvider;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
* @author LiYehe
* @description 针对表【model_provider(模型提供商配置表：存储用户配置的 LLM 服务商信息 (如 OpenAI, Azure, Ollama 等))】的数据库操作Service
* @createDate 2026-03-09 13:26:59
*/
public interface ModelProviderDao extends IService<ModelProvider> {

    ModelProvider getModelProviderById(Long id);

    List<ModelProvider> getModelProviderListByUserId(Long userId);

    List<ModelProvider> publicList();
}
