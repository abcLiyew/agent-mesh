package com.esdllm.agentmesh.controller;

import com.esdllm.agentmesh.common.BaseResponse;
import com.esdllm.agentmesh.common.ErrorCode;
import com.esdllm.agentmesh.common.ResultUtils;
import com.esdllm.agentmesh.model.domain.User;
import com.esdllm.agentmesh.model.dto.DecisionExecutionResult;
import com.esdllm.agentmesh.model.dto.TaskExecutionPlan;
import com.esdllm.agentmesh.model.dto.WorkflowTemplate;
import com.esdllm.agentmesh.model.dto.unified.CollaborativeExecuteRequest;
import com.esdllm.agentmesh.model.dto.unified.TaskPlanRequest;
import com.esdllm.agentmesh.model.dto.unified.UnifiedAgentExecuteRequest;
import com.esdllm.agentmesh.service.UserService;
import com.esdllm.agentmesh.service.unified.UnifiedAgentEngine;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 统一智能体工作流控制器
 * 提供"龙虾"架构的统一入口，支持决策引擎和工作流引擎的融合调用
 */
@RestController
@RequestMapping("/api/unified-agent")
@Tag(name = "统一智能体引擎", description = "融合决策与工作流的智能体引擎（龙虾架构）")
@Slf4j
public class UnifiedAgentController {
    
    @Resource
    private UnifiedAgentEngine unifiedAgentEngine;
    
    @Resource
    private UserService userService;
    
    /**
     * 执行智能体工作流（同步）
     */
    @PostMapping("/execute")
    @Operation(summary = "执行智能体工作流", description = "支持自主决策或指定工作流执行")
    public BaseResponse<DecisionExecutionResult> execute(
            @RequestBody UnifiedAgentExecuteRequest request,
            HttpSession session) {
        
        User loginUser = userService.getLoginUser(session);
        if (loginUser == null) {
            return ResultUtils.error(ErrorCode.NOT_LOGIN);
        }
        
        log.info("统一引擎执行请求，agentId: {}, userId: {}, workflowId: {}", 
                request.getAgentId(), loginUser.getId(), request.getWorkflowId());
        
        DecisionExecutionResult result = unifiedAgentEngine.execute(
            request.getAgentId(), 
            request.getQuery(), 
            loginUser.getId(), 
            request.getWorkflowId(), 
            request.getContext()
        );
        
        return ResultUtils.success(result);
    }
    
    /**
     * 流式执行智能体工作流（SSE）
     */
    @GetMapping(value = "/execute-stream", produces = "text/event-stream")
    @Operation(summary = "流式执行智能体工作流", description = "支持实时推送执行进度和结果")
    public SseEmitter executeStream(
            @Parameter(description = "智能体ID") @RequestParam Long agentId,
            @Parameter(description = "用户查询") @RequestParam String query,
            @Parameter(description = "工作流ID（可选）") @RequestParam(required = false) Long workflowId,
            @Parameter(description = "会话ID（可选，用于多轮对话关联）") @RequestParam(required = false) String sessionId,
            @Parameter(description = "Session Token（可选，用于SSE认证）") @RequestParam(required = false) String token,
            HttpServletRequest request,
            HttpSession session) {
        
        User loginUser = null;
        
        // 优先从token获取用户，其次从session获取
        if (token != null && !token.isEmpty()) {
            // TODO: 根据token解析用户信息（可以使用JWT或Redis存储的token）
            // loginUser = userService.getUserByToken(token);
            log.warn("Token认证暂未实现，请使用Cookie方式");
        }
        
        // 从Session获取登录用户
        if (loginUser == null) {
            loginUser = userService.getLoginUser(session);
        }
        
        if (loginUser == null) {
            SseEmitter emitter = new SseEmitter();
            try {
                emitter.send(SseEmitter.event().name("error").data("未登录"));
                emitter.complete();
            } catch (Exception e) {
                log.error("发送错误消息失败", e);
            }
            return emitter;
        }
        
        SseEmitter emitter = new SseEmitter(300000L); // 5分钟超时
        
        log.info("统一引擎流式执行请求，agentId: {}, userId: {}, sessionId: {}", 
                agentId, loginUser.getId(), sessionId);
        
        unifiedAgentEngine.executeStream(agentId, query, loginUser.getId(), workflowId, null, emitter, sessionId);
        
        return emitter;
    }
    
    /**
     * 动态生成工作流
     */
    @PostMapping("/generate-workflow")
    @Operation(summary = "动态生成工作流", description = "基于任务描述自动生成工作流定义")
    public BaseResponse<Map<String, Object>> generateWorkflow(
            @RequestBody TaskPlanRequest request,
            HttpSession session) {
        
        User loginUser = userService.getLoginUser(session);
        if (loginUser == null) {
            return ResultUtils.error(ErrorCode.NOT_LOGIN);
        }
        
        log.info("动态生成工作流请求，agentId: {}, task: {}", request.getAgentId(), request.getQuery());
        
        Long workflowId = unifiedAgentEngine.generateWorkflow(
            request.getAgentId(), 
            request.getQuery(), 
            loginUser.getId()
        );
        
        Map<String, Object> response = new HashMap<>();
        response.put("workflowId", workflowId);
        response.put("message", "工作流生成成功");
        
        return ResultUtils.success(response);
    }
    
