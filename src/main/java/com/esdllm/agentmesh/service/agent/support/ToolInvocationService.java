package com.esdllm.agentmesh.service.agent.support;

import cn.hutool.core.thread.ThreadFactoryBuilder;
import cn.hutool.core.util.StrUtil;
import com.esdllm.agentmesh.common.ErrorCode;
import com.esdllm.agentmesh.exception.BusinessException;
import com.esdllm.agentmesh.model.domain.ToolVersion;
import com.esdllm.agentmesh.model.domain.Tools;
import com.esdllm.agentmesh.model.dto.ToolInvocationContext;
import com.esdllm.agentmesh.repository.dao.ToolVersionDao;
import com.esdllm.agentmesh.model.dto.response.AgentToolResponse;
import com.esdllm.agentmesh.service.AgentToolService;
import jakarta.annotation.PreDestroy;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.*;

/**
 * 工具调用服务：处理 HTTP、MCP、Agent 工具的调用逻辑
 */
@Service
@Slf4j
public class ToolInvocationService {

    @Resource
    private ToolVersionDao toolVersionDao;

    @Resource
    private AgentToolService agentToolService;

    private static final int MAX_RECURSION_DEPTH = 5;
    private static final long AGENT_TOOL_TIMEOUT_MS = 30000L;
    private final ExecutorService agentToolExecutor = new ThreadPoolExecutor(
        5,  // core pool size
        20, // maximum pool size
        60L, TimeUnit.SECONDS, // keep alive time
        new LinkedBlockingQueue<>(100), // work queue
        new ThreadFactoryBuilder().setNamePrefix("agent-tool-").build(),
        new ThreadPoolExecutor.CallerRunsPolicy() // rejection policy
    );

    /**
     * 加载工具的指定版本或当前激活版本
     */
    public Tools loadToolVersion(Tools originalTool, ToolInvocationContext context) {
        Long toolId = originalTool.getId();
        Long requestedVersionId = null;

        if (context.getParameters() != null && context.getParameters().containsKey("toolVersionId")) {
            try {
                requestedVersionId = Long.valueOf(context.getParameters().get("toolVersionId").toString());
                log.debug("请求使用工具版本：toolId={}, versionId={}", toolId, requestedVersionId);
            } catch (NumberFormatException e) {
                log.warn("版本号格式错误，使用默认版本");
            }
        }

        if (requestedVersionId != null) {
            ToolVersion requestedVersion = toolVersionDao.getById(requestedVersionId);
            if (requestedVersion != null && requestedVersion.getToolId().equals(toolId)) {
                log.info("使用指定版本的工具：toolId={}, versionId={}, versionNumber={}",
                        toolId, requestedVersionId, requestedVersion.getVersionNumber());
                return applyVersionSnapshot(originalTool, requestedVersion);
            } else {
                log.warn("请求的版本不存在，使用当前激活版本：toolId={}, versionId={}",
                        toolId, requestedVersionId);
            }
        }

        ToolVersion activeVersion = toolVersionDao.getActiveVersion(toolId);
        if (activeVersion != null) {
            log.info("使用激活版本的工具：toolId={}, versionId={}, versionNumber={}",
                    toolId, activeVersion.getId(), activeVersion.getVersionNumber());
            return applyVersionSnapshot(originalTool, activeVersion);
        }

        log.debug("工具未配置版本，使用原始配置：toolId={}", toolId);
        return originalTool;
    }

    /**
     * 调用 HTTP 工具
     */
    public String invokeHttpTool(ToolInvocationContext context, Tools tool) {
        log.info("调用 HTTP 工具，url: {}, toolId: {}",
                context.getParameters().get("endpointUrl"), tool.getId());

        try {
            RestTemplate restTemplate = new RestTemplate();

            String endpointUrl = StrUtil.isNotBlank(tool.getCustomEndpointUrl()) ?
                    tool.getCustomEndpointUrl() :
                    (String) context.getParameters().get("endpointUrl");

            if (StrUtil.isBlank(endpointUrl)) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "HTTP 工具端点 URL 不能为空");
            }

            Map<String, Object> requestBody = new HashMap<>(context.getParameters());
            requestBody.remove("endpointUrl");
            requestBody.remove("toolVersionId");

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            if (context.getParameters().containsKey("apiKey")) {
                headers.set("Authorization", "Bearer " + context.getParameters().get("apiKey"));
            }

            HttpEntity<Object> request = new HttpEntity<>(requestBody, headers);

