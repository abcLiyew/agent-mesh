package com.esdllm.agentmesh.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * RAG 配置属性
 */
@Data
@Component
@ConfigurationProperties(prefix = "rag")
public class RagProperties {
    
    /**
     * 工具匹配阶段的相似度阈值
     */
    private Double toolMatchingThreshold = 0.5;
    
    /**
     * 最终回答阶段的相似度阈值
     */
    private Double responseThreshold = 0.6;
    
    /**
     * 工具匹配阶段返回的最大结果数
     */
    private Integer toolMatchingTopK = 3;
    
    /**
     * 最终回答阶段返回的最大结果数
     */
    private Integer responseTopK = 5;
    
    /**
     * RAG 增强的最低得分阈值
     */
    private Double minScoreThreshold = 0.5;
    
    /**
     * 是否启用 RAG 工具推荐
     */
    private Boolean enableToolRecommendation = true;
}