    /**
     * 规划任务（返回待办清单，不执行）
     */
    @PostMapping("/plan-task")
    @Operation(summary = "规划任务执行计划", description = "分析用户意图，拆解任务步骤，返回待办清单供用户确认。简短任务会直接执行并返回结果")
    public BaseResponse<Object> planTask(
            @RequestBody TaskPlanRequest request,
            HttpSession session) {
        
        User loginUser = userService.getLoginUser(session);
        if (loginUser == null) {
            return ResultUtils.error(ErrorCode.NOT_LOGIN);
        }
        
        log.info("任务规划请求，agentId: {}, userId: {}, query: {}, sessionId: {}", 
                request.getAgentId(), loginUser.getId(), request.getQuery(), request.getSessionId());
        
        if (request.getSessionId() == null || request.getSessionId().isEmpty()) {
            log.warn("⚠️ 前端未传入sessionId，后端将自动生成新的sessionId");
        } else {
            log.info("✅ 前端传入sessionId: {}", request.getSessionId());
        }
        
        // ✅ 先判断是否为简单任务
        boolean isSimple = unifiedAgentEngine.isSimpleTask(
            request.getAgentId(), 
            request.getQuery(), 
            loginUser.getId()
        );
        
        if (isSimple) {
            log.info("✅ 检测到简单任务，直接执行并返回结果");
            
            // 直接执行简单任务，返回流式结果
            DecisionExecutionResult result = unifiedAgentEngine.executeSimpleTask(
                request.getAgentId(),
                request.getQuery(),
                loginUser.getId(),
                request.getSessionId()
            );
            
            Map<String, Object> response = new HashMap<>();
            response.put("isSimpleTask", true);
            response.put("result", result);
            response.put("message", "简单任务已直接执行");
            
            return ResultUtils.success(response);
        }
        
        // 复杂任务：生成任务计划
        log.info("复杂任务，生成任务计划");
        TaskExecutionPlan plan = unifiedAgentEngine.planTask(
            request.getAgentId(), 
            request.getQuery(), 
            loginUser.getId(), 
            request.getContext(),
            request.getSessionId()  // ✅ 传递sessionId
        );
        
        Map<String, Object> response = new HashMap<>();
        response.put("isSimpleTask", false);
        response.put("plan", plan);
        
        return ResultUtils.success(response);
    }
    
    /**
     * 执行已确认的任务计划
     */
    @PostMapping("/execute-planned-task")
    @Operation(summary = "执行已确认的任务", description = "根据用户确认的步骤列表执行任务")
    public BaseResponse<DecisionExecutionResult> executePlannedTask(
            @Parameter(description = "任务ID") @RequestParam String taskId,
            @Parameter(description = "确认的步骤ID列表") @RequestBody(required = false) List<String> confirmedSteps,
            HttpSession session) {
        
        User loginUser = userService.getLoginUser(session);
        if (loginUser == null) {
            return ResultUtils.error(ErrorCode.NOT_LOGIN);
        }
        
        log.info("执行任务计划，taskId: {}, userId: {}, confirmedSteps: {}", 
                taskId, loginUser.getId(), confirmedSteps);
        
        // ✅ 从缓存中获取plan，提取sessionId
        TaskExecutionPlan plan = unifiedAgentEngine.getTaskPlanFromCache(taskId);
        String sessionId = null;
        if (plan != null) {
            log.info("✅ 从缓存中找到plan: taskId={}, context={}", taskId, plan.getContext());
            if (plan.getContext() != null && plan.getContext().containsKey("sessionId")) {
                sessionId = (String) plan.getContext().get("sessionId");
                log.info("✅ 提取到sessionId: {}", sessionId);
            } else {
                log.warn("⚠️ plan.context 为空或不包含sessionId");
            }
        } else {
            log.error("❌ 缓存中未找到plan: taskId={}", taskId);
        }
        
        DecisionExecutionResult result = unifiedAgentEngine.executePlannedTask(
            taskId, confirmedSteps, loginUser.getId(), sessionId
        );
        
        return ResultUtils.success(result);
    }
    