            ResponseEntity<String> response = restTemplate.postForEntity(endpointUrl, request, String.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("HTTP 工具调用成功，status: {}", response.getStatusCode());
                return response.getBody();
            } else {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR,
                        "HTTP 工具调用失败：" + response.getStatusCode());
            }

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("HTTP 工具调用失败", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR,
                    "HTTP 工具调用失败：" + e.getMessage());
        }
    }

    /**
     * 调用 MCP 工具
     */
    public String invokeMcpTool(ToolInvocationContext context, Tools tool) {
        log.info("调用 MCP 工具，toolId: {}", tool.getId());

        try {
            String sseEndpoint = (String) context.getParameters().get("sseEndpoint");
            if (StrUtil.isBlank(sseEndpoint)) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "MCP 服务 SSE 端点不能为空");
            }

            WebClient webClient = WebClient.builder().build();

            Map<String, Object> params = new HashMap<>(context.getParameters());
            params.remove("toolVersionId");

            String result = webClient.post()
                    .uri(sseEndpoint + "/invoke")
                    .bodyValue(params)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block(Duration.ofSeconds(context.getTimeoutMs() / 1000));

            log.info("MCP 工具调用成功");
            return result != null ? result : "MCP 工具执行成功";

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("MCP 工具调用失败", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR,
                    "MCP 工具调用失败：" + e.getMessage());
        }
    }

    /**
     * 调用 Agent 工具
     */
    public String invokeAgentTool(Tools tool, ToolInvocationContext context, Map<String, Object> traceContext) {
        long startTime = System.currentTimeMillis();
        String toolCodeName = tool.getToolCodeName();
        Long targetAgentId = tool.getMcpServerId();

        log.info("开始调用智能体工具，toolCodeName: {}, agentId: {}", toolCodeName, targetAgentId);

        try {
            int currentDepth = Optional.ofNullable((Integer) context.getParameters().get("__recursion_depth"))
                    .orElse(0);

            if (currentDepth >= MAX_RECURSION_DEPTH) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR,
                        "智能体调用层级过深（最大" + MAX_RECURSION_DEPTH + "层），已终止");
            }

            if (targetAgentId == null) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR,
                        "智能体工具的 mcpServerId 应存储被调用智能体的 ID");
            }

            String query = (String) context.getParameters().getOrDefault("query", "");
            if (StrUtil.isBlank(query)) {
                query = tool.getDescription() + ": " + context.getParameters();
            }

            Map<String, Object> params = new HashMap<>(context.getParameters());
            params.put("__recursion_depth", currentDepth + 1);
            params.put("__caller_agent_id", targetAgentId);
            params.put("__start_time", startTime);
            params.remove("toolVersionId");

            Long userId = getUserIdFromContext(context);

            String finalQuery = query;
            Callable<AgentToolResponse> task = () ->
                agentToolService.invokeAgentTool(targetAgentId, finalQuery, params, userId);

            Future<AgentToolResponse> future = agentToolExecutor.submit(task);
            AgentToolResponse response;

            try {
                response = future.get(AGENT_TOOL_TIMEOUT_MS, TimeUnit.MILLISECONDS);
                log.info("智能体响应获取成功，agentId: {}", targetAgentId);
            } catch (TimeoutException e) {
                future.cancel(true);
                throw new BusinessException(ErrorCode.SYSTEM_ERROR,
                        "智能体工具调用超时（>" + AGENT_TOOL_TIMEOUT_MS + "ms）");
            } catch (ExecutionException e) {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR,
                        "智能体工具调用异常：" + e.getCause().getMessage());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "智能体调用被中断");
            }

            long executionTime = System.currentTimeMillis() - startTime;
            recordAgentToolCall(traceContext, targetAgentId, toolCodeName, executionTime, true);

            return response.result();

        } catch (BusinessException e) {
            recordAgentToolCall(traceContext, targetAgentId, toolCodeName,
                    System.currentTimeMillis() - startTime, false);
            throw e;
        } catch (Exception e) {
            recordAgentToolCall(traceContext, targetAgentId, toolCodeName,
                    System.currentTimeMillis() - startTime, false);
            throw e;
        }
    }

    // ==================== 辅助方法 ====================

    /**
     * 应用版本快照到工具对象
     */
    private Tools applyVersionSnapshot(Tools originalTool, ToolVersion version) {
        Tools versionedTool = new Tools();
        versionedTool.setId(originalTool.getId());
        versionedTool.setOwnerId(originalTool.getOwnerId());
        versionedTool.setSourceType(version.getSourceType());
        versionedTool.setToolCodeName(originalTool.getToolCodeName());
        versionedTool.setDisplayName(originalTool.getDisplayName());
        versionedTool.setDescription(originalTool.getDescription());
        versionedTool.setMcpServerId(version.getMcpServerId());
        versionedTool.setInputSchema(version.getInputSchema());
        versionedTool.setOutputSchema(version.getOutputSchema());
        versionedTool.setCustomEndpointUrl(version.getCustomEndpointUrl());
        versionedTool.setIsEnabled(originalTool.getIsEnabled());
        versionedTool.setIsDelete(originalTool.getIsDelete());
        versionedTool.setCreatedAt(originalTool.getCreatedAt());
        versionedTool.setUpdatedAt(originalTool.getUpdatedAt());
        return versionedTool;
    }

    /**
     * 记录智能体工具调用
     */
    @SuppressWarnings("unchecked")
    private void recordAgentToolCall(Map<String, Object> traceContext, Long agentId,
                                     String toolCodeName, long executionTime, boolean success) {
        if (traceContext.get("call_chain") instanceof java.util.List callChain) {
            Map<String, Object> callRecord = new HashMap<>();
            callRecord.put("agent_id", agentId);
            callRecord.put("tool_code_name", toolCodeName);
            callRecord.put("execution_time_ms", executionTime);
            callRecord.put("success", success);
            callRecord.put("timestamp", System.currentTimeMillis());
            callRecord.put("type", "AGENT_TOOL");
            callChain.add(callRecord);
        }
    }

    /**
     * 从上下文获取用户 ID
     */
    private Long getUserIdFromContext(ToolInvocationContext context) {
        Object userIdObj = context.getParameters().get("userId");
        if (userIdObj != null) {
            return Long.valueOf(userIdObj.toString());
        }
        throw new BusinessException(ErrorCode.NO_AUTH, "无法获取用户 ID");
    }

    @PreDestroy
    public void destroy() {
        agentToolExecutor.shutdown();
        try {
            if (!agentToolExecutor.awaitTermination(30, TimeUnit.SECONDS)) {
                agentToolExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            agentToolExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
