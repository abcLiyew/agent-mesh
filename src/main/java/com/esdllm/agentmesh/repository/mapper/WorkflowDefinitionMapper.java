package com.esdllm.agentmesh.repository.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.esdllm.agentmesh.model.domain.WorkflowDefinitionEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * 工作流定义Mapper
 */
@Mapper
public interface WorkflowDefinitionMapper extends BaseMapper<WorkflowDefinitionEntity> {
}