    /**
     * 创建工作流模板
     */
    @PostMapping("/workflow-template/create")
    @Operation(summary = "创建工作流模板", description = "支持全自动、半自定义、完全自定义三种模式")
    public BaseResponse<Map<String, Object>> createWorkflowTemplate(
            @RequestBody WorkflowTemplate template,
            HttpSession session) {
        
        User loginUser = userService.getLoginUser(session);
        if (loginUser == null) {
            return ResultUtils.error(ErrorCode.NOT_LOGIN);
        }
        
        template.setUserId(loginUser.getId());
        Long templateId = unifiedAgentEngine.createWorkflowTemplate(template);
        
        Map<String, Object> response = new HashMap<>();
        response.put("templateId", templateId);
        response.put("message", "工作流模板创建成功");
        
        return ResultUtils.success(response);
    }
    
    /**
     * AI辅助生成工作流（半自定义）
     */
    @PostMapping("/workflow-template/ai-assist")
    @Operation(summary = "AI辅助生成工作流", description = "用户提供部分节点，AI自动补充缺失部分")
    public BaseResponse<WorkflowTemplate> aiAssistWorkflow(
            @RequestBody WorkflowTemplate request,
            HttpSession session) {
        
        User loginUser = userService.getLoginUser(session);
        if (loginUser == null) {
            return ResultUtils.error(ErrorCode.NOT_LOGIN);
        }
        
        log.info("AI辅助生成工作流，agentId: {}, task: {}", request.getAgentId(), request.getTaskDescription());
        
        WorkflowTemplate template = unifiedAgentEngine.aiAssistWorkflow(
            request.getAgentId(), 
            request.getTaskDescription(), 
            request.getUserDefinedNodes(), 
            loginUser.getId()
        );
        
        return ResultUtils.success(template);
    }
    
    /**
     * 获取工作流模板列表
     */
    @GetMapping("/workflow-template/list")
    @Operation(summary = "获取工作流模板列表", description = "支持按模式过滤")
    public BaseResponse<List<WorkflowTemplate>> getWorkflowTemplates(
            @Parameter(description = "工作流模式") @RequestParam(required = false) String mode,
            HttpSession session) {
        
        User loginUser = userService.getLoginUser(session);
        if (loginUser == null) {
            return ResultUtils.error(ErrorCode.NOT_LOGIN);
        }
        
        List<WorkflowTemplate> templates = unifiedAgentEngine.getWorkflowTemplates(
            loginUser.getId(), mode
        );
        
        return ResultUtils.success(templates);
    }
    
    /**
     * 获取工作流模板列表（兼容复数路径）
     */
    @GetMapping("/workflow-templates")
    @Operation(summary = "获取工作流模板列表", description = "支持按模式过滤（兼容复数路径）")
    public BaseResponse<List<WorkflowTemplate>> getWorkflowTemplatesPlural(
            @Parameter(description = "工作流模式") @RequestParam(required = false) String mode,
            HttpSession session) {
        
        User loginUser = userService.getLoginUser(session);
        if (loginUser == null) {
            return ResultUtils.error(ErrorCode.NOT_LOGIN);
        }
        
        List<WorkflowTemplate> templates = unifiedAgentEngine.getWorkflowTemplates(
            loginUser.getId(), mode
        );
        
        return ResultUtils.success(templates);
    }
    
    /**
     * 基于模板执行工作流
     */
    @PostMapping("/workflow-template/execute/{templateId}")
    @Operation(summary = "基于模板执行工作流", description = "使用预定义的工作流模板执行任务")
    public BaseResponse<DecisionExecutionResult> executeFromTemplate(
            @Parameter(description = "模板ID") @PathVariable Long templateId,
            @Parameter(description = "输入参数") @RequestBody(required = false) Map<String, Object> inputParams,
            HttpSession session) {
        
        User loginUser = userService.getLoginUser(session);
        if (loginUser == null) {
            return ResultUtils.error(ErrorCode.NOT_LOGIN);
        }
        
        log.info("基于模板执行工作流，templateId: {}, userId: {}", templateId, loginUser.getId());
        
        DecisionExecutionResult result = unifiedAgentEngine.executeFromTemplate(
            templateId, inputParams, loginUser.getId()
        );
        
        return ResultUtils.success(result);
    }
    
    /**
     * 多智能体协同执行
     */
    @PostMapping("/execute-collaborative")
    @Operation(summary = "多智能体协同执行", description = "自动协调多个智能体共同完成复杂任务")
    public BaseResponse<Object> executeCollaboratively(
            @RequestBody CollaborativeExecuteRequest request,
            HttpSession session) {
        
        User loginUser = userService.getLoginUser(session);
        if (loginUser == null) {
            return ResultUtils.error(ErrorCode.NOT_LOGIN);
        }
        
        log.info("多智能体协同执行请求，agentId: {}, userId: {}, query: {}", 
                request.getAgentId(), loginUser.getId(), request.getQuery());
        
        Object result = unifiedAgentEngine.executeCollaboratively(
            request.getAgentId(), 
            request.getQuery(), 
            loginUser.getId(), 
            request.getContext()
        );
        
        return ResultUtils.success(result);
    }
}
