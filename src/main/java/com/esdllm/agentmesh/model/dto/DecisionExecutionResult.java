package com.esdllm.agentmesh.model.dto;

import lombok.Data;
import java.util.List;
import java.util.Map;

/**
 * 决策执行结果
 */
@Data
public class DecisionExecutionResult {
    
    /**
     * 最终回答内容
     */
  private String finalResponse;
    
    /**
     * 使用的决策路径
     */
  private List<DecisionStep> decisionPath;
    
    /**
     * 内部决策使用的模型
     */
  private String internalModel;
    
    /**
     * 最终回答使用的模型
     */
  private String responseModel;
    
    /**
     * 总成本（元）
     */
  private Double totalCost;
    
    /**
     * 执行耗时（毫秒）
     */
  private Long executionTimeMs;
    
    /**
     * 是否成功
     */
  private Boolean success;
    
    /**
     * 错误信息（如果有）
     */
  private String errorMessage;
    
    /**
     * 调用链追踪信息（可观测性）
     */
  private CallChainTrace callChainTrace;
    
    /**
     * 性能统计信息
     */
  private PerformanceStats performanceStats;
    
    /**
     * 调用链追踪数据
     */
  @Data
  public static class CallChainTrace {
      /**
       * 根智能体 ID
       */
      private Long rootAgentId;
      
      /**
       * 完整调用链记录
       */
      private List<Map<String, Object>> callRecords;
      
      /**
       * 调用拓扑结构（JSON 格式）
       */
      private String callTopology;
  }
    
    /**
     * 性能统计数据
     */
  @Data
  public static class PerformanceStats {
      /**
       * 总调用次数
       */
      private Integer totalCalls;
      
      /**
       * 智能体工具调用次数
       */
      private Integer agentToolCalls;
      
      /**
       * 普通工具调用次数
       */
      private Integer toolCalls;
      
      /**
       * 成功调用次数
       */
      private Integer successCount;
      
      /**
       * 失败调用次数
       */
      private Integer failureCount;
      
      /**
       * 平均执行时间（毫秒）
       */
      private Double avgExecutionTimeMs;
      
      /**
       * 最大执行时间（毫秒）
       */
      private Long maxExecutionTimeMs;
      
      /**
       * 最小执行时间（毫秒）
       */
      private Long minExecutionTimeMs;
  }
}

