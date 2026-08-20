package com.esdllm.agentmesh.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.esdllm.agentmesh.common.ErrorCode;
import com.esdllm.agentmesh.exception.BusinessException;
import com.esdllm.agentmesh.model.domain.Tools;
import com.esdllm.agentmesh.repository.dao.ToolsDao;
import com.esdllm.agentmesh.service.ToolService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;

/**
 * 工具服务实现类
 */
@Service
@Slf4j
public class ToolServiceImpl implements ToolService {

    @Resource
    private ToolsDao toolsDao;

    @Transactional(rollbackFor = Exception.class)
    @Override
    public Long createTool(Tools tool, Long userId) {
        // 1. 基础参数校验
        validateBasicParams(tool);

        // 2. 设置归属用户（NULL 表示系统工具）（MyBatis-Plus 会自动填充 is_delete=0）
        if (userId != null) {
           tool.setOwnerId(userId);
        }

        // 3. 保存到数据库
        boolean saved = toolsDao.save(tool);
        if (!saved) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "创建工具失败");
        }

       log.info("创建工具成功，toolId: {}, userId: {}", tool.getId(), userId);
        return tool.getId();
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public Boolean updateTool(Tools tool, Long userId) {
        // 1. 基础参数校验
        validateBasicParams(tool);

        // 2. 查询工具是否存在
        Tools existingTool = toolsDao.getById(tool.getId());
        if (existingTool == null) {
            throw new BusinessException(ErrorCode.TOOL_NOT_FOUND, "工具不存在");
        }

        // 3. 验证权限（系统工具需要管理员权限）
        if (existingTool.getOwnerId() == null) {
            // 系统工具，这里简化处理，允许所有者为 null 的情况
           log.warn("尝试更新系统工具，toolId: {}", tool.getId());
        } else if (!existingTool.getOwnerId().equals(userId)) {
            throw new BusinessException(ErrorCode.NO_AUTH, "无权限修改该工具");
        }

        // 4. 更新工具信息（MyBatis-Plus 会自动填充 updated_at）
       tool.setUpdatedAt(new java.util.Date());
        
        boolean updated = toolsDao.updateById(tool);
        if (!updated) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "更新工具失败");
        }

       log.info("更新工具成功，toolId: {}, userId: {}", tool.getId(), userId);
        return true;
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public Boolean deleteTool(Long toolId, Long userId) {
        // 1. 查询工具是否存在
        Tools existingTool = toolsDao.getById(toolId);
        if (existingTool == null) {
            throw new BusinessException(ErrorCode.TOOL_NOT_FOUND, "工具不存在");
        }

        // 2. 验证权限（系统工具不允许普通用户删除）
        if (existingTool.getOwnerId() == null) {
            throw new BusinessException(ErrorCode.NO_AUTH, "无权限删除系统工具");
        }

        if (!existingTool.getOwnerId().equals(userId)) {
            throw new BusinessException(ErrorCode.NO_AUTH, "无权限删除该工具");
        }

        // 3. 使用 MyBatis-Plus 的逻辑删除（自动设置 is_delete=1）
        boolean deleted = toolsDao.removeById(toolId);
        if (!deleted) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "删除工具失败");
        }

       log.info("删除工具成功，toolId: {}, userId: {}", toolId, userId);
        return true;
    }

    @Override
    public List<Tools> getMyTools(Long userId) {
        // 使用 DAO 层方法查询用户私有工具和系统内置工具（自动过滤已删除）
        return toolsDao.getMyTools(userId);
    }

    @Override
    public Tools getToolById(Long toolId, Long userId) {
        Tools tool = toolsDao.getById(toolId);
        if (tool == null) {
            throw new BusinessException(ErrorCode.TOOL_NOT_FOUND, "工具不存在");
        }

        // 验证权限
        if (tool.getOwnerId() != null && !tool.getOwnerId().equals(userId)) {
            throw new BusinessException(ErrorCode.NO_AUTH, "无权限查看该工具");
        }

        return tool;
    }

    @Override
    public List<Tools> getSystemTools() {
        // 使用 DAO 层方法查询系统内置工具（自动过滤已删除）
        return toolsDao.getSystemTools();
    }

    @Override
    public List<Tools> getAllTools(int page, int pageSize) {
        // 查询所有工具（排除已删除的）
        return toolsDao.list(
            new LambdaQueryWrapper<Tools>()
                .eq(Tools::getIsDelete, 0)
                .orderByDesc(Tools::getCreatedAt)
        ).stream()
                .skip((long) (page - 1) * pageSize)
                .limit(pageSize)
                .toList();
    }

    @Override
    public Long getToolsCount() {
        // 获取工具总数（排除已删除的）
        return toolsDao.count(
            new LambdaQueryWrapper<Tools>()
                .eq(Tools::getIsDelete, 0)
        );
    }

    /**
     * 验证基础参数
     */
    private void validateBasicParams(Tools tool) {
        // 工具来源不能为空
        if (StrUtil.isBlank(tool.getSourceType())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "工具来源不能为空");
        }

        // 验证工具来源枚举值
        List<String> validSourceTypes = Arrays.asList("SYSTEM", "USER_HTTP", "USER_MCP");
        if (!validSourceTypes.contains(tool.getSourceType().toUpperCase())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, 
                "无效的工具来源：" + tool.getSourceType() + "，有效值为：SYSTEM, USER_HTTP, USER_MCP");
        }

        // 工具代码名不能为空
        if (StrUtil.isBlank(tool.getToolCodeName())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "工具代码名不能为空");
        }

        // 工具代码名长度限制
        if (tool.getToolCodeName().length() < 2 || tool.getToolCodeName().length() > 100) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "工具代码名长度应在 2-100 个字符之间");
        }

        // 工具显示名不能为空
        if (StrUtil.isBlank(tool.getDisplayName())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "工具显示名不能为空");
        }

        // 输入参数 Schema 不能为空
        if (tool.getInputSchema() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "输入参数 Schema 不能为空");
        }

        // USER_HTTP 模式下 URL 必填
        if ("USER_HTTP".equalsIgnoreCase(tool.getSourceType())) {
            if (StrUtil.isBlank(tool.getCustomEndpointUrl())) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, 
                    "USER_HTTP 模式下自定义执行 URL 不能为空");
            }
        }

        // USER_MCP 模式下 MCP 服务器 ID 必填
        if ("USER_MCP".equalsIgnoreCase(tool.getSourceType())) {
            if (tool.getMcpServerId() == null) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, 
                    "USER_MCP 模式下关联 MCP 服务 ID 不能为空");
            }
        }
    }
}
