package com.esdllm.agentmesh.controller;

import com.esdllm.agentmesh.common.BaseResponse;
import com.esdllm.agentmesh.common.ResultUtils;
import com.esdllm.agentmesh.model.domain.User;
import com.esdllm.agentmesh.service.AgentDependency;
import com.esdllm.agentmesh.service.AgentNetworkService;
import com.esdllm.agentmesh.service.UserService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 智能体网络管理 Controller
 */
@RestController
@RequestMapping("/api/agent-network")
@Slf4j
public class AgentNetworkController {

    @Resource
  private AgentNetworkService agentNetworkService;

    @Resource
  private UserService userService;

    /**
     * 添加智能体依赖关系
     */
    @PostMapping("/dependency/add")
   public BaseResponse<Boolean> addAgentDependency(
        @RequestBody Map<String, Object> request,
        HttpServletRequest httpRequest
    ) {
       User loginUser= userService.getLoginUser(httpRequest.getSession());
        
       Long agentId = Long.valueOf(request.get("agentId").toString());
       Long dependsOnAgentId = Long.valueOf(request.get("dependsOnAgentId").toString());
    String dependencyType = (String) request.getOrDefault("dependencyType", "CALL");
       Integer priority = (Integer) request.getOrDefault("priority", 0);

       agentNetworkService.addAgentDependency(agentId, dependsOnAgentId, 
                                           dependencyType, priority, loginUser.getId());
        
        return ResultUtils.success(true);
    }

    /**
     * 移除智能体依赖关系
     */
    @DeleteMapping("/dependency/remove")
   public BaseResponse<Boolean> removeAgentDependency(
        @RequestParam Long agentId,
        @RequestParam Long dependsOnAgentId,
        HttpServletRequest httpRequest
    ) {
       User loginUser= userService.getLoginUser(httpRequest.getSession());
       
       agentNetworkService.removeAgentDependency(agentId, dependsOnAgentId, loginUser.getId());
        return ResultUtils.success(true);
    }

    /**
     * 获取智能体的依赖列表
     */
    @GetMapping("/dependencies/{agentId}")
   public BaseResponse<List<AgentDependency>> getAgentDependencies(
        @PathVariable Long agentId
    ) {
       List<AgentDependency> dependencies = agentNetworkService.getAgentDependencies(agentId);
        return ResultUtils.success(dependencies);
    }

    /**
     * 检测循环依赖
     */
    @GetMapping("/check-cycle/{agentId}")
   public BaseResponse<Map<String, Boolean>> checkCircularDependency(
        @PathVariable Long agentId
    ) {
       boolean hasCycle = agentNetworkService.hasCircularDependency(agentId);
        
       Map<String, Boolean> result = new HashMap<>();
        result.put("hasCircularDependency", hasCycle);
        
        return ResultUtils.success(result);
    }

    /**
     * 获取智能体网络拓扑图
     */
    @GetMapping("/topology")
   public BaseResponse<Object> getNetworkTopology(HttpServletRequest httpRequest) {
       User loginUser= userService.getLoginUser(httpRequest.getSession());
       
       Object topology = agentNetworkService.getNetworkTopology(loginUser.getId());
        return ResultUtils.success(topology);
    }
}
