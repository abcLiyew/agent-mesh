package com.esdllm.agentmesh.service.unified;

import com.esdllm.agentmesh.model.dto.DecisionExecutionResult;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 统一智能体引擎集成测试
 */
@SpringBootTest
@Slf4j
public class UnifiedAgentEngineIntegrationTest {
    
    @Resource
    private UnifiedAgentEngine unifiedAgentEngine;
    
    @Test
    public void testExecuteWithAutoDecision() {
        // 测试自主决策模式（不使用工作流）
        Long agentId = 1L;
        String query = "你好，请介绍一下你自己";
        Long userId = 1L;
        
        DecisionExecutionResult result = unifiedAgentEngine.execute(
            agentId, query, userId, null, null
        );
        
        assertNotNull(result);
        log.info("执行结果: success={}, response={}", 
                result.getSuccess(), 
                result.getFinalResponse());
    }
    
    @Test
    public void testExecuteWithContext() {
        // 测试带上下文的执行
        Long agentId = 1L;
        String query = "基于之前的讨论，继续分析这个问题";
        Long userId = 1L;
        
        Map<String, Object> context = new HashMap<>();
        context.put("previous_topic", "市场分析");
        context.put("sessionId", "test_session_001");
        
        DecisionExecutionResult result = unifiedAgentEngine.execute(
            agentId, query, userId, null, context
        );
        
        assertNotNull(result);
        log.info("带上下文执行结果: success={}", result.getSuccess());
    }
    
    @Test
    public void testAsyncExecution() {
        // 测试异步执行
        Long agentId = 1L;
        String query = "异步测试任务";
        Long userId = 1L;
        
        // 异步执行不应阻塞
        unifiedAgentEngine.executeAsync(agentId, query, userId, null, null);
        
        log.info("异步执行已提交");
        
        // 等待一段时间让异步任务完成
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
