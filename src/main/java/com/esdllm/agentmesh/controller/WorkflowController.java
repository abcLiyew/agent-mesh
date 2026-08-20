package com.esdllm.agentmesh.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.esdllm.agentmesh.common.BaseResponse;
import com.esdllm.agentmesh.common.ErrorCode;
import com.esdllm.agentmesh.common.ResultUtils;
import com.esdllm.agentmesh.model.domain.User;
import com.esdllm.agentmesh.model.domain.WorkflowDefinitionEntity;
import com.esdllm.agentmesh.model.dto.workflow.WorkflowDefinition;
import com.esdllm.agentmesh.model.dto.workflow.WorkflowExecutionResult;
import com.esdllm.agentmesh.service.UserService;
import com.esdllm.agentmesh.service.WorkflowDefinitionService;
import com.esdllm.agentmesh.service.workflow.impl.OrderProcessingWorkflowService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 工作流控制器 - 演示复杂工作流执行
 */
@RestController
@RequestMapping("/api/workflow")
@Tag(name = "工作流管理", description = "工作流定义与执行接口")
@Slf4j
public class WorkflowController {
    
    @Resource
    private OrderProcessingWorkflowService orderProcessingWorkflowService;
    
    @Resource
    private UserService userService;
    
    @Resource
    private WorkflowDefinitionService workflowDefinitionService;
    
