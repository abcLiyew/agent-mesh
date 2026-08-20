package com.esdllm.agentmesh.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 工具健康统计信息
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HealthStatistics {

    /**
     * 总工具数
     */
    private Integer totalTools;

    /**
     * 健康工具数
     */
    private Integer healthyTools;

    /**
     * 异常工具数
     */
    private Integer unhealthyTools;

    /**
     * 未知状态工具数
     */
    private Integer unknownTools;

    /**
     * 禁用工具数
     */
    private Integer disabledTools;

    /**
     * 健康率（百分比）
     */
    private Double healthRate;

    /**
     * 平均响应时间（毫秒）
     */
    private Double averageResponseTimeMs;
}
