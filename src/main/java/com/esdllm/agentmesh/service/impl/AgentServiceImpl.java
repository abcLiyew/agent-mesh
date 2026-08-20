package com.esdllm.agentmesh.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.esdllm.agentmesh.common.ErrorCode;
import com.esdllm.agentmesh.exception.BusinessException;
import com.esdllm.agentmesh.model.domain.Agent;
import com.esdllm.agentmesh.model.dto.request.AgentAddRequest;
import com.esdllm.agentmesh.model.dto.response.AgentResponse;
import com.esdllm.agentmesh.model.dto.tool.ToolSchemaConfig;
import com.esdllm.agentmesh.model.domain.User;
import com.esdllm.agentmesh.repository.dao.AgentDao;
import com.esdllm.agentmesh.service.AgentService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 智能体服务实现类
 */
@Service
@Slf4j
public class AgentServiceImpl implements AgentService {

    @Resource
    private AgentDao agentDao;

    @Transactional(rollbackFor = Exception.class)
    @Override
    public Long addAgent(AgentAddRequest request, User loginUser) {
        // 1. 基础参数校验
        validateBasicParams(request);

        // 2. 验证并处理工具配置
        if (request.getToolSchemaJson() != null) {
            validateAndProcessToolSchema(request.getToolSchemaJson());
        }

        // 3. 转换为实体对象
        Agent agent = convertToEntity(request, loginUser);

        // 4. 保存到数据库
        boolean saved = agentDao.save(agent);
        if (!saved) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "创建智能体失败");
        }

        return agent.getId();
    }

    @Override
    public List<AgentResponse> getAgentListByPage(int page, int pageSize) {
        List<Agent> agentList = agentDao.getAgentListBypage(page, pageSize);
        return agentList.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public AgentResponse getAgentById(Long id) {
        Agent agent = agentDao.getById(id);
        if (agent == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "智能体不存在");
        }
        return convertToResponse(agent);
    }

    @Override
    public Long getAgentNum() {
        return agentDao.getAgentNum();
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public Boolean updateAgent(Long id, AgentAddRequest request, User loginUser) {
        if (id == null || request == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数不能为空");
        }

        // 查询智能体是否存在
        Agent existingAgent = agentDao.getById(id);
        if (existingAgent == null) {
            throw new BusinessException(ErrorCode.AGENT_NOT_FOUND, "智能体不存在");
        }

        // 验证权限
        if (!existingAgent.getUserId().equals(loginUser.getId())) {
            throw new BusinessException(ErrorCode.NO_AUTH, "无权限修改该智能体");
        }

        // 基础参数校验
        validateBasicParams(request);

        // 验证并处理工具配置
        if (request.getToolSchemaJson() != null) {
            validateAndProcessToolSchema(request.getToolSchemaJson());
        }

        // 更新智能体信息
        Agent agent = convertToEntity(request, loginUser);
        agent.setId(id);
        agent.setUserId(existingAgent.getUserId()); // 保持原有归属用户

        boolean updated = agentDao.updateById(agent);
        if (!updated) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "更新智能体失败");
        }

        log.info("更新智能体成功，agentId: {}, userId: {}", agent.getId(), loginUser.getId());
        return true;
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public Boolean deleteAgent(Long id, User loginUser) {
        if (id == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "智能体 ID 不能为空");
        }

        // 查询智能体是否存在
        Agent existingAgent = agentDao.getById(id);
        if (existingAgent == null) {
            throw new BusinessException(ErrorCode.AGENT_NOT_FOUND, "智能体不存在");
        }

        // 验证权限
        if (!existingAgent.getUserId().equals(loginUser.getId())) {
            throw new BusinessException(ErrorCode.NO_AUTH, "无权限删除该智能体");
        }

        // 使用 MyBatis-Plus 的逻辑删除
        boolean deleted = agentDao.removeById(id);
        if (!deleted) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "删除智能体失败");
        }

        log.info("删除智能体成功，agentId: {}, userId: {}", id, loginUser.getId());
        return true;
    }

    @Override
    public List<AgentResponse> getMyAgents(Long userId, int page, int pageSize) {
        if (userId == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户 ID 不能为空");
        }

        Page<Agent> resultPage = agentDao.getMyAgentsPage(userId, page, pageSize);

        return resultPage.getRecords().stream()
                .map(this::convertToResponse)
                .toList();
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public Boolean updateAgentStatus(Long id, Integer status, User loginUser) {
        if (id == null || status == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数不能为空");
        }

        // 查询智能体是否存在
        Agent existingAgent = agentDao.getById(id);
        if (existingAgent == null) {
            throw new BusinessException(ErrorCode.AGENT_NOT_FOUND, "智能体不存在");
        }

        // 验证权限
        if (!existingAgent.getUserId().equals(loginUser.getId())) {
            throw new BusinessException(ErrorCode.NO_AUTH, "无权限修改该智能体");
        }

        // 更新状态
        Agent agent = new Agent();
        agent.setId(id);
        agent.setStatus(status);
        agent.setUpdatedAt(new Date());

        boolean updated = agentDao.updateById(agent);
        if (!updated) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "更新状态失败");
        }

        log.info("更新智能体状态成功，agentId: {}, status: {}", id, status);
        return true;
    }

    @Override
    public List<AgentResponse> searchAgents(String keyword, int page, int pageSize) {
        if (StrUtil.isBlank(keyword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "搜索关键词不能为空");
        }

        Page<Agent> resultPage = agentDao.searchAgentsPage(keyword, page, pageSize);

        return resultPage.getRecords().stream()
                .map(this::convertToResponse)
                .toList();
    }

    @Override
    public List<AgentResponse> getPublicAgents(int page, int pageSize) {
        Page<Agent> resultPage = agentDao.getPublicAgentsPage(page, pageSize);

        return resultPage.getRecords().stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    /**
     * 转换为响应对象
     */
    private AgentResponse convertToResponse(Agent agent) {
        AgentResponse response = new AgentResponse();
        response.setId(agent.getId());
        response.setUserId(agent.getUserId());  // 添加 userId
        response.setName(agent.getName());
        response.setDescription(agent.getDescription());
        response.setAvatarUrl(agent.getAvatarUrl());
        response.setSystemPrompt(agent.getSystemPrompt());
        response.setRoleDefinition(agent.getRoleDefinition());
        response.setDecisionModelId(agent.getDecisionModelId());
        response.setResponseModelId(agent.getResponseModelId());
        response.setIsToolEnabled(agent.getIsToolEnabled());
        response.setVersion(agent.getVersion());
        response.setStatus(agent.getStatus());
        response.setCreatedAt(agent.getCreatedAt());
        response.setUpdatedAt(agent.getUpdatedAt());


        return response;
    }

    /**
     * 校验基础参数
     */
    private void validateBasicParams(AgentAddRequest request) {
        if (StrUtil.isBlank(request.getName())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "智能体名称不能为空");
        }

        if (request.getName().length() < 2 || request.getName().length() > 50) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "智能体名称长度应在 2-50 个字符之间");
        }

        if (request.getDecisionModelId() == null && request.getResponseModelId() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "至少需要配置一个模型（决策模型或回复模型）");
        }
    }

    /**
     * 验证并处理工具配置
     */
    private void validateAndProcessToolSchema(ToolSchemaConfig config) {
        // 验证工具列表
        if (config.getTools() != null && !config.getTools().isEmpty()) {
            for (ToolSchemaConfig.ToolDefinition tool : config.getTools()) {
                validateSingleTool(tool);
            }
        }

        // 验证全局配置
        if (config.getGlobalConfig() != null) {
            validateGlobalConfig(config.getGlobalConfig());
        }
    }

    /**
     * 验证单个工具配置
     */
    private void validateSingleTool(ToolSchemaConfig.ToolDefinition tool) {
        // 工具名称不能为空
        if (StrUtil.isBlank(tool.getName())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "工具名称不能为空");
        }

        // 验证参数配置
        if (tool.getParameters() != null) {
            validateToolParameters(tool.getParameters());
        }

        // 验证调用配置
        if (tool.getInvocationConfig() != null) {
            validateInvocationConfig(tool.getInvocationConfig());
        }

        // 验证高级配置
        if (tool.getAdvancedConfig() != null) {
            validateAdvancedConfig(tool.getAdvancedConfig());
        }
    }

    /**
     * 验证工具参数
     */
    private void validateToolParameters(ToolSchemaConfig.ToolParameters params) {
        if (params.getProperties() != null) {
            for (Map.Entry<String, ToolSchemaConfig.ParameterProperty> entry : params.getProperties().entrySet()) {
                ToolSchemaConfig.ParameterProperty prop = entry.getValue();

                // 验证类型
                if (StrUtil.isBlank(prop.getType())) {
                    throw new BusinessException(ErrorCode.PARAMS_ERROR,
                            "参数 '" + entry.getKey() + "' 的类型不能为空");
                }

                // 验证字符串长度限制
                if ("string".equals(prop.getType())) {
                    if (prop.getMinLength() != null && prop.getMinLength() < 0) {
                        throw new BusinessException(ErrorCode.PARAMS_ERROR,
                                "参数 '" + entry.getKey() + "' 的最小长度不能为负数");
                    }
                }

                // 验证数字范围
                if (Arrays.asList("integer", "number").contains(prop.getType())) {
                    if (prop.getMinimum() != null && prop.getMaximum() != null) {
                        if (prop.getMinimum().doubleValue() > prop.getMaximum().doubleValue()) {
                            throw new BusinessException(ErrorCode.PARAMS_ERROR,
                                    "参数 '" + entry.getKey() + "' 的最小值不能大于最大值");
                        }
                    }
                }
            }
        }
    }

    /**
     * 验证调用配置
     */
    private void validateInvocationConfig(ToolSchemaConfig.InvocationConfig config) {
        if (StrUtil.isNotBlank(config.getMethod())) {
            List<String> validMethods = Arrays.asList("HTTP", "RPC", "FUNCTION", "MCP");
            if (!validMethods.contains(config.getMethod().toUpperCase())) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR,
                        "无效的调用方法：" + config.getMethod());
            }
        }

        if (config.getTimeoutMs() != null && config.getTimeoutMs() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "超时时间必须大于 0");
        }

        if (config.getRetryTimes() != null && config.getRetryTimes() < 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "重试次数不能为负数");
        }
    }

    /**
     * 验证高级配置
     */
    private void validateAdvancedConfig(ToolSchemaConfig.AdvancedConfig config) {
        if (config.getCacheConfig() != null) {
            ToolSchemaConfig.CacheConfig cache = config.getCacheConfig();
            if (cache.getEnabled() && cache.getTtlSeconds() != null && cache.getTtlSeconds() <= 0) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "缓存过期时间必须大于 0");
            }
        }

        if (config.getRateLimitConfig() != null) {
            ToolSchemaConfig.RateLimitConfig limit = config.getRateLimitConfig();
            if (limit.getEnabled()) {
                if (limit.getMaxCallsPerMinute() != null && limit.getMaxCallsPerMinute() <= 0) {
                    throw new BusinessException(ErrorCode.PARAMS_ERROR, "每分钟最大调用次数必须大于 0");
                }
            }
        }
    }

    /**
     * 验证全局配置
     */
    private void validateGlobalConfig(ToolSchemaConfig.GlobalToolConfig config) {
        if (config.getDefaultTimeoutMs() != null && config.getDefaultTimeoutMs() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "默认超时时间必须大于 0");
        }

        if (config.getMaxConcurrentCalls() != null && config.getMaxConcurrentCalls() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "最大并发数必须大于 0");
        }

        if (StrUtil.isNotBlank(config.getFailureStrategy())) {
            List<String> validStrategies = Arrays.asList("FAIL_FAST", "CONTINUE", "RETRY");
            if (!validStrategies.contains(config.getFailureStrategy().toUpperCase())) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR,
                        "无效的失败策略：" + config.getFailureStrategy());
            }
        }
    }

    /**
     * 将请求对象转换为实体对象
     */
    private Agent convertToEntity(AgentAddRequest request, User loginUser) {
        Agent agent = new Agent();
        agent.setUserId(loginUser.getId());
        agent.setName(request.getName());
        agent.setDescription(request.getDescription());
        agent.setAvatarUrl(request.getAvatarUrl());
        agent.setSystemPrompt(request.getSystemPrompt());
        agent.setRoleDefinition(request.getRoleDefinition());
        agent.setDecisionModelId(request.getDecisionModelId());
        agent.setResponseModelId(request.getResponseModelId());
        agent.setIsToolEnabled(request.getIsToolEnabled());

        agent.setToolSchemaJson(request.getToolSchemaJson());

        agent.setVersion(request.getVersion());
        agent.setStatus(request.getStatus());
        agent.setCreatedAt(new Date());
        agent.setUpdatedAt(new Date());

        return agent;
    }
}

