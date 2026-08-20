package com.esdllm.agentmesh.model.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.util.Date;
import lombok.Data;

/**
 * 模型实例表：存储具体可用的模型列表 (如 gpt-4, qwen-turbo)，关联到具体的 Provider
 * @TableName ai_model
 */
@TableName(value ="ai_model")
@Data
public class AiModel {
    /**
     * 主键：自增 ID
     */
    @TableId
    private Long id;

    /**
     * 归属用户ID：冗余字段，加速“查询某用户所有可用模型”的场景，避免连表
     */
    private Long userId;

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
    @TableField("input_cost_per_1k")
    private BigDecimal inputCostPer1k;

    /**
     * 输出成本：每 1k 输出 Token 的费用 (用户可自定义，用于统计)
     */
    @TableField("output_cost_per_1k")
    private BigDecimal outputCostPer1k;

    /**
     * 货币单位：成本统计的币种 (例："CNY", "USD")
     */
    private String currencyType;

    /**
     * 是否活跃：false 表示暂时不在智能体配置列表中显示，但保留数据
     */
    private Boolean isActive;

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
        AiModel other = (AiModel) that;
        return (this.getId() == null ? other.getId() == null : this.getId().equals(other.getId()))
            && (this.getUserId() == null ? other.getUserId() == null : this.getUserId().equals(other.getUserId()))
            && (this.getProviderId() == null ? other.getProviderId() == null : this.getProviderId().equals(other.getProviderId()))
            && (this.getModelName() == null ? other.getModelName() == null : this.getModelName().equals(other.getModelName()))
            && (this.getModelDisplayName() == null ? other.getModelDisplayName() == null : this.getModelDisplayName().equals(other.getModelDisplayName()))
            && (this.getModelType() == null ? other.getModelType() == null : this.getModelType().equals(other.getModelType()))
            && (this.getContextWindow() == null ? other.getContextWindow() == null : this.getContextWindow().equals(other.getContextWindow()))
            && (this.getMaxTokens() == null ? other.getMaxTokens() == null : this.getMaxTokens().equals(other.getMaxTokens()))
            && (this.getInputCostPer1k() == null ? other.getInputCostPer1k() == null : this.getInputCostPer1k().equals(other.getInputCostPer1k()))
            && (this.getOutputCostPer1k() == null ? other.getOutputCostPer1k() == null : this.getOutputCostPer1k().equals(other.getOutputCostPer1k()))
            && (this.getCurrencyType() == null ? other.getCurrencyType() == null : this.getCurrencyType().equals(other.getCurrencyType()))
            && (this.getIsActive() == null ? other.getIsActive() == null : this.getIsActive().equals(other.getIsActive()))
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
        result = prime * result + ((getProviderId() == null) ? 0 : getProviderId().hashCode());
        result = prime * result + ((getModelName() == null) ? 0 : getModelName().hashCode());
        result = prime * result + ((getModelDisplayName() == null) ? 0 : getModelDisplayName().hashCode());
        result = prime * result + ((getModelType() == null) ? 0 : getModelType().hashCode());
        result = prime * result + ((getContextWindow() == null) ? 0 : getContextWindow().hashCode());
        result = prime * result + ((getMaxTokens() == null) ? 0 : getMaxTokens().hashCode());
        result = prime * result + ((getInputCostPer1k() == null) ? 0 : getInputCostPer1k().hashCode());
        result = prime * result + ((getOutputCostPer1k() == null) ? 0 : getOutputCostPer1k().hashCode());
        result = prime * result + ((getCurrencyType() == null) ? 0 : getCurrencyType().hashCode());
        result = prime * result + ((getIsActive() == null) ? 0 : getIsActive().hashCode());
        result = prime * result + ((getIsDelete() == null) ? 0 : getIsDelete().hashCode());
        result = prime * result + ((getCreatedAt() == null) ? 0 : getCreatedAt().hashCode());
        result = prime * result + ((getUpdatedAt() == null) ? 0 : getUpdatedAt().hashCode());
        return result;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() +
                " [" +
                "Hash = " + hashCode() +
                ", id=" + id +
                ", userId=" + userId +
                ", providerId=" + providerId +
                ", modelName=" + modelName +
                ", modelDisplayName=" + modelDisplayName +
                ", modelType=" + modelType +
                ", contextWindow=" + contextWindow +
                ", maxTokens=" + maxTokens +
                ", inputCostPer1k=" + inputCostPer1k +
                ", outputCostPer1k=" + outputCostPer1k +
                ", currencyType=" + currencyType +
                ", isActive=" + isActive +
                ", isDelete=" + isDelete +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                "]";
    }
}