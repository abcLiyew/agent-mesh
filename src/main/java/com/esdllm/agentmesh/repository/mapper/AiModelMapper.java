package com.esdllm.agentmesh.repository.mapper;

import com.esdllm.agentmesh.model.domain.AiModel;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;

/**
* @author LiYehe
* @description 针对表【ai_model(模型实例表：存储具体可用的模型列表 (如 gpt-4, qwen-turbo)，关联到具体的 Provider)】的数据库操作Mapper
* @createDate 2026-03-09 13:26:58
* @Entity generator.domain.AiModel
*/
public interface AiModelMapper extends BaseMapper<AiModel> {

}




