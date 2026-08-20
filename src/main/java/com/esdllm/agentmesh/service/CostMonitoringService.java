package com.esdllm.agentmesh.service;

import com.esdllm.agentmesh.model.domain.UserCostThreshold;
import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.Map; /**
 * 成本监控服务接口
 */
public interface CostMonitoringService {
    
    /**
     * 记录模型调用成本
     * @param userId 用户 ID
     * @param agentId 智能体 ID
     * @param modelId 模型 ID
     * @param modelType 模型类型
     * @param inputTokens 输入 Token 数
     * @param outputTokens 输出 Token 数
     * @param cost 成本
     */
    void recordCost(Long userId, Long agentId, Long modelId, String modelType, 
                   int inputTokens, int outputTokens, BigDecimal cost);
    
    /**
     * 统计用户的总成本
     * @param userId 用户 ID
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 成本统计信息
     */
    CostStatistics getUserCostStats(Long userId, Date startDate, Date endDate);
    
    /**
     * 获取智能体的成本统计
     * @param agentId 智能体 ID
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 成本统计信息
     */
    CostStatistics getAgentCostStats(Long agentId, Date startDate, Date endDate);
    
    /**
     * 获取成本趋势（按天）
     * @param userId 用户 ID
     * @param days 天数
     * @return 每日成本列表
     */
    List<Map<String, Object>> getCostTrend(Long userId, int days);
    
    /**
     * 检查是否超过成本阈值
     * @param userId 用户 ID
     * @param threshold 阈值
     * @return 是否超过
     */
    boolean isOverThreshold(Long userId, BigDecimal threshold);
    
    /**
     * 检查成本并触发告警（使用用户配置）
     * @param userId 用户 ID
     * @param agentId 智能体 ID（可选）
     */
    void checkCostAndAlert(Long userId, Long agentId);
    
    /**
     * 获取用户的成本阈值配置
     * @param userId 用户 ID
     * @param agentId 智能体 ID（可选）
     * @return 配置信息
     */
    UserCostThreshold getCostThresholdConfig(Long userId, Long agentId);
    
    /**
     * 保存成本阈值配置
     * @param threshold 配置信息
     * @return 是否成功
     */
    boolean saveCostThreshold(UserCostThreshold threshold);
    
    /**
     * 执行降级策略
     * @param userId 用户 ID
     * @param agentId 智能体 ID
     * @param strategy 降级策略
     */
    void executeDowngradeStrategy(Long userId, Long agentId, String strategy);
}
