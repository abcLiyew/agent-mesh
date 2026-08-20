package com.esdllm.agentmesh.service.agent.impl;

import cn.hutool.core.util.StrUtil;
import com.esdllm.agentmesh.config.HealthCheckProperties;
import com.esdllm.agentmesh.exception.BusinessException;
import com.esdllm.agentmesh.model.domain.McpServers;
import com.esdllm.agentmesh.model.domain.Tools;
import com.esdllm.agentmesh.model.dto.HealthCheckResult;
import com.esdllm.agentmesh.model.dto.HealthStatistics;
import com.esdllm.agentmesh.repository.dao.McpServersDao;
import com.esdllm.agentmesh.repository.dao.ToolsDao;
import com.esdllm.agentmesh.service.agent.ToolHealthCheckService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.net.SocketTimeoutException;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

/**
 * 工具健康检查服务实现类
 */
@Service
@Slf4j
public class ToolHealthCheckServiceImpl implements ToolHealthCheckService {

    @Resource
    private ToolsDao toolsDao;

    @Resource
    private HealthCheckProperties healthCheckProperties;

    @Resource
    private McpServersDao mcpServersDao;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ExecutorService executorService = Executors.newFixedThreadPool(10);

    @Scheduled(fixedRateString = "#{@healthCheckProperties.intervalMs}")
    @Override
    public void scheduledHealthCheck() {
        if (!healthCheckProperties.getEnabled()) {
            log.debug("工具健康检查已禁用");
            return;
        }

        log.info("开始执行定时工具健康检查");
        
        try {
            // 获取所有启用的工具
            List<Tools> tools = toolsDao.list()
                    .stream()
                    .filter(Tools::getIsEnabled)
                    .filter(tool -> tool.getIsDelete() == 0)
                    .toList();

            if (tools.isEmpty()) {
                log.debug("暂无可用工具需要检查");
                return;
            }

            // 并行执行健康检查
            List<CompletableFuture<HealthCheckResult>> futures = tools.stream()
                    .map(tool -> CompletableFuture.supplyAsync(
                            () -> checkHealthWithRetry(tool), 
                            executorService))
                    .toList();

            // 等待所有检查完成并保存结果
            for (CompletableFuture<HealthCheckResult> future : futures) {
                try {
                    HealthCheckResult result = future.join();
                    saveHealthCheckResult(result);
                } catch (Exception e) {
                    log.error("保存健康检查结果失败", e);
                }
            }

            log.info("工具健康检查完成，共检查 {} 个工具", tools.size());
            
        } catch (Exception e) {
            log.error("执行工具健康检查失败", e);
        }
    }

    @Override
    public HealthCheckResult checkHealth(Tools tool) {
        return checkHealthWithRetry(tool);
    }

    @Override
    public List<HealthCheckResult> batchCheckHealth(List<Tools> tools) {
        return tools.stream()
                .map(this::checkHealthWithRetry)
                .collect(Collectors.toList());
    }

    @Override
    public Integer getToolHealthStatus(Long toolId) {
        Tools tool = toolsDao.getById(toolId);
        if (tool == null) {
            throw new BusinessException(com.esdllm.agentmesh.common.ErrorCode.TOOL_NOT_FOUND, "工具不存在");
        }
        return tool.getHealthStatus() != null ? tool.getHealthStatus() : 0;
    }

    @Override
    public HealthCheckResult manualCheck(Long toolId) {
        Tools tool = toolsDao.getById(toolId);
        if (tool == null) {
            throw new BusinessException(com.esdllm.agentmesh.common.ErrorCode.TOOL_NOT_FOUND, "工具不存在");
        }

        HealthCheckResult result = checkHealthWithRetry(tool);
        saveHealthCheckResult(result);
        return result;
    }

    @Override
    public void resetHealthStatus(Long toolId) {
        Tools tool = toolsDao.getById(toolId);
        if (tool == null) {
            throw new BusinessException(com.esdllm.agentmesh.common.ErrorCode.TOOL_NOT_FOUND, "工具不存在");
        }

        tool.setHealthStatus(0);
        tool.setLastHealthCheck(new Date());
        tool.setConsecutiveFailures(0);
        tool.setLastErrorMessage(null);
        toolsDao.updateById(tool);
        
        log.info("重置工具健康状态，toolId: {}", toolId);
    }

