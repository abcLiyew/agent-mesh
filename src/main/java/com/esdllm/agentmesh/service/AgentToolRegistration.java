package com.esdllm.agentmesh.service;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 智能体工具注册信息
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AgentToolRegistration {
    
    /**
     * 智能体 ID
     */
  private Long agentId;
    
    /**
     * 工具代码名
     */
  private String toolCodeName;
    
    /**
     * 工具显示名
     */
  private String displayName;
    
    /**
     * 工具描述
     */
  private String description;
    
    /**
     * 输入 Schema（JSON 字符串）
     */
  private Object inputSchema;
    
    /**
     * 输出 Schema（JSON 字符串）
     */
  private Object outputSchema;
}

