package com.esdllm.agentmesh.model.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.util.Date;
import lombok.Data;

/**
 * 模型提供商配置表：存储用户配置的 LLM 服务商信息 (如 OpenAI, Azure, Ollama 等)
 * @TableName model_provider
 */
@TableName(value ="model_provider")
@Data
public class ModelProvider {
    /**
     * 主键：自增 ID
     */
    @TableId
    private Long id;

    /**
     * 归属用户ID：关联到具体用户，实现多租户数据隔离
     */
    private Long userId;

    /**
     * 提供商名称：用户自定义的显示名称 (例："我的个人 OpenAI")
     */
    private String providerName;

    /**
     * 提供商代码：标准化标识 (例："openai", "azure")，用于前端图标匹配或逻辑路由
     */
    private String providerCode;

    /**
     * API 基础地址：用户配置的 API 入口 URL (例：https://api.openai.com/v1)
     */
    private String baseUrl;

    /**
     * 加密 API Key：敏感字段，必须经应用层加密后存储，严禁明文
     */
    private String apiKeyEncrypted;

    /**
     * 加密 API Secret：可选，部分厂商 (如 Azure) 需要的密钥，需加密存储
     */
    private String apiSecretEncrypted;

    /**
     * 启用状态:1=启用, 0=禁用。禁用后该提供商下所有模型不可用
     */
    private Integer status;
    
    /**
     * 逻辑删除标记:0=正常, 1=已删除
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
        ModelProvider other = (ModelProvider) that;
        return (this.getId() == null ? other.getId() == null : this.getId().equals(other.getId()))
            && (this.getUserId() == null ? other.getUserId() == null : this.getUserId().equals(other.getUserId()))
            && (this.getProviderName() == null ? other.getProviderName() == null : this.getProviderName().equals(other.getProviderName()))
            && (this.getProviderCode() == null ? other.getProviderCode() == null : this.getProviderCode().equals(other.getProviderCode()))
            && (this.getBaseUrl() == null ? other.getBaseUrl() == null : this.getBaseUrl().equals(other.getBaseUrl()))
            && (this.getApiKeyEncrypted() == null ? other.getApiKeyEncrypted() == null : this.getApiKeyEncrypted().equals(other.getApiKeyEncrypted()))
            && (this.getApiSecretEncrypted() == null ? other.getApiSecretEncrypted() == null : this.getApiSecretEncrypted().equals(other.getApiSecretEncrypted()))
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
        result = prime * result + ((getProviderName() == null) ? 0 : getProviderName().hashCode());
        result = prime * result + ((getProviderCode() == null) ? 0 : getProviderCode().hashCode());
        result = prime * result + ((getBaseUrl() == null) ? 0 : getBaseUrl().hashCode());
        result = prime * result + ((getApiKeyEncrypted() == null) ? 0 : getApiKeyEncrypted().hashCode());
        result = prime * result + ((getApiSecretEncrypted() == null) ? 0 : getApiSecretEncrypted().hashCode());
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
        sb.append(", providerName=").append(providerName);
        sb.append(", providerCode=").append(providerCode);
        sb.append(", baseUrl=").append(baseUrl);
        sb.append(", apiKeyEncrypted=").append(apiKeyEncrypted);
        sb.append(", apiSecretEncrypted=").append(apiSecretEncrypted);
        sb.append(", status=").append(status);
        sb.append(", isDelete=").append(isDelete);
        sb.append(", createdAt=").append(createdAt);
        sb.append(", updatedAt=").append(updatedAt);
        sb.append("]");
        return sb.toString();
    }
}