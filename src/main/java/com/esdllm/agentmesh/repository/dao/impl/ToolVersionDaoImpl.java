package com.esdllm.agentmesh.repository.dao.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.esdllm.agentmesh.model.domain.ToolVersion;
import com.esdllm.agentmesh.repository.dao.ToolVersionDao;
import com.esdllm.agentmesh.repository.mapper.ToolVersionMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ToolVersionDaoImpl extends ServiceImpl<ToolVersionMapper, ToolVersion>
    implements ToolVersionDao {

    @Resource
    private ToolVersionMapper toolVersionMapper;

    @Override
    public List<ToolVersion> getByToolId(Long toolId) {
        return toolVersionMapper.selectByToolId(toolId);
    }

    @Override
    public ToolVersion getActiveVersion(Long toolId) {
        return toolVersionMapper.selectActiveVersion(toolId);
    }

    @Override
    public ToolVersion getCurrentVersion(Long toolId) {
        return toolVersionMapper.selectCurrentVersion(toolId);
    }

    @Override
    public boolean activateVersion(Long versionId, Long toolId) {
        toolVersionMapper.deactivateAllVersions(toolId);
        return toolVersionMapper.activateVersion(versionId) > 0;
    }

    @Override
    public List<ToolVersion> getVersionHistory(Long toolId) {
        return this.lambdaQuery()
            .eq(ToolVersion::getToolId, toolId)
            .orderByDesc(ToolVersion::getCreatedAt)
            .list();
    }
}
