package com.esdllm.agentmesh.controller;

import com.esdllm.agentmesh.common.BaseResponse;
import com.esdllm.agentmesh.common.ResultUtils;
import com.esdllm.agentmesh.model.domain.McpServers;
import com.esdllm.agentmesh.model.domain.User;
import com.esdllm.agentmesh.service.McpServerService;
import com.esdllm.agentmesh.service.UserService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/mcp-server")
public class McpServerController {
    
    @Resource
    private McpServerService mcpServerService;
    @Resource
    private UserService userService;

    @PostMapping("/add")
    public BaseResponse<Long> addMcpServer(@RequestBody McpServers mcpServer, HttpSession session) {
        User loginUser = getLoginUser(session);
        Long serverId = mcpServerService.createMcpServer(mcpServer, loginUser.getId());
        return ResultUtils.success(serverId);
    }

    @PutMapping("/update")
    public BaseResponse<Boolean> updateMcpServer(@RequestBody McpServers mcpServer, HttpSession session) {
        User loginUser= getLoginUser(session);
        Boolean result = mcpServerService.updateMcpServer(mcpServer, loginUser.getId());
        return ResultUtils.success(result);
    }

    @DeleteMapping("/delete/{serverId}")
    public BaseResponse<Boolean> deleteMcpServer(@PathVariable Long serverId, HttpSession session) {
        User loginUser = getLoginUser(session);
        Boolean result = mcpServerService.deleteMcpServer(serverId, loginUser.getId());
        return ResultUtils.success(result);
    }

    @GetMapping("/my-list")
    public BaseResponse<List<McpServers>> getMyMcpServers(HttpSession session) {
        User loginUser = getLoginUser(session);
        List<McpServers> serverList = mcpServerService.getMyMcpServers(loginUser.getId());
        return ResultUtils.success(serverList);
    }

    @GetMapping("/{serverId}")
    public BaseResponse<McpServers> getMcpServer(@PathVariable Long serverId, HttpSession session) {
        User loginUser = getLoginUser(session);
        McpServers server = mcpServerService.getMcpServerById(serverId, loginUser.getId());
        return ResultUtils.success(server);
    }

    /**
     * 获取当前登录用户
     */
    private User getLoginUser(HttpSession session) {
        User userObj = userService.getLoginUser( session);
        if (userObj == null) {
            throw new com.esdllm.agentmesh.exception.BusinessException(
                com.esdllm.agentmesh.common.ErrorCode.NOT_LOGIN_ERROR);
        }
        return userObj;
    }
}
