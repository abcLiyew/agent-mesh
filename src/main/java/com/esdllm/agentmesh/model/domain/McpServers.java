package com.esdllm.agentmesh.model.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.util.Date;
import lombok.Data;

/**
 * MCP 服务器配置表：存储用户配置的 Model Context Protocol 服务连接信息
 * @TableName mcp_servers
 */
@TableName(value ="mcp_servers")
@Data
public class McpServers {
    /**
     * 主键：自增 ID
     */
    @TableId
    private Long id;

    /**
     * 所有者用户ID：MCP 服务的归属用户
     */
    private Long ownerId;

    /**
     * 服务名称：用户自定义的友好名称 (例："本地文件读取服务")
     */
    private String serverName;

    /**
     * 传输协议：枚举值 [SSE, STDIO, STREAMABLE_HTTP]
     */
    private String transportType;

    /**
     * 接入 URL：SSE 或 HTTP 模式下的服务端地址 (STDIO 模式下为空)
     */
    private String endpointUrl;

    /**
     * 启动命令参数：JSON 数组，STDIO 模式下启动进程的命令和参数 (例：["npx", "-y", "..."])
     */
    private Object commandArgs;

    /**
     * 加密认证配置：存储 Header Token 或 Basic Auth 密码等，需加密
     */
    private String authConfigEncrypted;

    /**
     * 加密环境变量：MCP 服务运行所需的环境变量，需加密
     */
    private String envVarsEncrypted;

    /**
     * 服务状态：1=运行中, 0=停止
     */
    private Integer status;

    /**
     * 最后心跳时间：由后端定时任务更新，用于监控服务在线状态
     */
    private Date lastHeartbeat;

    /**
     * 逻辑删除标记：0=正常, 1=已删除
     */
    private Integer isDelete;

    /**
     * 创建时间
     */
    private Date createdAt;

    /**
     * 最后更新时间
     */
    private Date updatedAt;

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
        McpServers other = (McpServers) that;
        return (this.getId() == null ? other.getId() == null : this.getId().equals(other.getId()))
            && (this.getOwnerId() == null ? other.getOwnerId() == null : this.getOwnerId().equals(other.getOwnerId()))
            && (this.getServerName() == null ? other.getServerName() == null : this.getServerName().equals(other.getServerName()))
            && (this.getTransportType() == null ? other.getTransportType() == null : this.getTransportType().equals(other.getTransportType()))
            && (this.getEndpointUrl() == null ? other.getEndpointUrl() == null : this.getEndpointUrl().equals(other.getEndpointUrl()))
            && (this.getCommandArgs() == null ? other.getCommandArgs() == null : this.getCommandArgs().equals(other.getCommandArgs()))
            && (this.getAuthConfigEncrypted() == null ? other.getAuthConfigEncrypted() == null : this.getAuthConfigEncrypted().equals(other.getAuthConfigEncrypted()))
            && (this.getEnvVarsEncrypted() == null ? other.getEnvVarsEncrypted() == null : this.getEnvVarsEncrypted().equals(other.getEnvVarsEncrypted()))
            && (this.getStatus() == null ? other.getStatus() == null : this.getStatus().equals(other.getStatus()))
            && (this.getLastHeartbeat() == null ? other.getLastHeartbeat() == null : this.getLastHeartbeat().equals(other.getLastHeartbeat()))
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
        result = prime * result + ((getServerName() == null) ? 0 : getServerName().hashCode());
        result = prime * result + ((getTransportType() == null) ? 0 : getTransportType().hashCode());
        result = prime * result + ((getEndpointUrl() == null) ? 0 : getEndpointUrl().hashCode());
        result = prime * result + ((getCommandArgs() == null) ? 0 : getCommandArgs().hashCode());
        result = prime * result + ((getAuthConfigEncrypted() == null) ? 0 : getAuthConfigEncrypted().hashCode());
        result = prime * result + ((getEnvVarsEncrypted() == null) ? 0 : getEnvVarsEncrypted().hashCode());
        result = prime * result + ((getStatus() == null) ? 0 : getStatus().hashCode());
        result = prime * result + ((getLastHeartbeat() == null) ? 0 : getLastHeartbeat().hashCode());
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
        sb.append(", serverName=").append(serverName);
        sb.append(", transportType=").append(transportType);
        sb.append(", endpointUrl=").append(endpointUrl);
        sb.append(", commandArgs=").append(commandArgs);
        sb.append(", authConfigEncrypted=").append(authConfigEncrypted);
        sb.append(", envVarsEncrypted=").append(envVarsEncrypted);
        sb.append(", status=").append(status);
        sb.append(", lastHeartbeat=").append(lastHeartbeat);
        sb.append(", isDelete=").append(isDelete);
        sb.append(", createdAt=").append(createdAt);
        sb.append(", updatedAt=").append(updatedAt);
        sb.append("]");
        return sb.toString();
    }
}