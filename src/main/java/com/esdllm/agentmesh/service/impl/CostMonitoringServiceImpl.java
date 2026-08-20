package com.esdllm.agentmesh.service.impl;

import cn.hutool.core.date.DateUtil;
import com.esdllm.agentmesh.common.ErrorCode;
import com.esdllm.agentmesh.exception.BusinessException;
import com.esdllm.agentmesh.model.domain.Agent;
import com.esdllm.agentmesh.model.domain.AiModel;
import com.esdllm.agentmesh.model.domain.ModelUsageCost;
import com.esdllm.agentmesh.model.domain.UserCostThreshold;
import com.esdllm.agentmesh.model.dto.CostAlertNotification;
import com.esdllm.agentmesh.repository.dao.AgentDao;
import com.esdllm.agentmesh.repository.dao.AiModelDao;
import com.esdllm.agentmesh.repository.dao.ModelUsageCostDao;
import com.esdllm.agentmesh.repository.dao.UserCostThresholdDao;
import com.esdllm.agentmesh.repository.mapper.ModelUsageCostMapper;
import com.esdllm.agentmesh.service.CostMonitoringService;
import com.esdllm.agentmesh.service.CostStatistics;
import com.esdllm.agentmesh.service.NotificationService;
import com.esdllm.agentmesh.service.TokenCounter;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 成本监控服务实现类
 */
@Service
@Slf4j
public class CostMonitoringServiceImpl implements CostMonitoringService {

    @Resource
    private ModelUsageCostDao modelUsageCostDao;

    @Resource
    private ModelUsageCostMapper modelUsageCostMapper;

    @Resource
    private AiModelDao aiModelDao;

    @Resource
    private TokenCounter tokenCounter;

    @Resource
    private UserCostThresholdDao userCostThresholdDao;

    @Resource
    private NotificationService notificationService;

    @Resource
    private AgentDao agentDao;

    // 内存缓存今日成本（简化实现，生产环境应用 Redis）
    private final Map<Long, BigDecimal> todayCostCache = new ConcurrentHashMap<>();
    
    // 缓存用户配置
    private final Map<Long, UserCostThreshold> configCache = new ConcurrentHashMap<>();
    
    // 限流缓存（用于降级策略中的 REDUCE_CALLS）
    private final Map<Long, Boolean> rateLimitCache = new ConcurrentHashMap<>();

    @Override
    public void recordCost(Long userId, Long agentId, Long modelId, String modelType,
                           int inputTokens, int outputTokens, BigDecimal cost) {
        log.info("记录成本，userId: {}, agentId: {}, modelId: {}, inputTokens: {}, outputTokens: {}, cost: {}",
                userId, agentId, modelId, inputTokens, outputTokens, cost);

        // 如果 modelId 为 null，跳过记录（DECISION 类型不涉及具体模型）
        if (modelId == null) {
            log.debug("modelId 为 null，跳过成本记录（DECISION类型无需记录模型成本）");
            return;
        }

        try {
            ModelUsageCost costRecord = new ModelUsageCost();
            costRecord.setUserId(userId);
            costRecord.setAgentId(agentId);
            costRecord.setModelId(modelId);
            costRecord.setModelType(modelType);
            costRecord.setInputTokens(inputTokens);
            costRecord.setOutputTokens(outputTokens);
            costRecord.setTotalTokens(inputTokens + outputTokens);
            costRecord.setCost(cost);
            costRecord.setCurrencyType("CNY");
            costRecord.setStatus(1); // SUCCESS
            costRecord.setCreatedAt(new Date());

            modelUsageCostDao.save(costRecord);

            // 更新缓存
            updateTodayCostCache(userId, cost);
            
            // 记录成本后检查是否超限
            checkCostAndAlert(userId, agentId);

            log.info("成本记录成功，recordId: {}", costRecord.getId());

        } catch (Exception e) {
            log.error("记录成本失败", e);
            // 不抛出异常，避免影响主流程
        }
    }

