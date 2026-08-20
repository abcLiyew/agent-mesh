package com.esdllm.agentmesh.service;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor; /**
 * 智能体工具调用请求
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AgentToolInvocation {
    
    /**
     * 智能体 ID
     */
  private Long agentId;
    
    /**
     * 查询文本
     */
  private String query;
    
    /**
     * 参数
     */
  private Object parameters;
    
    /**
     * 用户 ID
     */
  private Long userId;
}
