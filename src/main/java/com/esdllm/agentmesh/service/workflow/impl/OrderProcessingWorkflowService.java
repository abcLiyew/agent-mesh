package com.esdllm.agentmesh.service.workflow.impl;

import com.esdllm.agentmesh.model.dto.workflow.WorkflowDefinition;
import com.esdllm.agentmesh.model.dto.workflow.WorkflowNode;
import com.esdllm.agentmesh.model.dto.workflow.WorkflowExecutionResult;
import com.esdllm.agentmesh.service.workflow.WorkflowEngine;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * 订单处理工作流服务 - 演示复杂工作流场景
 * 
 * 工作流程:
 * 1. 用户查询订单状态
 * 2. 调用订单查询工具获取订单详情
 * 3. 判断是否符合退款条件
 *    - 是: 执行退款流程(库存回滚 → 支付退款 → 发送通知)
 *    - 否: 返回拒绝原因
 * 4. 生成最终回复
 */
@Service
@Slf4j
public class OrderProcessingWorkflowService {
    
    @Resource
    private WorkflowEngine workflowEngine;
    
    /**
     * 创建订单处理工作流定义
     */
    public WorkflowDefinition createOrderProcessingWorkflow() {
        List<WorkflowNode> nodes = new ArrayList<>();
        
        // 1. 起始节点
        nodes.add(WorkflowNode.builder()
                .nodeId("start")
                .nodeName("开始")
                .nodeType(WorkflowNode.NodeType.START)
                .description("工作流入口")
                .build());
        
        // 2. 查询订单信息
        nodes.add(WorkflowNode.builder()
                .nodeId("query_order")
                .nodeName("查询订单")
                .nodeType(WorkflowNode.NodeType.TOOL_CALL)
                .resourceId(1L) // 假设订单查询工具的ID为1
                .resourceType(WorkflowNode.ResourceType.TOOL)
                .inputParams(Map.of(
                        "order_id", "${order_id}",
                        "user_id", "${user_id}"
                ))
                .timeoutMs(10000L)
                .retryCount(2)
                .errorStrategy("FAIL_FAST")
                .description("调用订单查询工具获取订单详情")
                .build());
        
        // 3. 条件判断: 是否可以退款
        nodes.add(WorkflowNode.builder()
                .nodeId("check_refund_eligible")
                .nodeName("检查退款资格")
                .nodeType(WorkflowNode.NodeType.CONDITION)
                .conditionExpression("${query_order_result.can_refund}")
                .trueBranch("process_refund")
                .falseBranch("reject_refund")
                .description("判断订单是否符合退款条件")
                .build());
        
        // 4. 并行执行退款流程
        List<String> refundSteps = Arrays.asList("rollback_inventory", "process_payment_refund");
        nodes.add(WorkflowNode.builder()
                .nodeId("process_refund")
                .nodeName("处理退款")
                .nodeType(WorkflowNode.NodeType.SEQUENCE)
                .childNodes(refundSteps)
                .description("顺序执行退款步骤")
                .build());
        
        // 4a. 库存回滚
        nodes.add(WorkflowNode.builder()
                .nodeId("rollback_inventory")
                .nodeName("库存回滚")
                .nodeType(WorkflowNode.NodeType.TOOL_CALL)
                .resourceId(2L) // 假设库存工具的ID为2
                .resourceType(WorkflowNode.ResourceType.TOOL)
                .inputParams(Map.of(
                        "product_id", "${query_order_result.product_id}",
                        "quantity", "${query_order_result.quantity}"
                ))
                .timeoutMs(5000L)
                .retryCount(1)
                .errorStrategy("RETRY")
                .description("回滚商品库存")
                .build());
        
        // 4b. 支付退款
        nodes.add(WorkflowNode.builder()
                .nodeId("process_payment_refund")
                .nodeName("支付退款")
                .nodeType(WorkflowNode.NodeType.TOOL_CALL)
                .resourceId(3L) // 假设支付工具的ID为3
                .resourceType(WorkflowNode.ResourceType.TOOL)
                .inputParams(Map.of(
                        "order_id", "${order_id}",
                        "amount", "${query_order_result.amount}",
                        "refund_reason", "${refund_reason}"
                ))
                .timeoutMs(15000L)
                .retryCount(2)
                .errorStrategy("FAIL_FAST")
                .description("调用支付接口进行退款")
                .build());
        
        // 5. 拒绝退款分支
        nodes.add(WorkflowNode.builder()
                .nodeId("reject_refund")
                .nodeName("拒绝退款")
                .nodeType(WorkflowNode.NodeType.TOOL_CALL)
                .resourceId(4L) // 假设通知工具的ID为4
                .resourceType(WorkflowNode.ResourceType.TOOL)
                .inputParams(Map.of(
                        "user_id", "${user_id}",
                        "message", "您的订单不符合退款条件: ${query_order_result.reject_reason}"
                ))
                .description("发送拒绝退款通知")
                .build());
        
        // 6. 发送成功通知 (并行执行)
        nodes.add(WorkflowNode.builder()
                .nodeId("send_notification")
                .nodeName("发送通知")
                .nodeType(WorkflowNode.NodeType.TOOL_CALL)
                .resourceId(4L)
                .resourceType(WorkflowNode.ResourceType.TOOL)
                .inputParams(Map.of(
                        "user_id", "${user_id}",
                        "message", "退款已成功处理,订单号: ${order_id}"
                ))
                .description("发送退款成功通知")
                .build());
        
        // 7. 生成最终回复
        nodes.add(WorkflowNode.builder()
                .nodeId("generate_response")
                .nodeName("生成回复")
                .nodeType(WorkflowNode.NodeType.AGENT_CALL)
                .resourceId(100L) // 假设回复生成智能体的ID为100
                .resourceType(WorkflowNode.ResourceType.AGENT)
                .inputParams(Map.of(
                        "order_info", "${query_order_result}",
                        "refund_status", "${process_refund_result}",
                        "original_query", "${original_query}"
                ))
                .description("使用AI生成人性化回复")
                .build());
        
        // 8. 结束节点
        nodes.add(WorkflowNode.builder()
                .nodeId("end")
                .nodeName("结束")
                .nodeType(WorkflowNode.NodeType.END)
                .description("工作流出口")
                .build());
        
        return WorkflowDefinition.builder()
                .workflowName("订单退款处理工作流")
                .description("自动化处理订单查询和退款流程")
                .version("1.0")
                .nodes(nodes)
                .startNodeId("start")
                .globalVariables(new HashMap<>())
                .timeoutMs(60000L) // 总超时60秒
                .enabled(true)
                .build();
    }
    
