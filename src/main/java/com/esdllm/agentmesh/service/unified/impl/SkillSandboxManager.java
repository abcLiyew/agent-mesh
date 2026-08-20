package com.esdllm.agentmesh.service.unified.impl;

import com.esdllm.agentmesh.common.ErrorCode;
import com.esdllm.agentmesh.exception.BusinessException;
import com.esdllm.agentmesh.model.domain.Tools;
import com.esdllm.agentmesh.model.dto.ToolInvocationContext;
import com.esdllm.agentmesh.repository.dao.ToolsDao;
import com.esdllm.agentmesh.service.agent.support.ToolInvocationService;
import jakarta.annotation.Resource;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 技能沙箱管理器 - 提供工具调用的安全隔离机制
 * 
 * 职责:
 * 1. 权限验证:检查用户是否有权限调用该工具
 * 2. 资源限制:限制工具调用的频率、超时时间等
 * 3. 输入验证:验证工具输入参数的合法性
 * 4. 错误隔离:单个工具失败不影响其他工具执行
 * 5. 审计日志:记录所有工具调用历史
 * 
 * 参考"龙虾"架构的安全设计原则
 */
@Component
@Slf4j
public class SkillSandboxManager {
    
    @Resource
    private ToolsDao toolsDao;
    
    @Resource
    private ToolInvocationService toolInvocationService;
    
    // 工具调用计数器(用于限流)
    private final Map<Long, CallCounter> callCounters = new ConcurrentHashMap<>();
    
    // 工具调用黑名单(动态封禁异常工具)
    private final Set<Long> blacklistedTools = ConcurrentHashMap.newKeySet();
    
    // 默认配置
    private static final long DEFAULT_TIMEOUT_MS = 30000L; // 30秒
    private static final int MAX_CALLS_PER_MINUTE = 60; // 每分钟最多60次调用
    private static final int MAX_RETRIES = 3; // 最大重试次数
    
    /**
     * 在沙箱中安全地执行工具调用
     * 
     * @param toolId 工具ID
     * @param userId 用户ID
     * @param parameters 输入参数
     * @param executor 实际的工具执行器
     * @return 执行结果
     */
    public SandboxExecutionResult executeInSandbox(
            Long toolId, Long userId, Map<String, Object> parameters, ToolExecutor executor) {
        
        log.info("=== 开始沙箱执行 === toolId: {}, userId: {}", toolId, userId);
        
        long startTime = System.currentTimeMillis();
        
        try {
            // 1. 前置安全检查
            preExecutionChecks(toolId, userId);
            
            // 2. 加载工具信息
            Tools tool = loadAndValidateTool(toolId, userId);
            
            // 3. 验证输入参数
            validateInputParameters(tool, parameters);
            
            // 4. 检查限流
            checkRateLimit(toolId, userId);
            
            // 5. 执行工具(带超时和重试)
            Object result = executeWithTimeoutAndRetry(tool, parameters, userId);
            
            long executionTime = System.currentTimeMillis() - startTime;
            
            // 6. 记录成功日志
            recordSuccessLog(toolId, userId, executionTime, result);
            
            log.info("=== 沙箱执行成功 === 耗时: {}ms", executionTime);
            
            return SandboxExecutionResult.builder()
                .success(true)
                .result(result)
                .executionTimeMs(executionTime)
                .toolName(tool.getDisplayName())
                .build();
                
        } catch (BusinessException e) {
            // 业务异常(权限、验证等)
            long executionTime = System.currentTimeMillis() - startTime;
            log.error("沙箱执行失败(业务异常): {}", e.getMessage());
            
            recordFailureLog(toolId, userId, executionTime, e.getMessage());
            
            return SandboxExecutionResult.builder()
                .success(false)
                .errorMessage(e.getMessage())
                .errorCode(e.getCode())
                .executionTimeMs(executionTime)
                .build();
                
        } catch (Exception e) {
            // 系统异常
            long executionTime = System.currentTimeMillis() - startTime;
            log.error("沙箱执行失败(系统异常)", e);
            
            recordFailureLog(toolId, userId, executionTime, e.getMessage());
            
            // 如果连续失败多次,加入黑名单
            handleRepeatedFailures(toolId, e.getMessage());
            
            return SandboxExecutionResult.builder()
                .success(false)
                .errorMessage("工具执行异常: " + e.getMessage())
                .errorCode(ErrorCode.SYSTEM_ERROR.getCode())
                .executionTimeMs(executionTime)
                .build();
        }
    }
    
