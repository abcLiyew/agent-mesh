package com.esdllm.agentmesh.repository.dao;

import com.baomidou.mybatisplus.extension.service.IService;
import com.esdllm.agentmesh.model.domain.ToolVersion;
import java.util.List;

public interface ToolVersionDao extends IService<ToolVersion> {

    List<ToolVersion> getByToolId(Long toolId);

    ToolVersion getActiveVersion(Long toolId);

    ToolVersion getCurrentVersion(Long toolId);

    boolean activateVersion(Long versionId, Long toolId);

    List<ToolVersion> getVersionHistory(Long toolId);
}