    /**
     * 记录 LLM 调用成本（自动计算 Token 和成本）
     * @param userId 用户 ID
     * @param agentId 智能体 ID
     * @param modelId 模型 ID
     * @param inputText 输入文本
     * @param outputText 输出文本
     */
    public void recordLlmCallCost(Long userId, Long agentId, Long modelId, 
                                  String inputText, String outputText) {
        try {
            // 1. 计算 Token 数
            int inputTokens = tokenCounter.countTokens(inputText);
            int outputTokens = tokenCounter.countTokens(outputText);
            
            // 2. 获取模型成本配置
            AiModel model = aiModelDao.getById(modelId);
            if (model == null) {
                log.warn("模型不存在，无法记录成本：{}", modelId);
                return;
            }
            
            // 3. 计算成本
            BigDecimal cost = tokenCounter.calculateCost(
                inputTokens, 
                outputTokens,
                model.getInputCostPer1k(),
                model.getOutputCostPer1k()
            );
            
            // 4. 记录成本
            recordCost(userId, agentId, modelId, model.getModelType(),
                      inputTokens, outputTokens, cost);
            
            log.info("LLM 调用成本记录成功，inputTokens: {}, outputTokens: {}, cost: {}",
                inputTokens, outputTokens, cost);
                
        } catch (Exception e) {
            log.error("记录 LLM 调用成本失败", e);
            // 不抛出异常，避免影响主流程
        }
    }

    @Override
    public CostStatistics getUserCostStats(Long userId, Date startDate, Date endDate) {
        if (userId == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户 ID 不能为空");
        }

        try {
            // 从数据库查询统计数据
            Map<String, Object> stats = modelUsageCostMapper.getUserCostStats(userId, startDate, endDate);
            
            BigDecimal totalCost = stats.get("total_cost") != null ? 
                (BigDecimal) stats.get("total_cost") : BigDecimal.ZERO;
            long totalCalls = stats.get("total_calls") != null ?
                ((Number) stats.get("total_calls")).longValue() : 0L;
            long totalTokens = stats.get("total_tokens") != null ?
                ((Number) stats.get("total_tokens")).longValue() : 0L;

            // 查询按模型分组的成本
            List<Map<String, Object>> costByModelList = modelUsageCostMapper.getCostByModel(userId, startDate, endDate);
            Map<String, BigDecimal> costByModel = new HashMap<>();
            for (Map<String, Object> item : costByModelList) {
                String modelName = (String) item.get("model_name");
                BigDecimal cost = (BigDecimal) item.get("cost");
                if (modelName != null && cost != null) {
                    costByModel.put(modelName, cost);
                }
            }

            // 查询按智能体分组的调用次数
            List<Map<String, Object>> callsByAgentList = modelUsageCostMapper.getCallsByAgent(userId, startDate, endDate);
            Map<String, Integer> callsByAgent = new HashMap<>();
            for (Map<String, Object> item : callsByAgentList) {
                String agentName = (String) item.get("agent_name");
                Long calls = (Long) item.get("calls");
                if (agentName != null && calls != null) {
                    callsByAgent.put(agentName, calls.intValue());
                }
            }

            return new CostStatistics(totalCost, Math.toIntExact(totalCalls), Math.toIntExact(totalTokens), costByModel, callsByAgent);
            
        } catch (Exception e) {
            log.error("查询用户成本统计失败，userId: {}", userId, e);
            // 返回空统计信息
            return new CostStatistics(BigDecimal.ZERO, 0, 0, Map.of(), Map.of());
        }
    }

    @Override
    public CostStatistics getAgentCostStats(Long agentId, Date startDate, Date endDate) {
        if (agentId == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "智能体 ID 不能为空");
        }

