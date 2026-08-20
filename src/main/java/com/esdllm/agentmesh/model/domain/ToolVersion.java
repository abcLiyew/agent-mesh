package com.esdllm.agentmesh.model.domain;

import com.baomidou.mybatisplus.annotation.*;
import com.esdllm.agentmesh.config.PostgreSqlJsonbTypeHandler;
import lombok.Data;

import java.util.Date;

/**
 * 工具版本管理实体
 */
@Data
@TableName(value = "tool_version", autoResultMap = true)
public class ToolVersion {
    
    @TableId
    private Long id;
    
    private Long toolId;
    
    private String versionNumber;
    
    private String versionName;
    
    private String description;
    
    private String sourceType;
    
    @TableField(typeHandler = PostgreSqlJsonbTypeHandler.class)
    private Object inputSchema;
    
    @TableField(typeHandler = PostgreSqlJsonbTypeHandler.class)
    private Object outputSchema;
    
    private String customEndpointUrl;
    
    private Long mcpServerId;
    
    private Boolean isActive;
    
    private Boolean isCurrent;
    
    private Long parentVersionId;
    
    private String changeLog;
    
    private Long createdBy;
    
    private Date createdAt;
}
