package com.esdllm.agentmesh.controller;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.esdllm.agentmesh.common.BaseResponse;
import com.esdllm.agentmesh.common.ErrorCode;
import com.esdllm.agentmesh.common.ResultUtils;
import com.esdllm.agentmesh.exception.BusinessException;
import com.esdllm.agentmesh.model.domain.Agent;
import com.esdllm.agentmesh.model.domain.ConversationLog;
import com.esdllm.agentmesh.model.domain.User;
import com.esdllm.agentmesh.model.dto.request.AgentAddRequest;
import com.esdllm.agentmesh.model.dto.request.BatchUpdateAgentsRequest;
import com.esdllm.agentmesh.model.dto.response.AgentResponse;
import com.esdllm.agentmesh.model.dto.tool.ToolSchemaConfig;
import com.esdllm.agentmesh.repository.dao.AgentDao;
import com.esdllm.agentmesh.service.AgentService;
import com.esdllm.agentmesh.service.BatchOperationService;
import com.esdllm.agentmesh.service.ConversationLogService;
import com.esdllm.agentmesh.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/agent")
@Slf4j
public class AgentController {
    @Resource
    private AgentService agentService;
    @Resource
    private UserService userService;
    @Resource
    private BatchOperationService batchOperationService;
    @Resource
    private ConversationLogService conversationLogService;
    @Resource
    private AgentDao agentDao;

    private static final int pageSize = 20;
    @GetMapping("/list")
    public BaseResponse<List<AgentResponse>> getAgentList(HttpSession session, int page) {
        User loginUser = userService.getLoginUser(session);
        if (ObjectUtil.isEmpty(loginUser)){
            return ResultUtils.success(agentService.getAgentListByPage(page,15));
        }
        List<AgentResponse> result = agentService.getAgentListByPage(page,pageSize);
        return ResultUtils.success(result);
    }
    @GetMapping("/agentNum")
    public BaseResponse<Long> getAgentNum() {
        Long result = agentService.getAgentNum();
        return ResultUtils.success(result);
    }

    /**
     * 创建智能体
     */
    @PostMapping("/add")
    public BaseResponse<Long> addAgent(@RequestBody AgentAddRequest request,
                                       HttpServletRequest httpRequest) {
        try {
            User loginUser = userService.getLoginUser(httpRequest.getSession());
            if (ObjectUtil.isEmpty(loginUser)) {
                throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
            }
            Long agentId = agentService.addAgent(request, loginUser);
            return ResultUtils.success(agentId);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("创建智能体失败", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "创建失败：" + e.getMessage());
        }
    }

    /**
     * 获取智能体详情（包含解析后的工具配置和历史对话统计）
     */
    @GetMapping("/{id}")
    public BaseResponse<Map<String, Object>> getAgent(@PathVariable Long id) {
        AgentResponse agent = agentService.getAgentById(id);
        if (agent == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "智能体不存在");
        }

        AgentResponse vo = BeanUtil.copyProperties(agent, AgentResponse.class);

        // 解析工具配置
        if (StringUtils.isNotBlank(agent.getToolSchemaJson())) {
            try {
                ObjectMapper mapper = new ObjectMapper();
                ToolSchemaConfig config = mapper.readValue(
                        agent.getToolSchemaJson(),
                        ToolSchemaConfig.class
                );
                vo.setToolSchemaConfig(config);
            } catch (Exception e) {
                log.warn("解析工具配置失败", e);
            }
        }

        // 构建响应数据
        Map<String, Object> result = new HashMap<>();
        result.put("agent", vo);

        // 添加历史对话统计信息
        try {
            // 检查 userId 是否有效
            if (agent.getUserId() == null) {
                log.warn("智能体 {} 的 userId 为空，跳过统计信息查询", id);
                result.put("conversationStats", new HashMap<>());
            } else {
                // 最近 7 天的统计数据
                Date endDate = new Date();
                Date startDate = new Date(System.currentTimeMillis() - 7 * 24L * 60 * 60 * 1000);
                
                var stats = conversationLogService.getStatistics(
                    agent.getUserId(), startDate, endDate
                );
                
                // 过滤出该智能体的统计
                Map<String, Object> conversationStats = new HashMap<>();
                conversationStats.put("totalConversations", stats.getTotalConversations());
                conversationStats.put("successRate", stats.getSuccessRate());
                conversationStats.put("averageResponseTime", stats.getAverageResponseTime());
                conversationStats.put("totalCost", stats.getTotalCost());
                
                result.put("conversationStats", conversationStats);
            }
        } catch (Exception e) {
            log.warn("获取对话统计失败", e);
            result.put("conversationStats", new HashMap<>());
        }