    @Override
    public HealthStatistics getHealthStatistics() {
        List<Tools> allTools = toolsDao.list();
        
        int totalTools = allTools.size();
        int healthyTools = 0;
        int unhealthyTools = 0;
        int unknownTools = 0;
        int disabledTools = 0;
        long totalResponseTime = 0;
        int responseTimeCount = 0;

        for (Tools tool : allTools) {
            Integer status = tool.getHealthStatus();
            if (status == null || status == 0) {
                unknownTools++;
            } else if (status == 1) {
                healthyTools++;
            } else if (status == 2) {
                unhealthyTools++;
            } else if (status == 3) {
                disabledTools++;
            }
        }

        double healthRate = totalTools > 0 ? (double) healthyTools / totalTools * 100 : 0.0;

        return HealthStatistics.builder()
                .totalTools(totalTools)
                .healthyTools(healthyTools)
                .unhealthyTools(unhealthyTools)
                .unknownTools(unknownTools)
                .disabledTools(disabledTools)
                .healthRate(healthRate)
                .averageResponseTimeMs(0.0)
                .build();
    }

    /**
     * 带重试的健康检查
     */
    private HealthCheckResult checkHealthWithRetry(Tools tool) {
        int retryCount = 0;
        HealthCheckResult result = null;

        while (retryCount < healthCheckProperties.getMaxRetries()) {
            try {
                result = performSingleCheck(tool);
                
                if (result.getHealthy()) {
                    result.setRetryCount(retryCount);
                    result.setRetried(retryCount > 0);
                    return result;
                }
                
                // 检查失败，等待后重试
                retryCount++;
                if (retryCount < healthCheckProperties.getMaxRetries()) {
                    log.warn("工具健康检查失败，{} 秒后重试... toolId: {}, attempt: {}", 
                             healthCheckProperties.getRetryIntervalMs() / 1000, 
                             tool.getId(), retryCount);
                    Thread.sleep(healthCheckProperties.getRetryIntervalMs());
                }
                
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.error("健康检查被中断", e);
                break;
            } catch (Exception e) {
                log.error("健康检查异常", e);
                retryCount++;
                if (retryCount >= healthCheckProperties.getMaxRetries()) {
                    result = createFailedResult(tool, e.getMessage());
                }
            }
        }

        if (result == null) {
            result = createFailedResult(tool, "多次检查失败");
        }
        
        result.setRetryCount(retryCount);
        result.setRetried(retryCount > 0);
        return result;
    }

    /**
     * 执行单次健康检查
     */
    private HealthCheckResult performSingleCheck(Tools tool) {
        long startTime = System.currentTimeMillis();
        
        HealthCheckResult.HealthCheckResultBuilder builder = HealthCheckResult.builder()
                .toolId(tool.getId())
                .toolName(tool.getDisplayName())
                .checkTime(new Date());

        // 如果工具未启用，直接返回禁用状态
        if (!tool.getIsEnabled()) {
            return builder.healthy(false)
                    .healthStatus(3)
                    .statusCode(0)
                    .errorMessage("工具已禁用")
                    .responseTimeMs(0L)
                    .build();
        }

        try {
            // 根据工具类型执行不同的检查策略
            boolean healthy = executeHealthCheck(tool);
            long responseTime = System.currentTimeMillis() - startTime;

            return builder.healthy(healthy)
                    .healthStatus(healthy ? 1 : 2)
                    .statusCode(healthy ? 200 : 500)
                    .responseTimeMs(responseTime)
                    .build();

        } catch (HttpClientErrorException e) {
            long responseTime = System.currentTimeMillis() - startTime;
            return builder.healthy(false)
                    .healthStatus(2)
                    .statusCode(e.getStatusCode().value())
                    .errorMessage("客户端错误：" + e.getMessage())
                    .responseTimeMs(responseTime)
                    .build();
        } catch (HttpServerErrorException e) {
            long responseTime = System.currentTimeMillis() - startTime;
            return builder.healthy(false)
                    .healthStatus(2)
                    .statusCode(e.getStatusCode().value())
                    .errorMessage("服务端错误：" + e.getMessage())
                    .responseTimeMs(responseTime)
                    .build();
        } catch (ResourceAccessException e) {
            long responseTime = System.currentTimeMillis() - startTime;
            String errorMsg = e.getCause() instanceof SocketTimeoutException ? 
                    "连接超时" : "网络无法访问";
            return builder.healthy(false)
                    .healthStatus(2)
                    .statusCode(0)
                    .errorMessage(errorMsg + ": " + e.getMessage())
                    .responseTimeMs(responseTime)
                    .build();
        } catch (Exception e) {
            long responseTime = System.currentTimeMillis() - startTime;
            return builder.healthy(false)
                    .healthStatus(2)
                    .statusCode(0)
                    .errorMessage("检查异常：" + e.getMessage())
                    .responseTimeMs(responseTime)
                    .build();
        }
    }

