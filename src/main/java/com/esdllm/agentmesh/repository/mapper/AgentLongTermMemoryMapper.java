package com.esdllm.agentmesh.repository.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.esdllm.agentmesh.model.domain.AgentLongTermMemory;
import org.apache.ibatis.annotations.Mapper;

/**
 * 长期记忆Mapper
 */
@Mapper
public interface AgentLongTermMemoryMapper extends BaseMapper<AgentLongTermMemory> {
}
