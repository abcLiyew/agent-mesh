package com.esdllm.agentmesh.service.workflow.impl;

import com.esdllm.agentmesh.common.ErrorCode;
import com.esdllm.agentmesh.exception.BusinessException;
import com.esdllm.agentmesh.model.domain.Tools;
import com.esdllm.agentmesh.model.dto.IntentRecognitionResult;
import com.esdllm.agentmesh.model.dto.ToolInvocationContext;
import com.esdllm.agentmesh.model.dto.workflow.WorkflowDefinition;
import com.esdllm.agentmesh.model.dto.workflow.WorkflowExecutionResult;
import com.esdllm.agentmesh.model.dto.workflow.WorkflowNode;
import com.esdllm.agentmesh.repository.dao.ToolsDao;
import com.esdllm.agentmesh.service.AgentToolService;
import com.esdllm.agentmesh.service.agent.support.AiModelSupport;
import com.esdllm.agentmesh.service.agent.support.SystemToolService;
import com.esdllm.agentmesh.service.agent.support.ToolInvocationService;
import com.esdllm.agentmesh.service.workflow.WorkflowEngine;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 工作流引擎实现类
 */
@Service
@Slf4j
public class WorkflowEngineImpl implements WorkflowEngine {
    
    @Resource
    private ToolsDao toolsDao;
    
    @Resource
    private AgentToolService agentToolService;
    
    @Resource
    private AiModelSupport aiModelSupport;
    
    @Resource
    private ToolInvocationService toolInvocationService;
    
    @Resource
    private SystemToolService systemToolService;
    
    private final ExecutorService workflowExecutor = Executors.newFixedThreadPool(10);
    
    private static final Pattern EXPRESSION_PATTERN = Pattern.compile("\\$\\{([^}]+)\\}");
    
    @Override
    public WorkflowExecutionResult execute(WorkflowDefinition workflowDefinition, 
                                          Map<String, Object> inputParams, 
                                          Long userId) {
        long startTime = System.currentTimeMillis();
        String executionId = "wf_" + workflowDefinition.getWorkflowId() + "_" + System.currentTimeMillis();
        
        log.info("开始执行工作流, executionId: {}, workflowId: {}", executionId, workflowDefinition.getWorkflowId());
        
        // 验证工作流定义
        if (!validateWorkflow(workflowDefinition)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "工作流定义无效");
        }
        
        // 初始化执行上下文
        Map<String, Object> context = new HashMap<>();
        if (workflowDefinition.getGlobalVariables() != null) {
            context.putAll(workflowDefinition.getGlobalVariables());
        }
        if (inputParams != null) {
            context.putAll(inputParams);
        }
        
        WorkflowExecutionResult result = WorkflowExecutionResult.builder()
                .executionId(executionId)
                .workflowId(workflowDefinition.getWorkflowId())
                .executionPath(new ArrayList<>())
                .nodeResults(new HashMap<>())
                .build();
        
        try {
            // 从起始节点开始执行
            String currentNodeId = workflowDefinition.getStartNodeId();
            executeNode(currentNodeId, workflowDefinition, context, result, userId);
            
            result.setSuccess(true);
            result.setTotalDurationMs(System.currentTimeMillis() - startTime);
            result.setFinalVariables(context);
            
            log.info("工作流执行成功, executionId: {}, duration: {}ms", 
                    executionId, result.getTotalDurationMs());
            
        } catch (Exception e) {
            log.error("工作流执行失败, executionId: {}", executionId, e);
            result.setSuccess(false);
            result.setErrorMessage(e.getMessage());
            result.setTotalDurationMs(System.currentTimeMillis() - startTime);
        }
        
