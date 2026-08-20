package com.esdllm.agentmesh.model.dto.request;

import lombok.Data;

@Data
public class ModelProviderRequest {

    /**
     * 提供商名称：用户自定义的显示名称 (例："我的个人 OpenAI")
     */
    private String providerName;

    /**
     * 提供商代码：标准化标识 (例："openai", "azure")，用于前端图标匹配或逻辑路由
     */
    private String providerCode;

    /**
     * API 基础地址：用户配置的 API 入口 URL (例：<a href="https://api.openai.com/v1">...</a>)
     */
    private String baseUrl;

    /**
     * 加密 API Key：敏感字段，必须经应用层加密后存储，严禁明文
     * 注意：Ollama 等本地部署的模型不需要此字段
     */
    private String apiKeyEncrypted;

    /**
     * 加密 API Secret：可选，部分厂商 (如 Azure) 需要的密钥，需加密存储
     */
    private String apiSecretEncrypted;

    /**
     * 启用状态：1=启用, 0=禁用。禁用后该提供商下所有模型不可用
     */
    private Integer status;

}
