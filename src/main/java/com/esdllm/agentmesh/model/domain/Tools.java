package com.esdllm.agentmesh.model.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.TableField;
import com.esdllm.agentmesh.config.PostgreSqlJsonbTypeHandler;
import java.util.Date;
import lombok.Data;

/**
 * 统一工具表：存储所有可用工具定义 (系统内置 + 用户自定义 HTTP + 用户 MCP 暴露的工具)
 * @TableName tools
 */
@TableName(value ="tools", autoResultMap = true)
@Data
public class Tools {
    /**
     * 主键：自增 ID
     */
    @TableId
    private Long id;

    /**
     * 归属用户ID：NULL 表示系统内置工具 (所有用户可见); 非 NULL 表示用户私有工具
     */
    private Long ownerId;

    /**
     * 工具来源：枚举值 [SYSTEM, USER_HTTP, USER_MCP, USER_AGENT]
     */
    private String sourceType;

    /**
     * 工具代码名：LLM 调用时使用的唯一标识符 (例："get_weather")
     */
    private String toolCodeName;

    /**
     * 工具显示名：前端展示的友好名称
     */
    private String displayName;

    /**
     * 工具描述：功能说明，用于帮助 LLM 理解何时调用该工具
     */
    private String description;

    /**
     * 关联 MCP 服务ID：若来源为 USER_MCP，则指向 mcp_servers 表；否则为 NULL
     */
    private Long mcpServerId;

    /**
     * 输入参数 Schema：JSON Schema 格式，定义 LLM 需要传递的参数结构
     */
    @TableField(typeHandler = PostgreSqlJsonbTypeHandler.class)
    private Object inputSchema;

    /**
     * 输出参数 Schema：可选，定义预期返回值的结构
     */
    @TableField(typeHandler = PostgreSqlJsonbTypeHandler.class)
    private Object outputSchema;

    /**
     * 自定义执行 URL：USER_HTTP 模式必填；SYSTEM/MCP 模式通常为空或使用默认路由
     */
    private String customEndpointUrl;

    /**
     * 是否启用：false 表示暂时对智能体隐藏
     */
    private Boolean isEnabled;

    /**
     * 逻辑删除标记：0=正常，1=已删除
     */
    private Integer isDelete;

    /**
     * 当前使用的版本 ID：关联 tool_version 表
     */
    private Long currentVersionId;

    /**
     * 创建时间
     */
    private Date createdAt;

    /**
     * 最后更新时间
     */
    private Date updatedAt;

    /**
     * 工具健康状态：0=未知，1=健康，2=异常，3=禁用
     */
    private Integer healthStatus;

    /**
     * 最后健康检查时间
     */
    private Date lastHealthCheck;

    /**
     * 连续失败次数
     */
    private Integer consecutiveFailures;

    /**
     * 最后错误信息
     */
    private String lastErrorMessage;

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
        Tools other = (Tools) that;
        return (this.getId() == null ? other.getId() == null : this.getId().equals(other.getId()))
            && (this.getOwnerId() == null ? other.getOwnerId() == null : this.getOwnerId().equals(other.getOwnerId()))
            && (this.getSourceType() == null ? other.getSourceType() == null : this.getSourceType().equals(other.getSourceType()))
            && (this.getToolCodeName() == null ? other.getToolCodeName() == null : this.getToolCodeName().equals(other.getToolCodeName()))
            && (this.getDisplayName() == null ? other.getDisplayName() == null : this.getDisplayName().equals(other.getDisplayName()))
            && (this.getDescription() == null ? other.getDescription() == null : this.getDescription().equals(other.getDescription()))
            && (this.getMcpServerId() == null ? other.getMcpServerId() == null : this.getMcpServerId().equals(other.getMcpServerId()))
            && (this.getInputSchema() == null ? other.getInputSchema() == null : this.getInputSchema().equals(other.getInputSchema()))
            && (this.getOutputSchema() == null ? other.getOutputSchema() == null : this.getOutputSchema().equals(other.getOutputSchema()))
            && (this.getCustomEndpointUrl() == null ? other.getCustomEndpointUrl() == null : this.getCustomEndpointUrl().equals(other.getCustomEndpointUrl()))
            && (this.getIsEnabled() == null ? other.getIsEnabled() == null : this.getIsEnabled().equals(other.getIsEnabled()))
            && (this.getIsDelete() == null ? other.getIsDelete() == null : this.getIsDelete().equals(other.getIsDelete()))
            && (this.getCreatedAt() == null ? other.getCreatedAt() == null : this.getCreatedAt().equals(other.getCreatedAt()))
            && (this.getUpdatedAt() == null ? other.getUpdatedAt() == null : this.getUpdatedAt().equals(other.getUpdatedAt()));
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((getId() == null) ? 0 : getId().hashCode());
        result = prime * result + ((getOwnerId() == null) ? 0 : getOwnerId().hashCode());
        result = prime * result + ((getSourceType() == null) ? 0 : getSourceType().hashCode());
        result = prime * result + ((getToolCodeName() == null) ? 0 : getToolCodeName().hashCode());
        result = prime * result + ((getDisplayName() == null) ? 0 : getDisplayName().hashCode());
        result = prime * result + ((getDescription() == null) ? 0 : getDescription().hashCode());
        result = prime * result + ((getMcpServerId() == null) ? 0 : getMcpServerId().hashCode());
        result = prime * result + ((getInputSchema() == null) ? 0 : getInputSchema().hashCode());
        result = prime * result + ((getOutputSchema() == null) ? 0 : getOutputSchema().hashCode());
        result = prime * result + ((getCustomEndpointUrl() == null) ? 0 : getCustomEndpointUrl().hashCode());
        result = prime * result + ((getIsEnabled() == null) ? 0 : getIsEnabled().hashCode());
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
        sb.append(", ownerId=").append(ownerId);
        sb.append(", sourceType=").append(sourceType);
        sb.append(", toolCodeName=").append(toolCodeName);
        sb.append(", displayName=").append(displayName);
        sb.append(", description=").append(description);
        sb.append(", mcpServerId=").append(mcpServerId);
        sb.append(", inputSchema=").append(inputSchema);
        sb.append(", outputSchema=").append(outputSchema);
        sb.append(", customEndpointUrl=").append(customEndpointUrl);
        sb.append(", isEnabled=").append(isEnabled);
        sb.append(", isDelete=").append(isDelete);
        sb.append(", createdAt=").append(createdAt);
        sb.append(", updatedAt=").append(updatedAt);
        sb.append(", healthStatus=").append(healthStatus);
        sb.append(", lastHealthCheck=").append(lastHealthCheck);
        sb.append(", consecutiveFailures=").append(consecutiveFailures);
        sb.append(", lastErrorMessage=").append(lastErrorMessage);
        sb.append("]");
        return sb.toString();
    }
}