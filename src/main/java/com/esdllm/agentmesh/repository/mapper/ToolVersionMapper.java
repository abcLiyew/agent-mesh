package com.esdllm.agentmesh.repository.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.esdllm.agentmesh.model.domain.ToolVersion;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ToolVersionMapper extends BaseMapper<ToolVersion> {
    
    List<ToolVersion> selectByToolId(@Param("toolId") Long toolId);
    
    ToolVersion selectActiveVersion(@Param("toolId") Long toolId);
    
    ToolVersion selectCurrentVersion(@Param("toolId") Long toolId);
    
    int deactivateAllVersions(@Param("toolId") Long toolId);
    
    int activateVersion(@Param("versionId") Long versionId);
}