        return result;
    }
    
    @Override
    public void executeAsync(WorkflowDefinition workflowDefinition, 
                            Map<String, Object> inputParams, 
                            Long userId) {
        workflowExecutor.submit(() -> {
            try {
                execute(workflowDefinition, inputParams, userId);
            } catch (Exception e) {
                log.error("异步工作流执行失败", e);
            }
        });
    }
    
    @Override
    public boolean validateWorkflow(WorkflowDefinition workflowDefinition) {
        if (workflowDefinition == null || workflowDefinition.getNodes() == null) {
            return false;
        }
        
        // 检查是否有起始节点
        if (workflowDefinition.getStartNodeId() == null) {
            return false;
        }
        
        // 检查节点ID唯一性
        Set<String> nodeIds = new HashSet<>();
        for (WorkflowNode node : workflowDefinition.getNodes()) {
            if (!nodeIds.add(node.getNodeId())) {
                log.error("节点ID重复: {}", node.getNodeId());
                return false;
            }
        }
        
        // 检查起始节点是否存在
        if (!nodeIds.contains(workflowDefinition.getStartNodeId())) {
            log.error("起始节点不存在: {}", workflowDefinition.getStartNodeId());
            return false;
        }
        
        return true;
    }
    
    /**
     * 递归执行节点
     */
    private void executeNode(String nodeId, 
                           WorkflowDefinition workflow, 
                           Map<String, Object> context,
                           WorkflowExecutionResult result,
                           Long userId) {
        // 查找节点定义
        WorkflowNode node = findNodeById(workflow.getNodes(), nodeId);
        if (node == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "节点不存在: " + nodeId);
        }
        
        log.info("执行节点: {}, 类型: {}", node.getNodeName(), node.getNodeType());
        
        // 记录执行路径
        result.getExecutionPath().add(nodeId);
        
        long nodeStartTime = System.currentTimeMillis();
        WorkflowExecutionResult.NodeExecutionResult nodeResult = 
                WorkflowExecutionResult.NodeExecutionResult.builder()
                        .nodeId(nodeId)
                        .nodeName(node.getNodeName())
                        .build();
        
        try {
            Object output = null;
            
            switch (node.getNodeType()) {
                case START:
                    // 起始节点,直接跳转到下一个节点
                    String nextNode = findNextNode(node, null);
                    if (nextNode != null) {
                        executeNode(nextNode, workflow, context, result, userId);
                    }
                    break;
                    
                case END:
                    // 结束节点,将最终结果保存到输出
                    result.setOutput(context.get("_final_output"));
                    break;
                    
                case TOOL_CALL:
                    output = executeToolCall(node, context, userId);
                    nodeResult.setOutput(output);
                    // 将结果存入上下文
                    context.put(node.getNodeId() + "_result", output);
                    break;
                    
                case AGENT_CALL:
                    output = executeAgentCall(node, context, userId);
                    nodeResult.setOutput(output);
                    context.put(node.getNodeId() + "_result", output);
                    break;
                    
                case CONDITION:
                    String branch = evaluateCondition(node, context);
                    String nextBranch = "true".equals(branch) ? node.getTrueBranch() : node.getFalseBranch();
                    if (nextBranch != null) {
                        executeNode(nextBranch, workflow, context, result, userId);
                    }
                    break;
                    
                case SEQUENCE:
                    // 顺序执行子节点
                    if (node.getChildNodes() != null) {
                        for (String childNodeId : node.getChildNodes()) {
                            executeNode(childNodeId, workflow, context, result, userId);
                        }
                    }
                    break;
                    
                case PARALLEL:
                    // 并行执行子节点
                    if (node.getChildNodes() != null) {
                        executeParallelNodes(node.getChildNodes(), workflow, context, result, userId);
                    }
                    break;
                    
                default:
                    throw new BusinessException(ErrorCode.PARAMS_ERROR, 
                            "不支持的节点类型: " + node.getNodeType());
            }
            
            nodeResult.setSuccess(true);
            nodeResult.setDurationMs(System.currentTimeMillis() - nodeStartTime);
            
        } catch (Exception e) {
            log.error("节点执行失败: {}", nodeId, e);
            nodeResult.setSuccess(false);
            nodeResult.setErrorMessage(e.getMessage());
            nodeResult.setDurationMs(System.currentTimeMillis() - nodeStartTime);
            
            // 根据错误策略处理
            if ("FAIL_FAST".equals(node.getErrorStrategy())) {
                throw e;
            }
            // CONTINUE: 继续执行
            // RETRY: 重试逻辑可在上层实现
        }
        
        result.getNodeResults().put(nodeId, nodeResult);
    }
    
    /**
     * 执行工具调用
     */
    private Object executeToolCall(WorkflowNode node, Map<String, Object> context, Long userId) {
        Tools tool = toolsDao.getById(node.getResourceId());
        if (tool == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "工具不存在");
        }
        
        // 解析输入参数
        Map<String, Object> params = resolveParameters(node.getInputParams(), context);
        
        log.info("调用工具: {}, 类型: {}, 参数: {}", tool.getDisplayName(), tool.getSourceType(), params);
        
        // 构建工具调用上下文
        ToolInvocationContext invocationContext = ToolInvocationContext.builder()
            .toolId(tool.getId())
            .toolType(tool.getSourceType())
            .parameters(params)
            .timeoutMs(node.getTimeoutMs() != null ? node.getTimeoutMs() : 30000L)
            .build();
        
        // 根据工具类型调用不同的处理器
        try {
            String response = switch (tool.getSourceType()) {
                case "SYSTEM" -> {
                    // 系统工具
                    yield systemToolService.invokeSystemTool(tool.getToolCodeName(), invocationContext);
                }
                case "USER_HTTP" -> {
                    // HTTP 工具
                    yield toolInvocationService.invokeHttpTool(invocationContext, tool);
                }
                case "USER_MCP" -> {
                    // MCP 工具
                    yield toolInvocationService.invokeMcpTool(invocationContext, tool);
                }
                default -> throw new BusinessException(ErrorCode.PARAMS_ERROR, 
                        "不支持的工具类型: " + tool.getSourceType());
            };
            
            log.info("工具调用成功: {}, 响应长度: {}", tool.getDisplayName(), 
                    response != null ? response.length() : 0);
            return response;
            
        } catch (Exception e) {
            log.error("工具调用失败: {}", tool.getDisplayName(), e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "工具调用失败: " + e.getMessage());
        }
    }
    
    /**
     * 执行智能体调用
     */
    private Object executeAgentCall(WorkflowNode node, Map<String, Object> context, Long userId) {
        // 解析输入参数
        Map<String, Object> params = resolveParameters(node.getInputParams(), context);
        
        log.info("调用智能体: {}, 参数: {}", node.getResourceId(), params);
        
        // 构建查询文本
        String query = buildQueryFromParams(params);
        
        // 调用智能体
        try {
            return agentToolService.invokeAgentTool(node.getResourceId(), query, params, userId);
        } catch (Exception e) {
            log.error("智能体调用失败: {}", node.getResourceId(), e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "智能体调用失败: " + e.getMessage());
        }
    }
    
    /**
     * 评估条件表达式
     */
    private String evaluateCondition(WorkflowNode node, Map<String, Object> context) {
        String expression = node.getConditionExpression();
        if (expression == null) {
            return "false";
        }
        
        // 简单的表达式求值 (生产环境建议使用 SpEL 或 MVEL)
        String evaluatedExpression = resolveExpression(expression, context);
        
        log.info("评估条件: {} => {}", expression, evaluatedExpression);
        
        // 简单的布尔判断
        if ("true".equalsIgnoreCase(evaluatedExpression) || 
            "yes".equalsIgnoreCase(evaluatedExpression) ||
            "1".equals(evaluatedExpression)) {
            return "true";
        }
        
        return "false";
    }
    
    /**
     * 并行执行节点
     */
    private void executeParallelNodes(List<String> nodeIds, 
                                     WorkflowDefinition workflow,
                                     Map<String, Object> context,
                                     WorkflowExecutionResult result,
                                     Long userId) {
        List<Future<?>> futures = new ArrayList<>();
        
        for (String nodeId : nodeIds) {
            Future<?> future = workflowExecutor.submit(() -> {
                try {
                    // 每个并行分支使用独立的上下文副本
                    Map<String, Object> branchContext = new ConcurrentHashMap<>(context);
                    executeNode(nodeId, workflow, branchContext, result, userId);
                    // 合并结果到主上下文
                    synchronized (context) {
                        context.putAll(branchContext);
                    }
                } catch (Exception e) {
                    log.error("并行节点执行失败: {}", nodeId, e);
                }
            });
            futures.add(future);
        }
        
        // 等待所有并行任务完成
        for (Future<?> future : futures) {
            try {
                future.get(30, TimeUnit.SECONDS); // 30秒超时
            } catch (Exception e) {
                log.error("等待并行任务完成时出错", e);
            }
        }
    }
    
    /**
     * 解析参数中的表达式
     */
    private Map<String, Object> resolveParameters(Map<String, Object> inputParams, 
                                                  Map<String, Object> context) {
        if (inputParams == null) {
            return new HashMap<>();
        }
        
        Map<String, Object> resolved = new HashMap<>();
        for (Map.Entry<String, Object> entry : inputParams.entrySet()) {
            Object value = entry.getValue();
            if (value instanceof String) {
                resolved.put(entry.getKey(), resolveExpression((String) value, context));
            } else {
                resolved.put(entry.getKey(), value);
            }
        }
        
        return resolved;
    }
    
    /**
     * 解析表达式
     */
    private String resolveExpression(String expression, Map<String, Object> context) {
        Matcher matcher = EXPRESSION_PATTERN.matcher(expression);
        StringBuffer result = new StringBuffer();
        
        while (matcher.find()) {
            String varName = matcher.group(1);
            Object value = context.get(varName);
            matcher.appendReplacement(result, value != null ? value.toString() : "");
        }
        matcher.appendTail(result);
        
        return result.toString();
    }
    
    /**
     * 查找下一个节点
     */
    private String findNextNode(WorkflowNode node, Object lastResult) {
        // 简单实现: 对于非条件节点,返回第一个未执行的子节点或null
        // 实际应该根据工作流的边关系来确定
        return null;
    }
    
    /**
     * 根据ID查找节点
     */
    private WorkflowNode findNodeById(List<WorkflowNode> nodes, String nodeId) {
        return nodes.stream()
                .filter(n -> n.getNodeId().equals(nodeId))
                .findFirst()
                .orElse(null);
    }
    
    /**
     * 从参数构建查询文本
     */
    private String buildQueryFromParams(Map<String, Object> params) {
        if (params == null || params.isEmpty()) {
            return "";
        }
        
        StringBuilder query = new StringBuilder();
        for (Map.Entry<String, Object> entry : params.entrySet()) {
            if (query.length() > 0) {
                query.append(", ");
            }
            query.append(entry.getKey()).append("=").append(entry.getValue());
        }
        
        return query.toString();
    }
}