        try {
            // 从数据库查询统计数据
            Map<String, Object> stats = modelUsageCostMapper.getAgentCostStats(agentId, startDate, endDate);
            
            BigDecimal totalCost = stats.get("total_cost") != null ? 
                (BigDecimal) stats.get("total_cost") : BigDecimal.ZERO;
            Long totalCalls = stats.get("total_calls") != null ? 
                ((Number) stats.get("total_calls")).longValue() : 0L;
            Long totalTokens = stats.get("total_tokens") != null ? 
                ((Number) stats.get("total_tokens")).longValue() : 0L;

            // 查询按模型分组的成本
            List<Map<String, Object>> costByModelList = modelUsageCostMapper.getCostByModel(null, startDate, endDate);
            Map<String, BigDecimal> costByModel = new HashMap<>();
            for (Map<String, Object> item : costByModelList) {
                String modelName = (String) item.get("model_name");
                BigDecimal cost = (BigDecimal) item.get("cost");
                if (modelName != null && cost != null) {
                    costByModel.put(modelName, cost);
                }
            }

            return new CostStatistics(totalCost, Math.toIntExact(totalCalls), Math.toIntExact(totalTokens), costByModel, Map.of());
            
        } catch (Exception e) {
            log.error("查询智能体成本统计失败，agentId: {}", agentId, e);
            return new CostStatistics(BigDecimal.ZERO, 0, 0, Map.of(), Map.of());
        }
    }

    @Override
    public List<Map<String, Object>> getCostTrend(Long userId, int days) {
        if (userId == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户 ID 不能为空");
        }

        try {
            Date startDate = DateUtil.offsetDay(new Date(), -days + 1);
            
            // 从数据库查询趋势数据
            List<Map<String, Object>> trendData = modelUsageCostMapper.getCostTrend(userId, startDate);
            
            if (trendData.isEmpty()) {
                log.debug("未查询到成本趋势数据，userId: {}", userId);
                return new ArrayList<>();
            }

            // 转换数据格式
            List<Map<String, Object>> trend = new ArrayList<>();
            for (Map<String, Object> item : trendData) {
                Map<String, Object> dataPoint = new HashMap<>();
                dataPoint.put("date", item.get("date"));
                dataPoint.put("cost", item.get("cost"));
                dataPoint.put("calls", item.get("calls"));
                trend.add(dataPoint);
            }

            return trend;
            
        } catch (Exception e) {
            log.error("查询成本趋势失败，userId: {}", userId, e);
            return new ArrayList<>();
        }
    }

    @Override
    public boolean isOverThreshold(Long userId, BigDecimal threshold) {
        if (userId == null || threshold == null) {
            return false;
        }

        BigDecimal todayCost= todayCostCache.getOrDefault(userId, BigDecimal.ZERO);
        boolean isOver= todayCost.compareTo(threshold) > 0;

        if (isOver) {
            log.warn("用户 {} 今日成本已超过阈值，当前：{}, 阈值：{}",
                    userId, todayCost, threshold);
        }

        return isOver;
    }
    
    @Override
    public void checkCostAndAlert(Long userId, Long agentId) {
        if (userId == null) {
            return;
        }
        
        try {
            // 获取用户配置
            UserCostThreshold config = getCostThresholdConfig(userId, agentId);
            if (config == null || !config.getAlertEnabled()) {
                return;
            }
            
            // 计算当前成本
            BigDecimal currentCost = getCurrentUserCost(userId);
            
            // 检查各种阈值
            checkThreshold(userId, agentId, currentCost, 
                          config.getDailyThreshold(), "DAILY_EXCEEDED", config);
            checkThreshold(userId, agentId, currentCost, 
                          config.getWeeklyThreshold(), "WEEKLY_EXCEEDED", config);
            checkThreshold(userId, agentId, currentCost, 
                          config.getMonthlyThreshold(), "MONTHLY_EXCEEDED", config);
            checkThreshold(userId, agentId, currentCost, 
                          config.getTotalThreshold(), "TOTAL_EXCEEDED", config);
            
        } catch (Exception e) {
            log.error("检查成本告警失败", e);
        }
    }
    
    /**
     * 检查单个阈值
     */
    private void checkThreshold(Long userId, Long agentId, BigDecimal currentCost, 
                               BigDecimal threshold, String alertType, 
                               UserCostThreshold config) {
        if (threshold == null || currentCost.compareTo(threshold) <= 0) {
            return;
        }
        
        // 计算超出金额和百分比
        BigDecimal exceededAmount = currentCost.subtract(threshold);
        Double exceededPercentage = exceededAmount.divide(threshold, 4, BigDecimal.ROUND_HALF_UP)
                                                  .multiply(new BigDecimal("100"))
                                                  .doubleValue();
        
        // 构建告警通知
        CostAlertNotification notification = CostAlertNotification.builder()
            .userId(userId)
            .agentId(agentId)
            .alertType(alertType)
            .currentCost(currentCost)
            .threshold(threshold)
            .exceededAmount(exceededAmount)
            .exceededPercentage(exceededPercentage)
            .downgradeTriggered(config.getAutoDowngradeEnabled())
            .downgradeStrategy(config.getDowngradeStrategy())
            .alertTime(new Date())
            .message(config.getNotificationMethod())
            .build();
        
        // 发送通知
        notificationService.sendCostAlert(notification);
        
        // 执行降级策略
        if (config.getAutoDowngradeEnabled() && config.getDowngradeStrategy() != null) {
            executeDowngradeStrategy(userId, agentId, config.getDowngradeStrategy());
        }
        
        // 更新配置中的告警记录
        updateAlertRecord(config);
        
        log.warn("触发成本告警：用户={}, 类型={}, 当前成本={}, 阈值={}", 
                userId, alertType, currentCost, threshold);
    }
    
    /**
     * 获取用户当前成本（今日）
     */
    private BigDecimal getCurrentUserCost(Long userId) {
        return todayCostCache.getOrDefault(userId, BigDecimal.ZERO);
    }
    
    @Override
    public UserCostThreshold getCostThresholdConfig(Long userId, Long agentId) {
        if (userId == null) {
            return null;
        }
        
        // 优先获取智能体级别配置
        if (agentId != null) {
            List<UserCostThreshold> list = userCostThresholdDao.list(
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<UserCostThreshold>()
                    .eq("user_id", userId)
                    .eq("agent_id", agentId)
            );
            if (!list.isEmpty()) {
                return list.getFirst();
            }
        }
        
        // 获取全局配置（agent_id 为 null）
        List<UserCostThreshold> list = userCostThresholdDao.list(
            new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<UserCostThreshold>()
                .eq("user_id", userId)
                .isNull("agent_id")
        );
        return list.isEmpty() ? null : list.getFirst();
    }
    
    @Override
    public boolean saveCostThreshold(UserCostThreshold threshold) {
        if (threshold == null || threshold.getUserId() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户 ID 不能为空");
        }
        
        // 验证配置
        validateThresholdConfig(threshold);
        
        // 设置更新时间
        threshold.setUpdatedAt(new Date());
        
        // 保存到数据库
        boolean success = userCostThresholdDao.saveOrUpdate(threshold);
        
        if (success) {
            // 更新缓存
            configCache.put(threshold.getUserId(), threshold);
            log.info("保存成本阈值配置成功，userId: {}, agentId: {}", 
                    threshold.getUserId(), threshold.getAgentId());
        }
        
        return success;
    }
    
    @Override
    public void executeDowngradeStrategy(Long userId, Long agentId, String strategy) {
        if (userId == null || strategy == null) {
            return;
        }
        
        log.info("执行降级策略：userId={}, agentId={}, strategy={}", userId, agentId, strategy);
        
        try {
            switch (strategy) {
                case "DOWNGRADE_MODEL":
                    // 降级到更便宜的模型
                    log.info("策略：降级到经济型模型");
                    // 实际实现需要修改智能体的模型配置
                    if (agentId != null) {
                        Agent agent = agentDao.getById(agentId);
                        if (agent != null) {
                            // 查找更便宜的模型
                            AiModel cheaperModel = findCheaperModel(agent.getDecisionModelId());
                            if (cheaperModel != null) {
                                agent.setDecisionModelId(cheaperModel.getId());
                                agentDao.updateById(agent);
                                log.info("已将智能体 {} 的决策模型降级为 {}", agentId, cheaperModel.getModelName());
                            }
                        }
                    }
                    break;
                    
                case "DISABLE_AGENT":
                    // 禁用智能体
                    log.info("策略：禁用智能体");
                    if (agentId != null) {
                        Agent agent = agentDao.getById(agentId);
                        if (agent != null && agent.getUserId().equals(userId)) {
                            agent.setStatus(0); // 设置为停用状态
                            agentDao.updateById(agent);
                            log.info("已禁用智能体 {}", agentId);
                        }
                    }
                    break;
                    
                case "REDUCE_CALLS":
                    // 限制调用频率
                    log.info("策略：限制调用频率");
                    // 实际实现需要添加限流逻辑
                    // 这里可以通过缓存或 Redis 实现简单的限流
                    if (agentId != null) {
                        // 设置该智能体的限流标记
                        rateLimitCache.put(agentId, true);
                        log.info("已对智能体 {} 启用限流", agentId);
                    }
                    break;
                    
                default:
                    log.warn("未知的降级策略：{}", strategy);
            }
        } catch (Exception e) {
            log.error("执行降级策略失败", e);
        }
    }

    /**
     * 查找比当前模型更便宜的替代模型
     */
    private AiModel findCheaperModel(Long currentModelId) {
        if (currentModelId == null) {
            return null;
        }
        
        AiModel currentModel = aiModelDao.getById(currentModelId);
        if (currentModel == null) {
            return null;
        }
        
        // 查找同类型但成本更低的模型
        List<AiModel> allModels = aiModelDao.list(new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<AiModel>()
            .eq("model_type", currentModel.getModelType())
            .ne("id", currentModelId)
            .eq("is_active", true)
            .orderByAsc("input_cost_per_1k"));
        
        if (!allModels.isEmpty()) {
            return allModels.getFirst(); // 返回最便宜的
        }
        
        return null;
    }

    /**
     * 验证阈值配置
     */
    private void validateThresholdConfig(UserCostThreshold config) {
        if (config.getDailyThreshold() != null && config.getDailyThreshold().compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "日阈值不能为负数");
        }
        if (config.getWeeklyThreshold() != null && config.getWeeklyThreshold().compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "周阈值不能为负数");
        }
        if (config.getMonthlyThreshold() != null && config.getMonthlyThreshold().compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "月阈值不能为负数");
        }
        if (config.getTotalThreshold() != null && config.getTotalThreshold().compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "总阈值不能为负数");
        }
        
        // 验证降级策略
        if (config.getAutoDowngradeEnabled() && config.getDowngradeStrategy() != null) {
            List<String> validStrategies = Arrays.asList(
                "DOWNGRADE_MODEL", "DISABLE_AGENT", "REDUCE_CALLS"
            );
            if (!validStrategies.contains(config.getDowngradeStrategy().toUpperCase())) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, 
                        "无效的降级策略：" + config.getDowngradeStrategy());
            }
        }
        
        // 验证通知方式
        if (config.getAlertEnabled() && config.getNotificationMethod() != null) {
            List<String> validMethods = Arrays.asList("EMAIL", "SMS", "WEBHOOK");
            if (!validMethods.contains(config.getNotificationMethod().toUpperCase())) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, 
                        "无效的通知方式：" + config.getNotificationMethod());
            }
        }
    }
    
    /**
     * 更新告警记录
     */
    private void updateAlertRecord(UserCostThreshold config) {
        if (config == null || config.getId() == null) {
            return;
        }
        
        // 增加今日告警次数
        config.setAlertCountToday(config.getAlertCountToday() + 1);
        config.setLastAlertTime(new Date());
        
        // 异步更新数据库（避免阻塞主流程）
        new Thread(() -> {
            try {
                userCostThresholdDao.saveOrUpdate(config);
            } catch (Exception e) {
                log.error("更新告警记录失败", e);
            }
        }).start();
    }
    
    /**
     * 更新今日成本缓存
     */
    private void updateTodayCostCache(Long userId, BigDecimal cost) {
        BigDecimal currentCost= todayCostCache.getOrDefault(userId, BigDecimal.ZERO);
        todayCostCache.put(userId, currentCost.add(cost));
    }
}