    /**
     * 执行订单退款工作流
     * 
     * @param orderId 订单ID
     * @param refundReason 退款原因
     * @return 执行结果
     */
    @PostMapping("/order-refund")
    @Operation(summary = "执行订单退款工作流", description = "演示复杂的工作流编排:条件分支、顺序执行、并行处理")
    public BaseResponse<WorkflowExecutionResult> processOrderRefund(
            @RequestParam String orderId,
            @RequestParam(required = false) String refundReason,
            HttpSession session) {
        
        User loginUser = userService.getLoginUser(session);
        if (loginUser == null) {
            return ResultUtils.error(ErrorCode.NOT_LOGIN_ERROR);
        }
        
        log.info("收到订单退款请求, orderId: {}, userId: {}", orderId, loginUser.getId());
        
        try {
            WorkflowExecutionResult result = orderProcessingWorkflowService.processOrderRefund(
                    orderId, loginUser.getId(), refundReason != null ? refundReason : "用户申请"
            );
            
            return ResultUtils.success(result);
            
        } catch (Exception e) {
            log.error("订单退款工作流执行失败", e);
            return ResultUtils.error(ErrorCode.SYSTEM_ERROR, "工作流执行失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取订单处理工作流定义
     * 
     * @return 工作流定义
     */
    @GetMapping("/order-processing/definition")
    @Operation(summary = "获取订单处理工作流定义", description = "查看工作流的节点结构和执行逻辑")
    public BaseResponse<WorkflowDefinition> getOrderProcessingWorkflow() {
        try {
            WorkflowDefinition workflow = orderProcessingWorkflowService.createOrderProcessingWorkflow();
            return ResultUtils.success(workflow);
        } catch (Exception e) {
            log.error("获取工作流定义失败", e);
            return ResultUtils.error(ErrorCode.SYSTEM_ERROR, "获取工作流定义失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取并行查询工作流定义
     * 
     * @return 工作流定义
     */
    @GetMapping("/parallel-query/definition")
    @Operation(summary = "获取并行查询工作流定义", description = "演示并行执行多个任务的工作流")
    public BaseResponse<WorkflowDefinition> getParallelQueryWorkflow() {
        try {
            WorkflowDefinition workflow = orderProcessingWorkflowService.createParallelDataQueryWorkflow();
            return ResultUtils.success(workflow);
        } catch (Exception e) {
            log.error("获取工作流定义失败", e);
            return ResultUtils.error(ErrorCode.SYSTEM_ERROR, "获取工作流定义失败: " + e.getMessage());
        }
    }
    
    /**
     * 执行自定义工作流
     * 
     * @param workflowDefinition 工作流定义
     * @param inputParams 输入参数
     * @return 执行结果
     */
    @PostMapping("/execute")
    @Operation(summary = "执行自定义工作流", description = "传入工作流定义和参数,动态执行")
    public BaseResponse<WorkflowExecutionResult> executeCustomWorkflow(
            @RequestBody WorkflowDefinition workflowDefinition,
            @RequestParam Map<String, Object> inputParams,
            HttpSession session) {
        
        User loginUser = userService.getLoginUser(session);
        if (loginUser == null) {
            return ResultUtils.error(ErrorCode.NOT_LOGIN_ERROR);
        }
        
        log.info("收到自定义工作流执行请求, workflowName: {}, userId: {}", 
                workflowDefinition.getWorkflowName(), loginUser.getId());
        
        // TODO: 注入 WorkflowEngine 并执行
        // WorkflowExecutionResult result = workflowEngine.execute(workflowDefinition, inputParams, loginUser.getId());
        
        Map<String, Object> mockResult = new HashMap<>();
        mockResult.put("message", "工作流执行功能待集成到决策引擎");
        mockResult.put("workflowId", workflowDefinition.getWorkflowId());
        
        return ResultUtils.success(null);
    }
    
    // ==================== 工作流CRUD接口 ====================
    
    /**
     * 创建工作流
     */
    @PostMapping("/create")
    @Operation(summary = "创建工作流", description = "创建自定义工作流编排")
    public BaseResponse<Long> createWorkflow(
            @RequestBody WorkflowDefinitionEntity workflow,
            HttpSession session) {
        
        User loginUser = userService.getLoginUser(session);
        if (loginUser == null) {
            return ResultUtils.error(ErrorCode.NOT_LOGIN_ERROR);
        }
        
        // 从Session获取userId
        workflow.setUserId(loginUser.getId());
        
        log.info("创建工作流: name={}, userId={}", workflow.getWorkflowName(), loginUser.getId());
        
        try {
            Long workflowId = workflowDefinitionService.createWorkflow(workflow);
            return ResultUtils.success(workflowId);
        } catch (Exception e) {
            log.error("创建工作流失败", e);
            return ResultUtils.error(ErrorCode.SYSTEM_ERROR, "创建工作流失败: " + e.getMessage());
        }
    }
    
    /**
     * 更新工作流
     */
    @PutMapping("/{workflowId}")
    @Operation(summary = "更新工作流", description = "更新工作流定义")
    public BaseResponse<Boolean> updateWorkflow(
            @PathVariable Long workflowId,
            @RequestBody WorkflowDefinitionEntity workflow,
            HttpSession session) {
        
        User loginUser = userService.getLoginUser(session);
        if (loginUser == null) {
            return ResultUtils.error(ErrorCode.NOT_LOGIN_ERROR);
        }
        
        // 设置ID和userId
        workflow.setId(workflowId);
        workflow.setUserId(loginUser.getId());
        
        log.info("更新工作流: ID={}, userId={}", workflowId, loginUser.getId());
        
        try {
            boolean success = workflowDefinitionService.updateWorkflow(workflow);
            if (!success) {
                return ResultUtils.error(ErrorCode.PARAMS_ERROR, "更新失败，请检查工作流是否存在且属于当前用户");
            }
            return ResultUtils.success(true);
        } catch (Exception e) {
            log.error("更新工作流失败", e);
            return ResultUtils.error(ErrorCode.SYSTEM_ERROR, "更新工作流失败: " + e.getMessage());
        }
    }
    
    /**
     * 删除工作流
     */
    @DeleteMapping("/{workflowId}")
    @Operation(summary = "删除工作流", description = "软删除工作流")
    public BaseResponse<Boolean> deleteWorkflow(
            @PathVariable Long workflowId,
            HttpSession session) {
        
        User loginUser = userService.getLoginUser(session);
        if (loginUser == null) {
            return ResultUtils.error(ErrorCode.NOT_LOGIN_ERROR);
        }
        
        log.info("删除工作流: ID={}, userId={}", workflowId, loginUser.getId());
        
        try {
            boolean success = workflowDefinitionService.deleteWorkflow(workflowId);
            if (!success) {
                return ResultUtils.error(ErrorCode.PARAMS_ERROR, "删除失败，工作流不存在或不属于当前用户");
            }
            return ResultUtils.success(true);
        } catch (Exception e) {
            log.error("删除工作流失败", e);
            return ResultUtils.error(ErrorCode.SYSTEM_ERROR, "删除工作流失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取工作流详情
     */
    @GetMapping("/{workflowId}")
    @Operation(summary = "获取工作流详情", description = "根据ID获取工作流定义")
    public BaseResponse<WorkflowDefinitionEntity> getWorkflowById(
            @PathVariable Long workflowId) {
        
        log.info("获取工作流详情: ID={}", workflowId);
        
        try {
            WorkflowDefinitionEntity workflow = workflowDefinitionService.getWorkflowById(workflowId);
            if (workflow == null) {
                return ResultUtils.error(ErrorCode.PARAMS_ERROR, "工作流不存在");
            }
            return ResultUtils.success(workflow);
        } catch (Exception e) {
            log.error("获取工作流详情失败", e);
            return ResultUtils.error(ErrorCode.SYSTEM_ERROR, "获取工作流详情失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取用户的工作流列表
     */
    @GetMapping("/my-workflows")
    @Operation(summary = "获取我的工作流", description = "分页获取当前用户的工作流列表")
    public BaseResponse<Page<WorkflowDefinitionEntity>> getUserWorkflows(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize,
            HttpSession session) {
        
        User loginUser = userService.getLoginUser(session);
        if (loginUser == null) {
            return ResultUtils.error(ErrorCode.NOT_LOGIN_ERROR);
        }
        
        log.info("获取用户工作流列表: userId={}, page={}, pageSize={}", loginUser.getId(), page, pageSize);
        
        try {
            Page<WorkflowDefinitionEntity> workflows = workflowDefinitionService.getUserWorkflows(
                    loginUser.getId(), page, pageSize);
            return ResultUtils.success(workflows);
        } catch (Exception e) {
            log.error("获取工作流列表失败", e);
            return ResultUtils.error(ErrorCode.SYSTEM_ERROR, "获取工作流列表失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取智能体的工作流列表
     */
    @GetMapping("/agent/{agentId}")
    @Operation(summary = "获取智能体的工作流", description = "获取指定智能体关联的已启用工作流")
    public BaseResponse<List<WorkflowDefinitionEntity>> getAgentWorkflows(
            @PathVariable Long agentId) {
        
        log.info("获取智能体工作流列表: agentId={}", agentId);
        
        try {
            List<WorkflowDefinitionEntity> workflows = workflowDefinitionService.getAgentWorkflows(agentId);
            return ResultUtils.success(workflows);
        } catch (Exception e) {
            log.error("获取智能体工作流列表失败", e);
            return ResultUtils.error(ErrorCode.SYSTEM_ERROR, "获取智能体工作流列表失败: " + e.getMessage());
        }
    }
    
    /**
     * 启用/禁用工作流
     */
    @PutMapping("/{workflowId}/toggle")
    @Operation(summary = "启用/禁用工作流", description = "切换工作流的启用状态")
    public BaseResponse<Boolean> toggleWorkflow(
            @PathVariable Long workflowId,
            @RequestParam boolean enabled,
            HttpSession session) {
        
        User loginUser = userService.getLoginUser(session);
        if (loginUser == null) {
            return ResultUtils.error(ErrorCode.NOT_LOGIN_ERROR);
        }
        
        log.info("{}工作流: ID={}, userId={}", enabled ? "启用" : "禁用", workflowId, loginUser.getId());
        
        try {
            boolean success = workflowDefinitionService.toggleWorkflow(workflowId, enabled);
            if (!success) {
                return ResultUtils.error(ErrorCode.PARAMS_ERROR, "操作失败，工作流不存在或不属于当前用户");
            }
            return ResultUtils.success(true);
        } catch (Exception e) {
            log.error("切换工作流状态失败", e);
            return ResultUtils.error(ErrorCode.SYSTEM_ERROR, "切换工作流状态失败: " + e.getMessage());
        }
    }
}
