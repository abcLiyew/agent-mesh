package com.esdllm.agentmesh.model.domain;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.util.Date;

import com.esdllm.agentmesh.config.PostgreSqlJsonbTypeHandler;
import com.esdllm.agentmesh.model.dto.tool.ToolSchemaConfig;
import lombok.Data;

/**
 * 智能体主表：存储用户创建的 AI 智能体配置
 * @TableName agent
 */
@TableName(value ="agent",autoResultMap = true)
@Data
public class Agent {
    /**
     * 主键：自增 ID
     */
    @TableId
    private Long id;

    /**
     * 归属用户ID：智能体的创建者
     */
    private Long userId;

    /**
     * 智能体名称
     */
    private String name;

    /**
     * 智能体简介
     */
    private String description;

    /**
     * 头像 URL
     */
    private String avatarUrl;

    /**
     * 系统提示词：定义智能体核心行为和角色的 Prompt
     */
    private String systemPrompt;

    /**
     * 角色定义补充：额外的角色设定描述
     */
    private String roleDefinition;

    /**
     * 决策模型 ID：负责思考、规划、调用工具的模型 (高智力模型)
     */
    private Long decisionModelId;

    /**
     * 回复模型 ID：负责最终生成文本的模型 (可是低成本模型)
     */
    private Long responseModelId;
    
    /**
     * 模型选择策略：COST_FIRST, PERFORMANCE_FIRST, BALANCED, ADAPTIVE
     */
    private String modelSelectionStrategy = "ADAPTIVE";
    
    /**
     * 预算约束（每次调用的最大成本，元）
     */
    private Double budgetConstraint;
    
    /**
     * 是否启用工具:false 表示该智能体不使用任何工具
     */
    private Boolean isToolEnabled;
    
    /**
     * 工具配置覆写：智能体级别的特定工具参数配置
     */
    @TableField(typeHandler = PostgreSqlJsonbTypeHandler.class)
    private Object toolSchemaJson;
    
    /**
     * 配置版本号：用于版本管理或回滚
     */
    private String version = "1.0";
    
    /**
     * 智能体状态:1=发布,0=草稿/停用
     */
    private Integer status = 1;

    /**
     * 逻辑删除标记：0=正常, 1=已删除
     */
    private Integer isDelete;

    /**
     * 创建时间
     */
    private Date createdAt = new Date();

    /**
     * 最后更新时间
     */
    private Date updatedAt = new Date();

    @Override
    public boolean equals(Object that) {
        if (this == that) {
            return true;
        }
        if (that == null) {
            return false;
        }
        if (getClass() != that.getClass()) {
            return false;
        }
        Agent other = (Agent) that;
        return (this.getId() == null ? other.getId() == null : this.getId().equals(other.getId()))
            && (this.getUserId() == null ? other.getUserId() == null : this.getUserId().equals(other.getUserId()))
            && (this.getName() == null ? other.getName() == null : this.getName().equals(other.getName()))
            && (this.getDescription() == null ? other.getDescription() == null : this.getDescription().equals(other.getDescription()))
            && (this.getAvatarUrl() == null ? other.getAvatarUrl() == null : this.getAvatarUrl().equals(other.getAvatarUrl()))
            && (this.getSystemPrompt() == null ? other.getSystemPrompt() == null : this.getSystemPrompt().equals(other.getSystemPrompt()))
            && (this.getRoleDefinition() == null ? other.getRoleDefinition() == null : this.getRoleDefinition().equals(other.getRoleDefinition()))
            && (this.getDecisionModelId() == null ? other.getDecisionModelId() == null : this.getDecisionModelId().equals(other.getDecisionModelId()))
            && (this.getResponseModelId() == null ? other.getResponseModelId() == null : this.getResponseModelId().equals(other.getResponseModelId()))
            && (this.getIsToolEnabled() == null ? other.getIsToolEnabled() == null : this.getIsToolEnabled().equals(other.getIsToolEnabled()))
            && (this.getToolSchemaJson() == null ? other.getToolSchemaJson() == null : this.getToolSchemaJson().equals(other.getToolSchemaJson()))
            && (this.getVersion() == null ? other.getVersion() == null : this.getVersion().equals(other.getVersion()))
            && (this.getStatus() == null ? other.getStatus() == null : this.getStatus().equals(other.getStatus()))
            && (this.getIsDelete() == null ? other.getIsDelete() == null : this.getIsDelete().equals(other.getIsDelete()))
            && (this.getCreatedAt() == null ? other.getCreatedAt() == null : this.getCreatedAt().equals(other.getCreatedAt()))
            && (this.getUpdatedAt() == null ? other.getUpdatedAt() == null : this.getUpdatedAt().equals(other.getUpdatedAt()));
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((getId() == null) ? 0 : getId().hashCode());
        result = prime * result + ((getUserId() == null) ? 0 : getUserId().hashCode());
        result = prime * result + ((getName() == null) ? 0 : getName().hashCode());
        result = prime * result + ((getDescription() == null) ? 0 : getDescription().hashCode());
        result = prime * result + ((getAvatarUrl() == null) ? 0 : getAvatarUrl().hashCode());
        result = prime * result + ((getSystemPrompt() == null) ? 0 : getSystemPrompt().hashCode());
        result = prime * result + ((getRoleDefinition() == null) ? 0 : getRoleDefinition().hashCode());
        result = prime * result + ((getDecisionModelId() == null) ? 0 : getDecisionModelId().hashCode());
        result = prime * result + ((getResponseModelId() == null) ? 0 : getResponseModelId().hashCode());
        result = prime * result + ((getIsToolEnabled() == null) ? 0 : getIsToolEnabled().hashCode());
        result = prime * result + ((getToolSchemaJson() == null) ? 0 : getToolSchemaJson().hashCode());
        result = prime * result + ((getVersion() == null) ? 0 : getVersion().hashCode());
        result = prime * result + ((getStatus() == null) ? 0 : getStatus().hashCode());
        result = prime * result + ((getIsDelete() == null) ? 0 : getIsDelete().hashCode());
        result = prime * result + ((getCreatedAt() == null) ? 0 : getCreatedAt().hashCode());
        result = prime * result + ((getUpdatedAt() == null) ? 0 : getUpdatedAt().hashCode());
        return result;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(getClass().getSimpleName());
        sb.append(" [");
        sb.append("Hash = ").append(hashCode());
        sb.append(", id=").append(id);
        sb.append(", userId=").append(userId);
        sb.append(", name=").append(name);
        sb.append(", description=").append(description);
        sb.append(", avatarUrl=").append(avatarUrl);
        sb.append(", systemPrompt=").append(systemPrompt);
        sb.append(", roleDefinition=").append(roleDefinition);
        sb.append(", decisionModelId=").append(decisionModelId);
        sb.append(", responseModelId=").append(responseModelId);
        sb.append(", isToolEnabled=").append(isToolEnabled);
        sb.append(", toolSchemaJson=").append(toolSchemaJson);
        sb.append(", version=").append(version);
        sb.append(", status=").append(status);
        sb.append(", isDelete=").append(isDelete);
        sb.append(", createdAt=").append(createdAt);
        sb.append(", updatedAt=").append(updatedAt);
        sb.append("]");
        return sb.toString();
    }
}