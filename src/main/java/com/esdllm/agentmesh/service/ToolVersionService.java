package com.esdllm.agentmesh.service;

import com.esdllm.agentmesh.model.domain.ToolVersion;
import java.util.List;

public interface ToolVersionService {
    
    Long createVersion(ToolVersion version, Long userId);
    
    Boolean switchToVersion(Long toolId, Long versionId, Long userId);
    
    Boolean rollbackToVersion(Long toolId, Long versionId, Long userId);
    
    List<ToolVersion> getVersionHistory(Long toolId, Long userId);
    
    ToolVersion getActiveVersion(Long toolId, Long userId);
    
    Boolean deleteVersion(Long versionId, Long userId);
}
