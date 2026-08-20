package com.esdllm.agentmesh.service.unified;

import com.esdllm.agentmesh.model.dto.DecisionExecutionResult;
import com.esdllm.agentmesh.model.dto.TaskExecutionPlan;
import com.esdllm.agentmesh.model.dto.WorkflowTemplate;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;

/**
 * 统一的智能体工作流引擎接口
 * 融合决策引擎和工作流引擎，实现"龙虾"架构的感知-决策-执行-反馈闭环
 */
public interface UnifiedAgentEngine {
    /**
     * 执行智能体工作流（同步）
     * @param agentId 智能体ID
     * @param query 用户查询
     * @param userId 用户ID
     * @param workflowId 可选的工作流ID，如果提供则使用指定工作流，否则由AI自主决策
     * @param context 上下文参数（包含历史记忆、环境变量等）
     * @return 执行结果
     */
    DecisionExecutionResult execute(Long agentId, String query, Long userId, 
                                   Long workflowId, Map<String, Object> context);
    
    /**
     * 执行智能体工作流（异步）
     * @param agentId 智能体ID
     * @param query 用户查询
     * @param userId 用户ID
     * @param workflowId 可选的工作流ID
     * @param context 上下文参数
     */
    void executeAsync(Long agentId, String query, Long userId, 
                     Long workflowId, Map<String, Object> context);
    
    /**
     * 流式执行智能体工作流（SSE）
     * @param agentId 智能体ID
     * @param query 用户查询
     * @param userId 用户ID
     * @param workflowId 可选的工作流ID
     * @param context 上下文参数
     * @param emitter SSE发射器
     * @param sessionId 会话ID（可选，用于多轮对话关联）
     */
    void executeStream(Long agentId, String query, Long userId, 
                      Long workflowId, Map<String, Object> context, Object emitter, String sessionId);
    
    /**
     * 流式执行智能体工作流（Reactor Flux）
     * @param agentId 智能体ID
     * @param query 用户查询
     * @param userId 用户ID
     * @param workflowId 可选的工作流ID
     * @param context 上下文参数
     * @return 响应流
     */
    Flux<String> executeFlux(Long agentId, String query, Long userId, 
                            Long workflowId, Map<String, Object> context);
    
    /**
     * 动态创建工作流
     * 基于用户意图和历史经验，AI自主生成工作流定义
     * @param agentId 智能体ID
     * @param taskDescription 任务描述
     * @param userId 用户ID
     * @return 生成的工作流ID
     */
    Long generateWorkflow(Long agentId, String taskDescription, Long userId);
    
    /**
     * 学习和优化工作流
     * 基于执行结果和用户反馈，自动优化工作流定义
     * @param workflowId 工作流ID
     * @param executionResult 执行结果
     * @param userFeedback 用户反馈（可选）
     */
    void learnAndOptimize(Long workflowId, DecisionExecutionResult executionResult, 
                         String userFeedback);
    
    /**
     * 规划任务执行计划（不执行，仅返回待办清单）
     * 分析用户意图，拆解任务步骤，返回给前端确认
     * @param agentId 智能体ID
     * @param query 用户查询
     * @param userId 用户ID
     * @param context 上下文参数
     * @param sessionId 会话ID（可选，用于多轮对话关联）
     * @return 任务执行计划
     */
    TaskExecutionPlan planTask(Long agentId, String query, Long userId, Map<String, Object> context, String sessionId);
    
    /**
     * 判断是否为简单任务
     * @param agentId 智能体ID
     * @param query 用户查询
     * @param userId 用户ID
     * @return true=简单任务，false=复杂任务
     */
    boolean isSimpleTask(Long agentId, String query, Long userId);
    
    /**
     * 执行简单任务（直接调用LLM，不进行任务规划）
     * @param agentId 智能体ID
     * @param query 用户查询
     * @param userId 用户ID
     * @param sessionId 会话ID（可选）
     * @return 执行结果
     */
    DecisionExecutionResult executeSimpleTask(Long agentId, String query, Long userId, String sessionId);
    
    /**
     * 执行已确认的任务计划
     * @param taskId 任务ID（从planTask返回）
     * @param confirmedSteps 用户确认的步骤列表（可选，为空则执行全部）
     * @param userId 用户ID
     * @param sessionId 会话ID（可选，用于多轮对话关联）
     * @return 执行结果
     */
    DecisionExecutionResult executePlannedTask(String taskId, List<String> confirmedSteps, Long userId, String sessionId);
    
    /**
     * 从缓存中获取任务计划（用于提取sessionId）
     * @param taskId 任务ID
     * @return 任务计划，如果不存在则返回null
     */
    TaskExecutionPlan getTaskPlanFromCache(String taskId);
    
    /**
     * 创建工作流模板
     * @param template 工作流模板
     * @return 模板ID
     */
    Long createWorkflowTemplate(WorkflowTemplate template);
    
    /**
     * AI辅助生成工作流模板（半自定义）
     * 用户提供部分节点，AI自动补充缺失的部分
     * @param agentId 智能体ID
     * @param taskDescription 任务描述
     * @param userDefinedNodes 用户定义的节点
     * @param userId 用户ID
     * @return 完整的工作流模板
     */
    WorkflowTemplate aiAssistWorkflow(Long agentId, String taskDescription,
                                      List<WorkflowTemplate.TemplateNode> userDefinedNodes,
                                      Long userId);
    
    /**
     * 获取工作流模板列表
     * @param userId 用户ID
     * @param mode 工作流模式（可选）
     * @return 模板列表
     */
    List<WorkflowTemplate> getWorkflowTemplates(Long userId, String mode);
    
    /**
     * 基于模板执行工作流
     * @param templateId 模板ID
     * @param inputParams 输入参数
     * @param userId 用户ID
     * @return 执行结果
     */
    DecisionExecutionResult executeFromTemplate(Long templateId, Map<String, Object> inputParams, Long userId);

    Map<String, Object> enrichContextWithMemories(Long userId, Long agentId,
                                                  String query, Map<String, Object> context);

    /**
     * 多智能体协同执行
     * 根据任务需求自动协调多个智能体共同完成任务
     * @param mainAgentId 主智能体ID
     * @param query 用户查询
     * @param userId 用户ID
     * @param context 上下文参数
     * @return 协同执行结果(包含各子智能体的执行详情)
     */
    Object executeCollaboratively(Long mainAgentId, String query, Long userId, Map<String, Object> context);
}
