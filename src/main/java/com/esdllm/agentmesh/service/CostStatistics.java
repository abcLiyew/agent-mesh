package com.esdllm.agentmesh.service;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * 成本统计信息
 */
public record CostStatistics(
    BigDecimal totalCost,
    Integer totalCalls,
    Integer totalTokens,
    Map<String, BigDecimal> costByModel,
    Map<String, Integer> callsByAgent
) {}

