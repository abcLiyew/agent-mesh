package com.esdllm.agentmesh.repository.dao;

import com.esdllm.agentmesh.model.domain.AiModel;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
* @author LiYehe
*/
public interface AiModelDao extends IService<AiModel> {

    AiModel getDefaultChatModel(Long userId);

    /**
     * 获取用户的所有活跃模型
     * @param userId 用户 ID
     * @return 模型列表
     */
    List<AiModel> getUserModels(Long userId);

    /**
     * 根据模型名称查询模型
     * @param modelName 模型名称
     * @return 模型信息
     */
    AiModel getByModelName(String modelName);

    /**
     * 获取已启用的模型列表
     */
    List<AiModel> getActiveModels();
    
    /**
     * 获取数据库中的第一个CHAT模型(作为全局默认)
     */
    AiModel getFirstChatModel();
}
