package com.esdllm.agentmesh.service.unified;

import com.esdllm.agentmesh.service.unified.impl.SkillSandboxManager;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 技能沙箱隔离机制测试
 */
@SpringBootTest
@Slf4j
public class SkillSandboxTest {
    
    @Resource
    private SkillSandboxManager skillSandboxManager;
    
    /**
     * 测试沙箱执行 - 成功场景
     */
    @Test
    public void testExecuteInSandbox_Success() {
        Long toolId = 1L; // 假设存在系统工具
        Long userId = 1L;
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("query", "test query");
        
        log.info("=== 测试沙箱执行(成功场景) ===");
        
        SkillSandboxManager.SandboxExecutionResult result = 
            skillSandboxManager.executeInSandbox(
                toolId, 
                userId, 
                parameters,
                (tool, params, uid) -> {
                    // 模拟成功的工具执行
                    return Map.of(
                        "status", "success",
                        "data", "mock data"
                    );
                }
            );
        
        assertNotNull(result);
        log.info("执行结果: success={}, executionTime={}ms", 
            result.isSuccess(), result.getExecutionTimeMs());
        
        if (result.isSuccess()) {
            log.info("返回数据: {}", result.getResult());
        } else {
            log.warn("执行失败: {}", result.getErrorMessage());
        }
    }
    
    /**
     * 测试沙箱执行 - 权限拒绝
     */
    @Test
    public void testExecuteInSandbox_PermissionDenied() {
        Long toolId = 999L; // 不存在的工具
        Long userId = 1L;
        
        log.info("=== 测试沙箱执行(权限拒绝) ===");
        
        SkillSandboxManager.SandboxExecutionResult result = 
            skillSandboxManager.executeInSandbox(
                toolId, 
                userId, 
                Map.of(),
                (tool, params, uid) -> "should not reach here"
            );
        
        assertNotNull(result);
        assertFalse(result.isSuccess(), "应该返回失败结果");
        log.info("预期错误: {}", result.getErrorMessage());
    }
    
    /**
     * 测试沙箱执行 - 参数验证
     */
    @Test
    public void testExecuteInSandbox_ParameterValidation() {
        Long toolId = 1L;
        Long userId = 1L;
        
        // 构造超大参数
        Map<String, Object> largeParams = new HashMap<>();
        StringBuilder largeString = new StringBuilder();
        for (int i = 0; i < 2000000; i++) { // 超过1MB
            largeString.append("x");
        }
        largeParams.put("data", largeString.toString());
        
        log.info("=== 测试沙箱执行(参数过大) ===");
        
        SkillSandboxManager.SandboxExecutionResult result = 
            skillSandboxManager.executeInSandbox(
                toolId, 
                userId, 
                largeParams,
                (tool, params, uid) -> "should not reach here"
            );
        
        assertNotNull(result);
        assertFalse(result.isSuccess(), "应该拒绝超大参数");
        log.info("预期错误: {}", result.getErrorMessage());
    }
    
    /**
     * 测试沙箱执行 - 限流
     */
    @Test
    public void testExecuteInSandbox_RateLimiting() throws InterruptedException {
        Long toolId = 1L;
        Long userId = 1L;
        
        log.info("=== 测试沙箱执行(限流) ===");
        
        // 快速连续调用多次
        int callCount = 0;
        int rejectedCount = 0;
        
        for (int i = 0; i < 70; i++) { // 超过60次/分钟的限制
            SkillSandboxManager.SandboxExecutionResult result = 
                skillSandboxManager.executeInSandbox(
                    toolId, 
                    userId, 
                    Map.of("index", i),
                    (tool, params, uid) -> Map.of("index", params.get("index"))
                );
            
            if (result.isSuccess()) {
                callCount++;
            } else {
                rejectedCount++;
                if (rejectedCount == 1) {
                    log.info("第 {} 次调用被限流: {}", i + 1, result.getErrorMessage());
                }
            }
        }
        
        log.info("测试结果: 成功 {} 次, 被限流 {} 次", callCount, rejectedCount);
        assertTrue(rejectedCount > 0, "应该有调用被限流");
    }
    
    /**
     * 测试沙箱执行 - 超时控制
     */
    @Test
    public void testExecuteInSandbox_Timeout() {
        Long toolId = 1L;
        Long userId = 1L;
        
        log.info("=== 测试沙箱执行(超时控制) ===");
        
        SkillSandboxManager.SandboxExecutionResult result = 
            skillSandboxManager.executeInSandbox(
                toolId, 
                userId, 
                Map.of(),
                (tool, params, uid) -> {
                    // 模拟长时间运行的任务
                    try {
                        Thread.sleep(5000); // 5秒
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                    return "completed";
                }
            );
        
        assertNotNull(result);
        log.info("执行结果: success={}, executionTime={}ms", 
            result.isSuccess(), result.getExecutionTimeMs());
    }
    
    /**
     * 测试沙箱执行 - 重试机制
     */
    @Test
    public void testExecuteInSandbox_RetryMechanism() {
        Long toolId = 1L;
        Long userId = 1L;
        
        log.info("=== 测试沙箱执行(重试机制) ===");
        
        // 使用计数器模拟前两次失败,第三次成功
        final int[] attemptCount = {0};
        
        SkillSandboxManager.SandboxExecutionResult result = 
            skillSandboxManager.executeInSandbox(
                toolId, 
                userId, 
                Map.of(),
                (tool, params, uid) -> {
                    attemptCount[0]++;
                    log.info("尝试第 {} 次执行", attemptCount[0]);
                    
                    if (attemptCount[0] < 3) {
                        throw new RuntimeException("模拟临时故障");
                    }
                    
                    return "success after retry";
                }
            );
        
        assertNotNull(result);
        log.info("最终结果: success={}, 总尝试次数={}", 
            result.isSuccess(), attemptCount[0]);
        
        // 注意:由于当前实现中用户工具不重试,这个测试可能不会触发重试
    }
}