        return ResultUtils.success(result);
    }
    /**
     * 更新智能体
     */
    @PutMapping("/{id}")
    public BaseResponse<Boolean> updateAgent(
            @PathVariable Long id,
            @RequestBody AgentAddRequest request,
            HttpServletRequest httpRequest
    ) {
        try {
            User loginUser = userService.getLoginUser(httpRequest.getSession());
            if (loginUser == null) {
                throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
            }

            Boolean result = agentService.updateAgent(id, request, loginUser);
            return ResultUtils.success(result);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("更新智能体失败", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "更新失败：" + e.getMessage());
        }
    }

    /**
     * 删除智能体（逻辑删除）
     */
    @DeleteMapping("/{id}")
    public BaseResponse<Boolean> deleteAgent(
            @PathVariable Long id,
            HttpServletRequest httpRequest
    ) {
        try {
            User loginUser= userService.getLoginUser(httpRequest.getSession());
            if (loginUser == null) {
                throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
            }

            Boolean result = agentService.deleteAgent(id, loginUser);
            return ResultUtils.success(result);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("删除智能体失败", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "删除失败：" + e.getMessage());
        }
    }

    /**
     * 获取我的智能体列表
     */
    @GetMapping("/my/list")
    public BaseResponse<List<AgentResponse>> getMyAgents(
            HttpSession session,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize
    ) {
        User loginUser= userService.getLoginUser(session);
        if (loginUser == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }

        List<AgentResponse> agentList = agentService.getMyAgents(loginUser.getId(), page, pageSize);
        return ResultUtils.success(agentList);
    }

    /**
     * 发布/下架智能体
     */
    @PutMapping("/{id}/status")
    public BaseResponse<Boolean> updateAgentStatus(
            @PathVariable Long id,
            @RequestParam Integer status,
            HttpServletRequest httpRequest
    ) {
        try {
            User loginUser = userService.getLoginUser(httpRequest.getSession());
            if (loginUser == null) {
                throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
            }

            if (status != 0 && status != 1) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "状态值只能为 0 或 1");
            }

            Boolean result = agentService.updateAgentStatus(id, status, loginUser);
            return ResultUtils.success(result);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("更新智能体状态失败", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "更新失败：" + e.getMessage());
        }
    }

    /**
     * 根据名称搜索智能体
     */
    @GetMapping("/search")
    public BaseResponse<List<AgentResponse>> searchAgents(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize
    ) {
        if (StringUtils.isBlank(keyword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "搜索关键词不能为空");
        }

        List<AgentResponse> agentList = agentService.searchAgents(keyword, page, pageSize);
        return ResultUtils.success(agentList);
    }
    @PostMapping("/batch-update")
    public BaseResponse<Map<String, Object>> batchUpdateAgents(
            @RequestBody BatchUpdateAgentsRequest request,
            HttpServletRequest httpRequest
    ) {
        try {
            User loginUser = userService.getLoginUser(httpRequest.getSession());

            Map<String, Object> result = new HashMap<>();
            var updateResult = batchOperationService.batchUpdateAgents(
                    request.getAgentIds(),
                    request.getUpdateConfig(),
                    loginUser.getId()
            );

            result.put("successCount", updateResult.successCount());
            result.put("failCount", updateResult.failCount());
            result.put("errorMessages", updateResult.errorMessages());

            return ResultUtils.success(result);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("批量更新智能体失败", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "批量更新失败：" + e.getMessage());
        }
    }

    /**
     * 获取已发布已公开的智能体列表
     */
    @GetMapping("/public/list")
    public BaseResponse<List<AgentResponse>> getPublicAgents(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize
    ) {
        List<AgentResponse> agentList = agentService.getPublicAgents(page, pageSize);
        return ResultUtils.success(agentList);
    }

    /**
     * 获取智能体的历史对话列表
     */
    @GetMapping("/{id}/conversations")
    public BaseResponse<Page<ConversationLog>> getAgentConversations(
            @PathVariable Long id,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            HttpServletRequest request
    ) {
        User loginUser = userService.getLoginUser(request.getSession());
        if (loginUser == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }

        // 检查智能体是否存在（使用 Agent 实体类以获取完整字段）
        Agent agent = agentDao.getById(id);
        if (agent == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "智能体不存在");
        }

        // 权限检查：智能体所有者 或 已发布的智能体
        boolean isOwner = agent.getUserId().equals(loginUser.getId());
        boolean isPublished = agent.getStatus() != null && agent.getStatus() == 1;
        
        if (!isOwner && !isPublished) {
            throw new BusinessException(ErrorCode.NO_AUTH, "无权查看该智能体的对话记录");
        }

        // 查询当前用户与该智能体的对话记录
        Page<ConversationLog> logs = conversationLogService.getUserAgentConversationLogs(
                loginUser.getId(), id, page, pageSize
        );

        return ResultUtils.success(logs);
    }

}
