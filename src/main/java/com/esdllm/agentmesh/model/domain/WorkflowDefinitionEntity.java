package com.esdllm.agentmesh.model.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.esdllm.agentmesh.config.PostgreSqlJsonbTypeHandler;
import lombok.Data;

import java.util.Date;
import java.util.Map;

/**
 * 工作流定义实体
 */
@TableName(value = "workflow_definition", autoResultMap = true)
@Data
public class WorkflowDefinitionEntity {
    
    /**
     * 主键ID
     */
    @TableId
    private Long id;
    
    /**
     * 工作流名称
     */
    private String workflowName;
    
    /**
     * 工作流描述
     */
    private String description;
    
    /**
     * 关联的智能体ID
     */
    private Long agentId;
    
    /**
     * 工作流版本
     */
    private String version;
    
    /**
     * 节点定义(JSON格式)
     */
    private Object nodesJson;
    
    /**
     * 起始节点ID
     */
    private String startNodeId;
    
    /**
     * 全局变量(JSON格式)
     */
    private Object globalVariablesJson;
    
    /**
     * 超时时间(毫秒)
     */
    private Long timeoutMs;
    
    /**
     * 是否启用
     */
    private Boolean enabled;
    
    /**
     * 创建者用户ID
     */
    private Long userId;
    
    /**
     * 创建时间
     */
    private Date createdAt;
    
    /**
     * 更新时间
     */
    private Date updatedAt;
    
    /**
     * 逻辑删除标记
     */
    private Integer isDelete;
}
