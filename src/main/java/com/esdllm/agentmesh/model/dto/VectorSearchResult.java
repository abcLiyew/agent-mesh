package com.esdllm.agentmesh.model.dto;

/**
     * 向量检索结果
     */
public record VectorSearchResult(
            String id,
            String content,
            Double similarity,
            Object metadata
    ) {}