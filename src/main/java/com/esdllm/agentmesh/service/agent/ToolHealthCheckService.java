package com.esdllm.agentmesh.service.agent;

import com.esdllm.agentmesh.model.domain.Tools;
import com.esdllm.agentmesh.model.dto.HealthCheckResult;
import com.esdllm.agentmesh.model.dto.HealthStatistics;
import org.springframework.scheduling.annotation.Scheduled;

import java.util.List;

/**
 * 工具健康检查服务
 */
public interface ToolHealthCheckService {

    @Scheduled(fixedRateString = "#{@healthCheckProperties.intervalMs}")
    void scheduledHealthCheck();

    /**
     * 执行单次健康检查
     * @param tool 待检查的工具
     * @return 检查结果
     */
    HealthCheckResult checkHealth(Tools tool);

    /**
     * 批量执行健康检查
     * @param tools 待检查的工具列表
     * @return 检查结果列表
     */
    List<HealthCheckResult> batchCheckHealth(List<Tools> tools);

    /**
     * 获取工具的健康状态
     * @param toolId 工具 ID
     * @return 健康状态
     */
    Integer getToolHealthStatus(Long toolId);

    /**
     * 手动触发工具健康检查
     * @param toolId 工具 ID
     * @return 检查结果
     */
    HealthCheckResult manualCheck(Long toolId);

    /**
     * 重置工具的健康状态
     * @param toolId 工具 ID
     */
    void resetHealthStatus(Long toolId);

    /**
     * 获取所有工具的健康状态统计
     * @return 统计信息
     */
    HealthStatistics getHealthStatistics();
}
