package com.esdllm.agentmesh.model.dto.request;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class AiModelRequest {
    /**
     * 所属提供商ID：关联 model_provider 表
     */
    private Long providerId;

    /**
     * 模型代码名：调用 API 时使用的标准标识 (例："gpt-4-turbo")
     */
    private String modelName;

    /**
     * 模型显示名：前端展示的友好名称 (例："GPT-4 Turbo (高速版)")
     */
    private String modelDisplayName;

    /**
     * 模型类型：能力分类，枚举值 [CHAT, EMBEDDING, IMAGE]
     */
    private String modelType = "CHAT";

    /**
     * 上下文窗口：模型支持的最大 Token 数 (输入 + 输出)
     */
    private Integer contextWindow;

    /**
     * 最大输出长度：单次生成允许的最大 Token 数限制
     */
    private Integer maxTokens;

    /**
     * 输入成本：每 1k 输入 Token 的费用 (用户可自定义，用于统计)
     */
    private BigDecimal inputCostPer1k;

    /**
     * 输出成本：每 1k 输出 Token 的费用 (用户可自定义，用于统计)
     */
    private BigDecimal outputCostPer1k;

    /**
     * 货币单位：成本统计的币种 (例："CNY", "USD")
     */
    private String currencyType;

    /**
     * 是否活跃：false 表示暂时不在智能体配置列表中显示，但保留数据
     */
    private Boolean isActive;
}
