package com.esdllm.agentmesh.model.dto.response;

import lombok.Data;

@Data
public class ModelProviderResponse {
    /**
     * 主键id
     */
    private Long id;

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
     * 启用状态：1=启用, 0=禁用。禁用后该提供商下所有模型不可用
     */
    private Integer status;

}
