package com.esdllm.agentmesh.service.impl;

import cn.hutool.core.util.StrUtil;
import com.esdllm.agentmesh.common.ErrorCode;
import com.esdllm.agentmesh.exception.BusinessException;
import com.esdllm.agentmesh.model.domain.ToolVersion;
import com.esdllm.agentmesh.model.domain.Tools;
import com.esdllm.agentmesh.repository.dao.ToolVersionDao;
import com.esdllm.agentmesh.repository.dao.ToolsDao;
import com.esdllm.agentmesh.service.ToolVersionService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

@Service
@Slf4j
public class ToolVersionServiceImpl implements ToolVersionService {

    @Resource
    private ToolVersionDao toolVersionDao;

    @Resource
    private ToolsDao toolsDao;

    @Transactional(rollbackFor = Exception.class)
    @Override
    public Long createVersion(ToolVersion version, Long userId) {
        if (version.getToolId() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "工具 ID 不能为空");
        }

        Tools tool = toolsDao.getById(version.getToolId());
        if (tool == null) {
            throw new BusinessException(ErrorCode.TOOL_NOT_FOUND, "工具不存在");
        }

        if (tool.getOwnerId() != null && !tool.getOwnerId().equals(userId)) {
            throw new BusinessException(ErrorCode.NO_AUTH, "无权限为该工具创建版本");
        }

        if (StrUtil.isBlank(version.getVersionNumber())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "版本号不能为空");
        }

        ToolVersion existingVersion = toolVersionDao.getByToolId(version.getToolId()).stream()
            .filter(v -> v.getVersionNumber().equals(version.getVersionNumber()))
            .findFirst()
            .orElse(null);

        if (existingVersion != null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, 
                "版本号 " + version.getVersionNumber() + " 已存在");
        }

        ToolVersion activeVersion = toolVersionDao.getActiveVersion(version.getToolId());
        if (activeVersion != null) {
            version.setParentVersionId(activeVersion.getId());
        }

        version.setSourceType(tool.getSourceType());
        version.setInputSchema(tool.getInputSchema());
        version.setOutputSchema(tool.getOutputSchema());
        version.setCustomEndpointUrl(tool.getCustomEndpointUrl());
        version.setMcpServerId(tool.getMcpServerId());
        version.setIsActive(false);
        version.setIsCurrent(false);
        version.setCreatedBy(userId);
        version.setCreatedAt(new Date());

        boolean saved = toolVersionDao.save(version);
        if (!saved) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "创建版本失败");
        }

        log.info("创建工具版本成功，toolId: {}, version: {}, userId: {}", 
                version.getToolId(), version.getVersionNumber(), userId);

        return version.getId();
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public Boolean switchToVersion(Long toolId, Long versionId, Long userId) {
        Tools tool = toolsDao.getById(toolId);
        if (tool == null) {
            throw new BusinessException(ErrorCode.TOOL_NOT_FOUND, "工具不存在");
        }

        if (tool.getOwnerId() != null && !tool.getOwnerId().equals(userId)) {
            throw new BusinessException(ErrorCode.NO_AUTH, "无权限切换该工具版本");
        }

        ToolVersion targetVersion = toolVersionDao.getById(versionId);
        if (targetVersion == null || !targetVersion.getToolId().equals(toolId)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "版本不存在或不属于该工具");
        }

        boolean activated = toolVersionDao.activateVersion(versionId, toolId);
        if (!activated) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "激活版本失败");
        }

        tool.setCurrentVersionId(versionId);
        tool.setUpdatedAt(new Date());
        boolean updated = toolsDao.updateById(tool);
        if (!updated) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "更新工具版本引用失败");
        }

        log.info("切换工具版本成功，toolId: {}, versionId: {}, userId: {}", 
                toolId, versionId, userId);

        return true;
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public Boolean rollbackToVersion(Long toolId, Long versionId, Long userId) {
        Tools tool = toolsDao.getById(toolId);
        if (tool == null) {
            throw new BusinessException(ErrorCode.TOOL_NOT_FOUND, "工具不存在");
        }

        if (tool.getOwnerId() != null && !tool.getOwnerId().equals(userId)) {
            throw new BusinessException(ErrorCode.NO_AUTH, "无权限回滚该工具版本");
        }

        ToolVersion targetVersion = toolVersionDao.getById(versionId);
        if (targetVersion == null || !targetVersion.getToolId().equals(toolId)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "版本不存在或不属于该工具");
        }

        ToolVersion currentVersion = toolVersionDao.getCurrentVersion(toolId);
        if (currentVersion != null && currentVersion.getId().equals(versionId)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "当前已是该版本，无需回滚");
        }

        boolean activated = toolVersionDao.activateVersion(versionId, toolId);
        if (!activated) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "回滚版本失败");
        }

        tool.setCurrentVersionId(versionId);
        tool.setUpdatedAt(new Date());
        boolean updated = toolsDao.updateById(tool);
        if (!updated) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "更新工具版本引用失败");
        }

        log.info("回滚工具版本成功，toolId: {}, rollbackToVersionId: {}, userId: {}", 
                toolId, versionId, userId);

        return true;
    }

    @Override
    public List<ToolVersion> getVersionHistory(Long toolId, Long userId) {
        Tools tool = toolsDao.getById(toolId);
        if (tool == null) {
            throw new BusinessException(ErrorCode.TOOL_NOT_FOUND, "工具不存在");
        }

        if (tool.getOwnerId() != null && !tool.getOwnerId().equals(userId)) {
            throw new BusinessException(ErrorCode.NO_AUTH, "无权限查看该工具版本历史");
        }

        return toolVersionDao.getVersionHistory(toolId);
    }

    @Override
    public ToolVersion getActiveVersion(Long toolId, Long userId) {
        Tools tool = toolsDao.getById(toolId);
        if (tool == null) {
            throw new BusinessException(ErrorCode.TOOL_NOT_FOUND, "工具不存在");
        }

        if (tool.getOwnerId() != null && !tool.getOwnerId().equals(userId)) {
            throw new BusinessException(ErrorCode.NO_AUTH, "无权限查看该工具版本");
        }

        return toolVersionDao.getActiveVersion(toolId);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public Boolean deleteVersion(Long versionId, Long userId) {
        ToolVersion version = toolVersionDao.getById(versionId);
        if (version == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "版本不存在");
        }

        Tools tool = toolsDao.getById(version.getToolId());
        if (tool == null) {
            throw new BusinessException(ErrorCode.TOOL_NOT_FOUND, "工具不存在");
        }

        if (tool.getOwnerId() != null && !tool.getOwnerId().equals(userId)) {
            throw new BusinessException(ErrorCode.NO_AUTH, "无权限删除该工具版本");
        }

        if (version.getIsActive()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "不能删除当前激活的版本");
        }

        boolean deleted = toolVersionDao.removeById(versionId);
        if (!deleted) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "删除版本失败");
        }

        log.info("删除工具版本成功，versionId: {}, userId: {}", versionId, userId);

        return true;
    }
}
