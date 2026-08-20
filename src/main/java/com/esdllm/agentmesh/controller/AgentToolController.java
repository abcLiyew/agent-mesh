package com.esdllm.agentmesh.controller;

import com.esdllm.agentmesh.common.BaseResponse;
import com.esdllm.agentmesh.common.ErrorCode;
import com.esdllm.agentmesh.common.ResultUtils;
import com.esdllm.agentmesh.model.domain.Tools;
import com.esdllm.agentmesh.model.domain.User;
import com.esdllm.agentmesh.service.AgentToolService;
import com.esdllm.agentmesh.service.UserService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 智能体工具控制器
 */
@RestController
@RequestMapping("/api/agent-tool")
@Slf4j
public class AgentToolController {

    @Resource
    private AgentToolService agentToolService;

    @Resource
    private UserService userService;

    /**
     * 获取智能体工具列表
     * @param session HTTP 会话
     * @return 工具列表
     */
    @GetMapping("/list")
    public BaseResponse<List<Tools>> getAgentToolList(HttpSession session) {
        User loginUser = userService.getLoginUser(session);
        if (loginUser == null) {
            return ResultUtils.error(ErrorCode.NOT_LOGIN);
        }
        
        try {
            List<Tools> toolList = agentToolService.getAgentTools(loginUser.getId());
            return ResultUtils.success(toolList);
        } catch (Exception e) {
            log.error("获取智能体工具列表失败", e);
            return ResultUtils.error(ErrorCode.SYSTEM_ERROR, "获取工具列表失败：" + e.getMessage());
        }
    }
}
