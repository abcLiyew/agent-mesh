package com.esdllm.agentmesh.service.unified;

import com.esdllm.agentmesh.service.unified.impl.AgentOrchestrator;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 多智能体协同功能测试
 */
@SpringBootTest
@Slf4j
public class AgentCollaborationTest {
    
    @Resource
    private UnifiedAgentEngine unifiedAgentEngine;
    
    @Resource
    private AgentOrchestrator agentOrchestrator;
    
    /**
     * 测试多智能体协同执行
     */
    @Test
    public void testExecuteCollaboratively() {
        // 准备测试数据
        Long mainAgentId = 1L;
        String query = "帮我分析当前市场趋势并生成综合报告";
        Long userId = 1L;
        Map<String, Object> context = new HashMap<>();
        
        log.info("=== 开始多智能体协同测试 ===");
        log.info("主智能体: {}, 查询: {}", mainAgentId, query);
        
        // 执行协同
        Object result = unifiedAgentEngine.executeCollaboratively(
            mainAgentId, query, userId, context
        );
        
        // 验证结果
        assertNotNull(result, "协同执行结果不应为空");
        
        if (result instanceof AgentOrchestrator.CollaborativeExecutionResult) {
            AgentOrchestrator.CollaborativeExecutionResult collabResult = 
                (AgentOrchestrator.CollaborativeExecutionResult) result;
            
            log.info("协同执行成功: {}", collabResult.isSuccess());
            log.info("总耗时: {}ms", collabResult.getTotalExecutionTimeMs());
            log.info("参与智能体数: {}", collabResult.getParticipatingAgents().size());
            
            if (collabResult.getMergedResult() != null) {
                log.info("合并结果成功: {}", collabResult.getMergedResult().getSuccess());
                log.info("最终响应长度: {} 字符", 
                    collabResult.getMergedResult().getFinalResponse() != null ? 
                    collabResult.getMergedResult().getFinalResponse().length() : 0);
            }
            
            // 打印子智能体结果
            if (collabResult.getSubAgentResults() != null && !collabResult.getSubAgentResults().isEmpty()) {
                log.info("=== 子智能体执行详情 ===");
                collabResult.getSubAgentResults().forEach(subResult -> {
                    log.info("智能体 {} ({}): 成功={}, 耗时={}ms", 
                        subResult.getAgentName(),
                        subResult.getAgentId(),
                        subResult.isSuccess(),
                        subResult.getExecutionTimeMs()
                    );
                });
            }
            
        } else {
            log.warn("返回结果类型不是预期的CollaborativeExecutionResult: {}", result.getClass().getName());
        }
        
        log.info("=== 多智能体协同测试完成 ===");
    }
    
    /**
     * 测试单智能体执行(不需要协作的场景)
     */
    @Test
    public void testSingleAgentExecution() {
        Long agentId = 1L;
        String query = "你好";
        Long userId = 1L;
        
        log.info("=== 测试单智能体执行(简单任务) ===");
        
        Object result = unifiedAgentEngine.executeCollaboratively(
            agentId, query, userId, null
        );
        
        assertNotNull(result);
        log.info("单智能体执行完成");
    }
}
