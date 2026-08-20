package com.esdllm.agentmesh.model.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 意图识别结果
 */
@Data
public class IntentRecognitionResult {
    
    /**
     * 识别的意图类型
     */
  private String intentType;
    
    /**
     * 置信度 (0-1)
     */
  private BigDecimal confidence;
    
    /**
     * 提取的参数
     */
  private Map<String, Object> parameters;
    
    /**
     * 匹配的工具 ID 列表
     */
  private List<Long> matchedToolIds;
    
    /**
     * 匹配的知识库 ID 列表
     */
  private List<Long> matchedKbIds;
    
    /**
     * 推荐使用的模型
     */
  private String recommendedModel;
    
    /**
     * 是否需要调用外部工具
     */
  private Boolean needToolCall;
    
    /**
     * 原始用户问题
     */
  private String originalQuery;
}
