package com.esdllm.agentmesh.service;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 智能体依赖关系信息
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AgentDependency {
    
    /**
     * 智能体 ID
     */
  private Long agentId;
    
    /**
     * 被依赖的智能体 ID
     */
  private Long dependsOnAgentId;
    
    /**
     * 依赖类型：CALL, DATA_SHARE, WORKFLOW
     */
  private String dependencyType;
    
    /**
     * 优先级：数字越小优先级越高
     */
  private Integer priority;
}