    /**
     * 前置安全检查
     */
    private void preExecutionChecks(Long toolId, Long userId) {
        // 检查是否在黑名单中
        if (blacklistedTools.contains(toolId)) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, 
                "工具已被临时禁用,请稍后重试");
        }
        
        // 检查参数
        if (toolId == null || userId == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "工具ID和用户ID不能为空");
        }
    }
    
    /**
     * 加载并验证工具
     */
    private Tools loadAndValidateTool(Long toolId, Long userId) {
        Tools tool = toolsDao.getById(toolId);
        
        if (tool == null) {
            throw new BusinessException(ErrorCode.TOOL_NOT_FOUND, "工具不存在");
        }
        
        // 检查工具是否启用
        if (!Boolean.TRUE.equals(tool.getIsEnabled())) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "工具已禁用");
        }
        
        // 检查删除状态
        if (tool.getIsDelete() != null && tool.getIsDelete() == 1) {
            throw new BusinessException(ErrorCode.TOOL_NOT_FOUND, "工具已删除");
        }
        
        // 权限检查
        checkToolPermission(tool, userId);
        
        return tool;
    }
    
    /**
     * 检查工具使用权限
     */
    private void checkToolPermission(Tools tool, Long userId) {
        // 系统工具:所有人都可以使用
        if ("SYSTEM".equals(tool.getSourceType())) {
            return;
        }
        
        // 用户自定义工具:只有所有者可以使用
        if ("USER_HTTP".equals(tool.getSourceType()) || 
            "USER_MCP".equals(tool.getSourceType())) {
            
            if (tool.getOwnerId() != null && !tool.getOwnerId().equals(userId)) {
                throw new BusinessException(ErrorCode.NO_AUTH, "无权限使用该工具");
            }
        }
        
        // 智能体工具:需要检查关联关系
        if ("USER_AGENT".equals(tool.getSourceType())) {
            // TODO: 检查用户是否有权访问该智能体的工具
            log.debug("智能体工具权限检查: toolId={}, userId={}", tool.getId(), userId);
        }
    }
    
    /**
     * 验证输入参数
     */
    private void validateInputParameters(Tools tool, Map<String, Object> parameters) {
        if (parameters == null || parameters.isEmpty()) {
            log.debug("工具 {} 无输入参数", tool.getDisplayName());
            return;
        }
        
        // 检查参数大小限制(防止超大payload)
        int totalSize = estimateParameterSize(parameters);
        if (totalSize > 1024 * 1024) { // 1MB限制
            throw new BusinessException(ErrorCode.PARAMS_ERROR, 
                "输入参数过大,请控制在1MB以内");
        }
        
        // 检查敏感关键词(简单的安全防护)
        String[] sensitiveKeywords = {"rm -rf", "DROP TABLE", "exec(", "eval("};
        for (String keyword : sensitiveKeywords) {
            if (parameters.toString().contains(keyword)) {
                log.warn("检测到可疑参数内容,userId: {}", getUserIdFromContext());
                throw new BusinessException(ErrorCode.PARAMS_ERROR, 
                    "输入参数包含不安全内容");
            }
        }
        
        log.debug("参数验证通过,参数数量: {}", parameters.size());
    }
    
    /**
     * 估算参数大小
     */
    private int estimateParameterSize(Map<String, Object> parameters) {
        return parameters.toString().getBytes().length;
    }
    
    /**
     * 获取上下文中的用户ID
     */
    private Long getUserIdFromContext() {
        // TODO: 从SecurityContext或ThreadLocal获取
        return null;
    }
    
    /**
     * 检查限流
     */
    private void checkRateLimit(Long toolId, Long userId) {
        CallCounter counter = callCounters.computeIfAbsent(toolId, 
            k -> new CallCounter());
        
        // 重置过期的计数
        counter.resetIfExpired();
        
        // 检查是否超限
        if (counter.incrementAndGet() > MAX_CALLS_PER_MINUTE) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, 
                "调用频率过高,请稍后再试");
        }
    }
    
    /**
     * 带超时和重试的执行
     */
    private Object executeWithTimeoutAndRetry(
            Tools tool, Map<String, Object> parameters, Long userId) {
        
        Exception lastException = null;
        int maxRetries = getMaxRetries(tool);
        
        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                log.debug("执行工具调用 (尝试 {}/{}): {}", 
                    attempt, maxRetries, tool.getDisplayName());
                
                // 获取超时时间
                long timeout = getTimeout(tool);
                
                // 执行工具(这里简化处理,实际应该使用异步+Future实现真正的超时控制)
                Object result = executeTool(tool, parameters, userId, timeout);
                
                if (attempt > 1) {
                    log.info("工具调用在第 {} 次重试后成功", attempt);
                }
                
                return result;
                
            } catch (Exception e) {
                lastException = e;
                log.warn("工具调用失败 (尝试 {}/{}): {}", 
                    attempt, maxRetries, e.getMessage());
                
                if (attempt < maxRetries) {
                    // 等待一段时间后重试
                    try {
                        Thread.sleep(1000 * attempt); // 递增等待时间
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new BusinessException(ErrorCode.SYSTEM_ERROR, "执行被中断");
                    }
                }
            }
        }
        
        throw new BusinessException(ErrorCode.SYSTEM_ERROR, 
            "工具调用失败,已重试 " + maxRetries + " 次: " + lastException.getMessage());
    }
    
    /**
     * 执行工具(委托给实际的执行器)
     */
    private Object executeTool(Tools tool, Map<String, Object> parameters, 
                              Long userId, long timeout) {
        log.info("执行工具: type={}, name={}, timeout={}ms", 
            tool.getSourceType(), tool.getDisplayName(), timeout);
        
        try {
            // 构建ToolInvocationContext
            ToolInvocationContext context = ToolInvocationContext.builder()
                .toolId(tool.getId())
                .parameters(parameters)
                .timeoutMs(timeout)
                .build();
            
            // 根据工具类型调用不同的执行器
            String result;
            switch (tool.getSourceType()) {
                case "USER_HTTP":
                    result = toolInvocationService.invokeHttpTool(context, tool);
                    break;
                    
                case "USER_MCP":
                    result = toolInvocationService.invokeMcpTool(context, tool);
                    break;
                    
                case "USER_AGENT":
                    result = toolInvocationService.invokeAgentTool(tool, context, new HashMap<>());
                    break;
                    
                case "SYSTEM":
                    // 系统工具由调用方通过executor提供
                    if (parameters.containsKey("_system_executor")) {
                        @SuppressWarnings("unchecked")
                        SystemExecutor executor = (SystemExecutor) parameters.get("_system_executor");
                        return executor.execute(tool, parameters, userId);
                    }
                    throw new BusinessException(ErrorCode.SYSTEM_ERROR, "系统工具缺少执行器");
                    
                default:
                    throw new BusinessException(ErrorCode.PARAMS_ERROR, 
                        "不支持的工具类型: " + tool.getSourceType());
            }
            
            // 解析JSON结果
            return parseResult(result);
            
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("工具执行失败", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, 
                "工具执行失败: " + e.getMessage());
        }
    }
    
    /**
     * 解析结果字符串为对象
     */
    private Object parseResult(String result) {
        if (result == null || result.trim().isEmpty()) {
            return Map.of("success", true, "message", "执行成功");
        }
        
        try {
            // 尝试解析为JSON
            if (result.trim().startsWith("{")) {
                // 简单的JSON解析，实际应该使用Jackson或Gson
                return Map.of("success", true, "data", result);
            }
            return Map.of("success", true, "data", result);
        } catch (Exception e) {
            log.warn("结果解析失败，返回原始字符串", e);
            return Map.of("success", true, "data", result);
        }
    }
    
    /**
     * 系统工具执行器接口
     */
    @FunctionalInterface
    public interface SystemExecutor {
        Object execute(Tools tool, Map<String, Object> parameters, Long userId) throws Exception;
    }
    
    /**
     * 获取超时时间
     */
    private long getTimeout(Tools tool) {
        // 当前Tools实体中没有configParams字段，直接使用默认超时时间
        // TODO: 如果未来需要在tools表中添加timeout配置字段，可以在这里读取
        return DEFAULT_TIMEOUT_MS;
    }
    
    /**
     * 获取最大重试次数
     */
    private int getMaxRetries(Tools tool) {
        // 系统工具可以重试,用户自定义工具不重试(避免副作用)
        if ("SYSTEM".equals(tool.getSourceType())) {
            return MAX_RETRIES;
        }
        
        return 1; // 用户工具只执行一次
    }
    
    /**
     * 记录成功日志
     */
    private void recordSuccessLog(Long toolId, Long userId, long executionTime, Object result) {
        log.info("工具调用成功 | toolId: {} | userId: {} | 耗时: {}ms | 结果大小: {} bytes",
            toolId, userId, executionTime, 
            result != null ? result.toString().length() : 0);
        
        // TODO: 持久化到数据库的tool_call_log表
    }
    
    /**
     * 记录失败日志
     */
    private void recordFailureLog(Long toolId, Long userId, long executionTime, String errorMessage) {
        log.warn("工具调用失败 | toolId: {} | userId: {} | 耗时: {}ms | 错误: {}",
            toolId, userId, executionTime, errorMessage);
        
        // TODO: 持久化到数据库的tool_call_log表
    }
    
    /**
     * 处理重复失败(可能加入黑名单)
     */
    private void handleRepeatedFailures(Long toolId, String errorMessage) {
        // 简单实现:如果错误信息包含特定关键词,临时加入黑名单
        if (errorMessage.contains("timeout") || 
            errorMessage.contains("连接失败") ||
            errorMessage.contains("服务不可用")) {
            
            log.warn("工具 {} 因连续失败被临时加入黑名单", toolId);
            blacklistedTools.add(toolId);
            
            // 10分钟后自动移除(简化实现,生产环境应使用定时任务)
            CompletableFuture.delayedExecutor(10, java.util.concurrent.TimeUnit.MINUTES)
                .execute(() -> {
                    blacklistedTools.remove(toolId);
                    log.info("工具 {} 已从黑名单中移除", toolId);
                });
        }
    }
    
    // ========== 内部类 ==========
    
    /**
     * 调用计数器(用于限流)
     */
    private static class CallCounter {
        private final AtomicInteger count = new AtomicInteger(0);
        private volatile long windowStart = System.currentTimeMillis();
        private static final long WINDOW_MS = 60000; // 1分钟窗口
        
        public int incrementAndGet() {
            return count.incrementAndGet();
        }
        
        public void resetIfExpired() {
            long now = System.currentTimeMillis();
            if (now - windowStart > WINDOW_MS) {
                synchronized (this) {
                    if (now - windowStart > WINDOW_MS) {
                        count.set(0);
                        windowStart = now;
                    }
                }
            }
        }
    }
    
    /**
     * 工具执行器函数式接口
     */
    @FunctionalInterface
    public interface ToolExecutor {
        Object execute(Tools tool, Map<String, Object> parameters, Long userId) throws Exception;
    }
    
    /**
     * 沙箱执行结果
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SandboxExecutionResult {
        private boolean success;
        private Object result;
        private String errorMessage;
        private Integer errorCode;
        private Long executionTimeMs;
        private String toolName;
    }
}
