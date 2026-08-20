package com.esdllm.agentmesh.model.dto.response;

import com.esdllm.agentmesh.model.dto.tool.ToolSchemaConfig;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

import java.util.Date;

@Data
public class AgentResponse {

    private Long id;
    private Long userId;
    private String name;
    private String description;
    private String avatarUrl;
    private String systemPrompt;
    private String roleDefinition;
    private Long decisionModelId;
    private Long responseModelId;
    private Boolean isToolEnabled;

    /**
     * 工具配置对象（推荐方式）
     */
    private ToolSchemaConfig toolSchemaConfig;

    /**
     * 工具配置 JSON 字符串（兼容旧版本）
     */
    @JsonIgnore
    private String toolSchemaJsonStr;

    /**
     * 为了向后兼容，保留此字段但标记为废弃
     */
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    public String getToolSchemaJson() {
        return toolSchemaJsonStr;
    }

    @Deprecated
    public void setToolSchemaJson(String toolSchemaJson) {
        this.toolSchemaJsonStr = toolSchemaJson;
    }

    private String version;
    private Integer status;
    private Date createdAt;
    private Date updatedAt;
}
