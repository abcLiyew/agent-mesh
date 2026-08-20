package com.esdllm.agentmesh.model.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

/**
 * 智能体依赖关系实体
 */
@Data
@TableName("agent_dependency")
public class AgentDependencyEntity {
    
    /**
     * 主键 ID
     */
    @TableId
    private Long id;
    
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
    
    /**
     * 创建人用户 ID
     */
    private Long createdBy;
    
    /**
     * 创建时间
     */
    private Date createdAt;
    
    /**
     * 更新时间
     */
    private Date updatedAt;
}
