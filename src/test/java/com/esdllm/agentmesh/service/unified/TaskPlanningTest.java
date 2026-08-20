package com.esdllm.agentmesh.service.unified;

import com.esdllm.agentmesh.model.dto.TaskExecutionPlan;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 任务规划功能测试
 */
@SpringBootTest
@Slf4j
public class TaskPlanningTest {
    
    @Resource
    private UnifiedAgentEngine unifiedAgentEngine;
    
    @Test
    public void testPlanTask() {
        // 测试任务规划
        Long agentId = 1L;
        String query = "帮我查询订单ORDER123的状态";
        Long userId = 1L;
        
        TaskExecutionPlan plan = unifiedAgentEngine.planTask(agentId, query, userId, null);
        
        assertNotNull(plan);
        assertNotNull(plan.getTaskId());
        assertEquals(query, plan.getTaskDescription());
        assertFalse(plan.getSteps().isEmpty());
        
        log.info("=== 任务规划结果 ===");
        log.info("任务ID: {}", plan.getTaskId());
        log.info("步骤数量: {}", plan.getSteps().size());
        log.info("预估耗时: {}ms", plan.getEstimatedDurationMs());
        
        // 打印所有步骤
        plan.getSteps().forEach(step -> {
            log.info("步骤 {}: {} (类型: {}, 预估: {}ms)", 
                    step.getStepNumber(),
                    step.getDescription(),
                    step.getStepType(),
                    step.getEstimatedDurationMs());
        });
        
        // 验证至少包含意图识别和回答生成步骤
        assertTrue(plan.getSteps().stream()
            .anyMatch(s -> "INTENT_RECOGNITION".equals(s.getStepType())));
        assertTrue(plan.getSteps().stream()
            .anyMatch(s -> "RESPONSE_GENERATION".equals(s.getStepType())));
    }
    
    @Test
    public void testExecutePlannedTask() {
        // 1. 先规划任务
        Long agentId = 1L;
        String query = "你好，请介绍一下你自己";
        Long userId = 1L;
        
        TaskExecutionPlan plan = unifiedAgentEngine.planTask(agentId, query, userId, null);
        assertNotNull(plan);
        
        log.info("任务规划完成，taskId: {}", plan.getTaskId());
        
        // 2. 执行任务（执行所有步骤）
        var result = unifiedAgentEngine.executePlannedTask(
            plan.getTaskId(), 
            null, // null表示执行所有步骤
            userId
        );
        
        assertNotNull(result);
        log.info("任务执行完成，success: {}", result.getSuccess());
    }
    
    @Test
    public void testExecutePartialSteps() {
        // 1. 规划复杂任务
        String query = "帮我查询订单并申请退款";
        TaskExecutionPlan plan = unifiedAgentEngine.planTask(1L, query, 1L, null);
        
        // 2. 只执行部分步骤（跳过可选步骤）
        List<String> confirmedSteps = plan.getSteps().stream()
            .filter(step -> step.getIsRequired()) // 只执行必选步骤
            .map(TaskExecutionPlan.TaskStep::getStepId)
            .toList();
        
        log.info("确认执行的步骤: {}", confirmedSteps);
        
        var result = unifiedAgentEngine.executePlannedTask(
            plan.getTaskId(),
            confirmedSteps,
            1L
        );
        
        assertNotNull(result);
    }
    
    @Test
    public void testExpiredTaskPlan() {
        // 1. 规划任务
        TaskExecutionPlan plan = unifiedAgentEngine.planTask(
            1L, "测试任务", 1L, null
        );
        
        String taskId = plan.getTaskId();
        log.info("任务ID: {}", taskId);
        
        // 2. 模拟过期（实际应该等待5分钟，这里仅做逻辑测试）
        // 在生产环境中，过期的任务会被自动清理
        
        // 3. 尝试执行（如果未过期应该成功）
        try {
            var result = unifiedAgentEngine.executePlannedTask(taskId, null, 1L);
            log.info("执行成功");
        } catch (Exception e) {
            log.warn("执行失败（可能已过期）: {}", e.getMessage());
        }
    }
}
