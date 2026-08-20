package com.esdllm.agentmesh.model.dto;

import lombok.Data;
import java.util.Map;

/**
 * 决策步骤
 */
@Data
public class DecisionStep {
    
    /**
     * 步骤 ID（用于前端追踪）
     */
  private String stepId;
    
    /**
     * 步骤类型：INTENT_RECOGNITION, TOOL_MATCHING, TOOL_CALL, MODEL_RESPONSE
     */
  private String stepType;
    
    /**
     * 步骤描述
     */
  private String description;
    
    /**
     * 使用的工具/模型 ID
     */
  private Long resourceId;
    
    /**
     * 使用的模型 ID（用于 MODEL_RESPONSE 步骤）
     */
  private Long modelId;
    
    /**
     * 输入数据
     */
  private Map<String, Object> inputData;
    
    /**
     * 输出数据
     */
  private Object outputData;
    
    /**
     * 耗时（毫秒）
     */
  private Long durationMs;
    
    /**
     * 成本（元）
     */
  private Double cost;
    
    /**
     * 状态：PENDING, RUNNING, COMPLETED, FAILED
     */
  private String status;
    
    /**
     * 错误信息（如果有）
     */
  private String errorMessage;
    
    /**
     * 子步骤（用于嵌套展示）
     */
  private java.util.List<DecisionStep> subSteps;
}
