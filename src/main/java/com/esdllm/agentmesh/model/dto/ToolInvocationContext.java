package com.esdllm.agentmesh.model.dto;

import lombok.Builder;
import lombok.Data;
import java.util.Map;

/**
 * 工具调用上下文
 */
@Data
@Builder
public class ToolInvocationContext {
    
    /**
     * 工具 ID
     */
  private Long toolId;
    
    /**
     * 工具类型：HTTP, MCP, SYSTEM
     */
  private String toolType;
    
    /**
     * 输入参数
     */
  private Map<String, Object> parameters;
    
    /**
     * 超时时间（毫秒）
     */
  private Long timeoutMs;
    
    /**
     * 重试次数
     */
  private Integer retryCount;
}
