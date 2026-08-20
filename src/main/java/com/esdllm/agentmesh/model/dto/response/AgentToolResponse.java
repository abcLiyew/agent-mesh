package com.esdllm.agentmesh.model.dto.response;

/**
 * 智能体工具调用响应
 */
public record AgentToolResponse(
   String result,
    Long executionTimeMs,
    Double cost
) {}