    /**
     * 执行具体的健康检查逻辑
     */
    private boolean executeHealthCheck(Tools tool) {
        // 对于 HTTP 类型的工具，发送测试请求
        if ("USER_HTTP".equals(tool.getSourceType()) && 
            tool.getCustomEndpointUrl() != null && 
            !tool.getCustomEndpointUrl().isEmpty()) {
            
            return checkHttpEndpoint(tool.getCustomEndpointUrl());
        }
        
        // 对于 MCP 工具，检查 MCP 服务器状态
        if ("USER_MCP".equals(tool.getSourceType()) && tool.getMcpServerId() != null) {
            return checkMcpServer(tool.getMcpServerId());
        }
        
        // 对于系统工具和智能体工具，默认健康
        if ("SYSTEM".equals(tool.getSourceType()) || "USER_AGENT".equals(tool.getSourceType())) {
            return true;
        }
        
        // 其他类型默认返回健康
        return true;
    }

    /**
     * 检查 HTTP 端点
     */
    private boolean checkHttpEndpoint(String url) {
        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    null,
                    String.class
            );
            return response.getStatusCode().is2xxSuccessful();
        } catch (Exception e) {
            log.warn("HTTP 端点检查失败：{}", url, e);
            throw e;
        }
    }

    /**
     * 检查 MCP 服务器状态
     */
    private boolean checkMcpServer(Long mcpServerId) {
        log.debug("检查 MCP 服务器状态，serverId: {}", mcpServerId);

        try {
            // 查询 MCP 服务器信息
            McpServers mcpServer = mcpServersDao.getById(mcpServerId);
            if (mcpServer == null) {
                log.warn("MCP 服务器不存在，serverId: {}", mcpServerId);
                return false;
            }

            // 检查服务器状态字段
            if (mcpServer.getStatus() == 0) {
                log.debug("MCP 服务器已停止，serverId: {}", mcpServerId);
                return false;
            }

            // 根据传输类型检查连接
            String transportType = mcpServer.getTransportType();
            String endpointUrl = mcpServer.getEndpointUrl();

            if ("SSE".equals(transportType) || "STREAMABLE_HTTP".equals(transportType)) {
                // HTTP 类型的检查
                if (StrUtil.isBlank(endpointUrl)) {
                    log.warn("MCP 服务器端点 URL 为空，serverId: {}", mcpServerId);
                    return false;
                }

                // 发送 GET 请求检查服务是否可达
                ResponseEntity<String> response = restTemplate.exchange(
                        endpointUrl,
                        HttpMethod.GET,
                        null,
                        String.class
                );

                boolean isHealthy = response.getStatusCode().is2xxSuccessful();
                log.debug("MCP 服务器 HTTP 检查结果：{}, serverId: {}, status: {}",
                        isHealthy, mcpServerId, response.getStatusCode());
                return isHealthy;

            } else if ("STDIO".equals(transportType)) {
                // STDIO 模式无法直接检查，返回 true（假设进程在运行）
                log.debug("STDIO 模式的 MCP 服务器，无法直接检查，serverId: {}", mcpServerId);
                return true;
            }

            return false;

        } catch (Exception e) {
            log.error("检查 MCP 服务器失败，serverId: {}", mcpServerId, e);
            return false;
        }
    }

    /**
     * 创建失败结果
     */
    private HealthCheckResult createFailedResult(Tools tool, String errorMessage) {
        return HealthCheckResult.builder()
                .toolId(tool.getId())
                .toolName(tool.getDisplayName())
                .healthy(false)
                .healthStatus(2)
                .statusCode(0)
                .errorMessage(errorMessage)
                .responseTimeMs(0L)
                .checkTime(new Date())
                .build();
    }

    /**
     * 保存健康检查结果
     */
    private void saveHealthCheckResult(HealthCheckResult result) {
        Tools tool = toolsDao.getById(result.getToolId());
        if (tool == null) {
            log.warn("工具不存在，无法保存健康检查结果，toolId: {}", result.getToolId());
            return;
        }

        // 更新健康状态
        tool.setHealthStatus(result.getHealthStatus());
        tool.setLastHealthCheck(result.getCheckTime());

        // 更新失败计数
        if (result.getHealthy()) {
            // 检查成功
            if (tool.getConsecutiveFailures() != null && tool.getConsecutiveFailures() > 0) {
                log.info("工具健康检查恢复，toolId: {}, toolName: {}", 
                         tool.getId(), tool.getDisplayName());
            }
            tool.setConsecutiveFailures(0);
            tool.setLastErrorMessage(null);
        } else {
            // 检查失败
            int currentFailures = tool.getConsecutiveFailures() != null ? 
                    tool.getConsecutiveFailures() : 0;
            tool.setConsecutiveFailures(currentFailures + 1);
            tool.setLastErrorMessage(result.getErrorMessage());

            // 如果超过阈值，标记为异常
            if (tool.getConsecutiveFailures() >= healthCheckProperties.getFailureThreshold()) {
                log.warn("工具连续失败超过阈值，标记为异常，toolId: {}, failures: {}", 
                         tool.getId(), tool.getConsecutiveFailures());
            }
        }

        tool.setUpdatedAt(new Date());
        toolsDao.updateById(tool);
    }
}