    /**
     * 执行订单处理工作流
     * 
     * @param orderId 订单ID
     * @param userId 用户ID
     * @param refundReason 退款原因
     * @return 执行结果
     */
    public WorkflowExecutionResult processOrderRefund(String orderId, Long userId, String refundReason) {
        log.info("开始处理订单退款, orderId: {}, userId: {}", orderId, userId);
        
        // 创建工作流定义
        WorkflowDefinition workflow = createOrderProcessingWorkflow();
        
        // 准备输入参数
        Map<String, Object> inputParams = new HashMap<>();
        inputParams.put("order_id", orderId);
        inputParams.put("user_id", userId);
        inputParams.put("refund_reason", refundReason);
        inputParams.put("original_query", "查询订单 " + orderId + " 并申请退款");
        
        // 执行工作流
        WorkflowExecutionResult result = workflowEngine.execute(workflow, inputParams, userId);
        
        if (result.getSuccess()) {
            log.info("订单退款处理成功, executionId: {}", result.getExecutionId());
        } else {
            log.error("订单退款处理失败: {}", result.getErrorMessage());
        }
        
        return result;
    }
    
    /**
     * 创建更复杂的并行工作流示例
     * 同时查询多个数据源并合并结果
     */
    public WorkflowDefinition createParallelDataQueryWorkflow() {
        List<WorkflowNode> nodes = new ArrayList<>();
        
        // 起始节点
        nodes.add(WorkflowNode.builder()
                .nodeId("start")
                .nodeName("开始")
                .nodeType(WorkflowNode.NodeType.START)
                .build());
        
        // 并行查询三个数据源
        List<String> parallelQueries = Arrays.asList("query_user_info", "query_order_history", "query_preferences");
        nodes.add(WorkflowNode.builder()
                .nodeId("parallel_query")
                .nodeName("并行查询数据")
                .nodeType(WorkflowNode.NodeType.PARALLEL)
                .childNodes(parallelQueries)
                .description("同时查询用户信息、订单历史和偏好设置")
                .build());
        
        // 查询用户信息
        nodes.add(WorkflowNode.builder()
                .nodeId("query_user_info")
                .nodeName("查询用户信息")
                .nodeType(WorkflowNode.NodeType.TOOL_CALL)
                .resourceId(10L)
                .resourceType(WorkflowNode.ResourceType.TOOL)
                .inputParams(Map.of("user_id", "${user_id}"))
                .timeoutMs(5000L)
                .build());
        
        // 查询订单历史
        nodes.add(WorkflowNode.builder()
                .nodeId("query_order_history")
                .nodeName("查询订单历史")
                .nodeType(WorkflowNode.NodeType.TOOL_CALL)
                .resourceId(11L)
                .resourceType(WorkflowNode.ResourceType.TOOL)
                .inputParams(Map.of("user_id", "${user_id}", "limit", 10))
                .timeoutMs(5000L)
                .build());
        
        // 查询用户偏好
        nodes.add(WorkflowNode.builder()
                .nodeId("query_preferences")
                .nodeName("查询用户偏好")
                .nodeType(WorkflowNode.NodeType.TOOL_CALL)
                .resourceId(12L)
                .resourceType(WorkflowNode.ResourceType.TOOL)
                .inputParams(Map.of("user_id", "${user_id}"))
                .timeoutMs(5000L)
                .build());
        
        // 合并数据并生成推荐
        nodes.add(WorkflowNode.builder()
                .nodeId("generate_recommendation")
                .nodeName("生成个性化推荐")
                .nodeType(WorkflowNode.NodeType.AGENT_CALL)
                .resourceId(101L)
                .resourceType(WorkflowNode.ResourceType.AGENT)
                .inputParams(Map.of(
                        "user_info", "${query_user_info_result}",
                        "order_history", "${query_order_history_result}",
                        "preferences", "${query_preferences_result}"
                ))
                .description("基于多维度数据生成个性化推荐")
                .build());
        
        // 结束节点
        nodes.add(WorkflowNode.builder()
                .nodeId("end")
                .nodeName("结束")
                .nodeType(WorkflowNode.NodeType.END)
                .build());
        
        return WorkflowDefinition.builder()
                .workflowName("并行数据查询与推荐工作流")
                .description("同时查询多个数据源并生成个性化推荐")
                .version("1.0")
                .nodes(nodes)
                .startNodeId("start")
                .timeoutMs(30000L)
                .enabled(true)
                .build();
    }
}
