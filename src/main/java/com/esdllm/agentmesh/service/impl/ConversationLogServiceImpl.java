package com.esdllm.agentmesh.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.esdllm.agentmesh.common.ErrorCode;
import com.esdllm.agentmesh.exception.BusinessException;
import com.esdllm.agentmesh.model.domain.ConversationLog;
import com.esdllm.agentmesh.model.domain.AiModel;
import com.esdllm.agentmesh.model.dto.*;
import com.esdllm.agentmesh.repository.dao.ConversationLogDao;
import com.esdllm.agentmesh.repository.dao.AiModelDao;
import com.esdllm.agentmesh.repository.dao.ModelProviderDao;
import com.esdllm.agentmesh.service.ConversationLogService;
import com.esdllm.agentmesh.service.TokenCounter;
import com.esdllm.agentmesh.service.agent.support.AiModelSupport;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * 对话日志服务实现类
 */
@Service
@Slf4j
public class ConversationLogServiceImpl implements ConversationLogService {

    private final ConversationLogDao conversationLogDao;

    private final AiModelDao aiModelDao;
    
    private final ModelProviderDao modelProviderDao;
    
    private final AiModelSupport aiModelSupport;
    
    private final TokenCounter tokenCounter;
    
    // 会话标题缓存（sessionId -> title）
    private final Map<String, String> sessionTitleCache = new java.util.concurrent.ConcurrentHashMap<>();

    public ConversationLogServiceImpl(ConversationLogDao conversationLogDao, 
                                     AiModelDao aiModelDao,
                                     ModelProviderDao modelProviderDao,
                                     AiModelSupport aiModelSupport,
                                     TokenCounter tokenCounter) {
        this.conversationLogDao = conversationLogDao;
        this.aiModelDao = aiModelDao;
        this.modelProviderDao = modelProviderDao;
        this.aiModelSupport = aiModelSupport;
        this.tokenCounter = tokenCounter;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long logConversation(Long userId, Long agentId, String query, DecisionExecutionResult result) {
        return logConversationWithIntent(userId, agentId, generateSessionId(userId), query, null, result);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long logConversationWithIntent(
            Long userId,
            Long agentId,
            String sessionId,
            String query,
            IntentRecognitionResult intentResult,
            DecisionExecutionResult executionResult
    ) {
        try {
            ConversationLog conversationLog = new ConversationLog();
            conversationLog.setUserId(userId);
            conversationLog.setAgentId(agentId);
            conversationLog.setSessionId(sessionId);
            conversationLog.setUserQuery(query);
            conversationLog.setFinalResponse(executionResult.getFinalResponse());

            // 设置意图识别信息
            if (intentResult != null) {
                conversationLog.setIntentType(intentResult.getIntentType());
                conversationLog.setIntentConfidence(intentResult.getConfidence());
                conversationLog.setInvokedToolIds(intentResult.getMatchedToolIds());
                conversationLog.setSearchedKbIds(intentResult.getMatchedKbIds());
            }

            // 设置决策路径
            conversationLog.setDecisionPath(executionResult.getDecisionPath());

            // 设置模型信息
            conversationLog.setDecisionModelId(parseModelId(executionResult.getInternalModel()));
            conversationLog.setResponseModelId(parseModelId(executionResult.getResponseModel()));

            // 计算 Token 数和成本
            calculateTokensAndCost(conversationLog, executionResult);

            // 设置执行时间和状态
            conversationLog.setExecutionTimeMs(executionResult.getExecutionTimeMs());
            conversationLog.setStatus(executionResult.getSuccess() ? 1 : 0);
            conversationLog.setErrorMessage(executionResult.getErrorMessage());
            conversationLog.setCreatedAt(new Date());

            conversationLogDao.save(conversationLog);

            log.info("对话日志记录成功，logId: {}, userId: {}, agentId: {}",
                    conversationLog.getId(), userId, agentId);

            return conversationLog.getId();

        } catch (Exception e) {
            log.error("记录对话日志失败", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR,
                    "记录对话日志失败：" + e.getMessage());
        }
    }

    @Override
    public Page<ConversationLog> getUserConversationLogs(Long userId, int page, int pageSize) {
        if (userId == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户 ID 不能为空");
        }
        return conversationLogDao.getUserConversationLogs(userId, page, pageSize);
    }

    @Override
    public Page<ConversationSessionGroup> getUserConversationSessionGroups(Long userId, int page, int pageSize) {
        if (userId == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户 ID 不能为空");
        }
        
        // 1. 先查询所有对话记录
        Page<ConversationLog> allLogsPage = conversationLogDao.getUserConversationLogs(userId, 1, Integer.MAX_VALUE);
        List<ConversationLog> allLogs = allLogsPage.getRecords();
        
        // 2. 按 sessionId 分组
        Map<String, List<ConversationLog>> groupedBySession = allLogs.stream()
            .collect(Collectors.groupingBy(
                log -> log.getSessionId() != null ? log.getSessionId() : "unknown_" + log.getId(),
                LinkedHashMap::new,  // 保持插入顺序
                Collectors.toList()
            ));
        
        // 3. 转换为会话组列表
        List<ConversationSessionGroup> sessionGroups = groupedBySession.entrySet().stream()
            .map(entry -> {
                String sessionId = entry.getKey();
                List<ConversationLog> logs = entry.getValue();
                
                // 按时间排序
                logs.sort(Comparator.comparing(ConversationLog::getCreatedAt));
                
                ConversationLog firstLog = logs.get(0);
                ConversationLog lastLog = logs.get(logs.size() - 1);
                
                // 构建会话组
                String sessionTitle = generateSessionTitle(sessionId, firstLog.getUserQuery(), lastLog.getFinalResponse());
                
                return ConversationSessionGroup.builder()
                    .sessionId(sessionId)
                    .userId(firstLog.getUserId())
                    .agentId(firstLog.getAgentId())
                    .conversationCount(logs.size())
                    .firstConversationTime(firstLog.getCreatedAt())
                    .lastConversationTime(lastLog.getCreatedAt())
                    .firstQuery(truncateText(firstLog.getUserQuery(), 50))
                    .sessionTitle(sessionTitle)  // ✅ 设置AI生成的标题
                    .lastResponsePreview(truncateText(lastLog.getFinalResponse(), 100))
                    .status(lastLog.getStatus())
                    .userRating(lastLog.getUserRating())
                    .conversations(logs.stream()
                        .map(log -> ConversationSessionGroup.ConversationLogDetail.builder()
                            .id(log.getId())
                            .userQuery(log.getUserQuery())
                            .finalResponse(log.getFinalResponse())
                            .status(log.getStatus())
                            .executionTimeMs(log.getExecutionTimeMs())
                            .createdAt(log.getCreatedAt())
                            .userRating(log.getUserRating())
                            .build())
                        .collect(Collectors.toList()))
                    .build();
            })
            .sorted((a, b) -> b.getLastConversationTime().compareTo(a.getLastConversationTime()))  // 按最后更新时间倒序
            .collect(Collectors.toList());
        
        // 4. 分页处理
        int total = sessionGroups.size();
        int fromIndex = (page - 1) * pageSize;
        int toIndex = Math.min(fromIndex + pageSize, total);
        
        List<ConversationSessionGroup> pagedGroups = fromIndex < total 
            ? sessionGroups.subList(fromIndex, toIndex) 
            : Collections.emptyList();
        
        // 5. 构建分页结果
        Page<ConversationSessionGroup> resultPage = new Page<>(page, pageSize, total);
        resultPage.setRecords(pagedGroups);
        
        return resultPage;
    }
    
    /**
     * 截断文本，添加省略号
     */
    private String truncateText(String text, int maxLength) {
        if (text == null || text.isEmpty()) {
            return "";
        }
        if (text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, maxLength) + "...";
    }
    
    /**
     * 生成会话标题（优先从缓存获取，否则调用AI生成）
     */
    private String generateSessionTitle(String sessionId, String userQuery, String aiResponse) {
        // 1. 先从缓存中查找
        if (sessionTitleCache.containsKey(sessionId)) {
            return sessionTitleCache.get(sessionId);
        }
        
        // 2. 异步生成标题（不阻塞当前请求）
        CompletableFuture.runAsync(() -> {
            try {
                String title = generateTitleWithAI(userQuery, aiResponse);
                if (title != null && !title.isEmpty()) {
                    sessionTitleCache.put(sessionId, title);
                    log.info("✅ 会话标题生成成功: sessionId={}, title={}", sessionId, title);
                }
            } catch (Exception e) {
                log.error("生成会话标题失败: sessionId={}", sessionId, e);
            }
        });
        
        // 3. 首次返回时使用用户查询作为临时标题
        String tempTitle = truncateText(userQuery, 30);
        sessionTitleCache.putIfAbsent(sessionId, tempTitle);  // 防止重复生成
        return tempTitle;
    }
    
    /**
     * 使用AI生成简洁的会话标题
     */
    private String generateTitleWithAI(String userQuery, String aiResponse) {
        try {
            // 获取默认模型
            var model = aiModelDao.getFirstChatModel();
            if (model == null) {
                log.warn("没有可用的ChatModel，无法生成标题");
                return null;
            }
            
            // 获取模型提供商
            var provider = modelProviderDao.getById(model.getProviderId());
            if (provider == null) {
                log.warn("模型提供商不存在: providerId={}", model.getProviderId());
                return null;
            }
            
            // 创建ChatClient
            org.springframework.ai.chat.client.ChatClient chatClient = aiModelSupport.createChatClient(model, provider);
            
            // 构建提示词
            String systemPrompt = """
                你是一个专业的对话标题生成助手。
                
                任务：根据用户的提问和AI的回答，生成一个简洁、准确的会话标题。
                
                要求：
                - 标题长度控制在 5-15 个字以内
                - 准确概括对话的核心主题
                - 使用中文
                - 不要包含标点符号
                - 不要使用“关于”、“讨论”等冗余词汇
                - 直接输出标题，不要有其他内容
                
                示例：
                用户：游戏为啥让人上瘾？
                AI：游戏吸引人的原因包括沉浸感、成就感...
                标题：游戏吸引力分析
                
                用户：如何学习Java编程？
                AI：学习Java可以从基础语法开始...
                标题：Java编程学习指南
                """;
            
            String userPrompt = String.format("""
                用户提问：%s
                
                AI回答：%s
                
                请生成会话标题：
                """, 
                truncateText(userQuery, 200),
                truncateText(aiResponse != null ? aiResponse : "", 200)
            );
            
            // 调用AI
            String title = chatClient.prompt()
                .system(systemPrompt)
                .user(userPrompt)
                .call()
                .content();
            
            if (title != null && !title.trim().isEmpty()) {
                // 清理标题：去除标点、限制长度
                title = title.trim()
                    .replaceAll("[。！？，、；：\"'\\[\\]{}()]", "")
                    .replaceAll("\s+", "");
                
                if (title.length() > 15) {
                    title = title.substring(0, 15);
                }
                
                return title;
            }
            
            return null;
            
        } catch (Exception e) {
            log.error("调用AI生成标题失败", e);
            return null;
        }
    }

    @Override
    public Page<ConversationLog> getUserAgentConversationLogs(Long userId, Long agentId, int page, int pageSize) {
        if (userId == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户 ID 不能为空");
        }
        if (agentId == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "智能体 ID 不能为空");
        }
        return conversationLogDao.getUserAgentConversationLogs(userId, agentId, page, pageSize);
    }

    @Override
    public Page<ConversationLog> getAgentConversationLogs(Long agentId, int page, int pageSize) {
        if (agentId == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "智能体 ID 不能为空");
        }
        return conversationLogDao.getAgentConversationLogs(agentId, page, pageSize);
    }

    @Override
    public List<ConversationLog> getSessionDetail(String sessionId) {
        if (StrUtil.isBlank(sessionId)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "会话 ID 不能为空");
        }
        return conversationLogDao.getBySessionId(sessionId);
    }

    @Override
    public ConversationStatistics getStatistics(Long userId, Date startDate, Date endDate) {
        if (userId == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户 ID 不能为空");
        }

        ConversationStatistics stats = new ConversationStatistics();

        // 统计对话次数
        Long total = conversationLogDao.countConversations(userId, startDate, endDate);
        stats.setTotalConversations(total);

        // 统计成功率（需要获取所有日志）
        // 这里简化处理，实际应该优化 SQL
        stats.setSuccessfulConversations(total); // 临时值
        stats.setFailedConversations(0L);
        stats.setSuccessRate(100.0);

        // 统计总成本
        Double totalCost = conversationLogDao.getTotalCost(userId, startDate, endDate);
        stats.setTotalCost(BigDecimal.valueOf(totalCost));

        // 统计平均响应时间
        // 这里需要按 agentId 统计后取平均
        stats.setAverageResponseTime(0L);

        // 统计活跃会话数（最近 7 天）
        Long activeSessions = conversationLogDao.countActiveSessions(userId, 7);
        stats.setActiveSessions(activeSessions);

        return stats;
    }

    @Override
    public List<IntentStatistics> getAgentIntentStats(Long agentId, int limit) {
        if (agentId == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "智能体 ID 不能为空");
        }
        return conversationLogDao.getTopIntentTypes(agentId, limit);
    }

    @Override
    public List<ToolUsageStatistics> getAgentToolStats(Long agentId, int limit) {
        if (agentId == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "智能体 ID 不能为空");
        }
        return conversationLogDao.getTopUsedTools(agentId, limit);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateFeedback(Long logId, Integer rating, String feedback) {
        if (logId == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "日志 ID 不能为空");
        }

        if (rating != null && (rating < 1 || rating > 5)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "评分必须在 1-5 之间");
        }

        ConversationLog conversationLog = conversationLogDao.getById(logId);
        if (conversationLog == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "对话日志不存在");
        }

        conversationLog.setUserRating(rating);
        conversationLog.setUserFeedback(feedback);
        conversationLog.setUpdatedAt(new Date());

        conversationLogDao.updateById(conversationLog);

        log.info("更新对话日志反馈成功，logId: {}, rating: {}", logId, rating);
    }

    @Override
    public ConversationLog getFeedbackDetail(Long logId) {
        if (logId == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "日志 ID 不能为空");
        }
        
        ConversationLog conversationLog = conversationLogDao.getById(logId);
        if (conversationLog == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "对话日志不存在");
        }
        
        if (conversationLog.getUserRating() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "该对话暂无反馈");
        }
        
        return conversationLog;
    }

    @Override
    public FeedbackStatistics getUserFeedbackStats(Long userId) {
        if (userId == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户 ID 不能为空");
        }
        
        List<ConversationLog> feedbackLogs = conversationLogDao.getFeedbackByUserId(userId);
        
        return buildFeedbackStatistics(feedbackLogs);
    }

    @Override
    public FeedbackStatistics getAgentFeedbackStats(Long agentId) {
        if (agentId == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "智能体 ID 不能为空");
        }
        
        List<ConversationLog> feedbackLogs = conversationLogDao.getFeedbackByAgentId(agentId);
        
        return buildFeedbackStatistics(feedbackLogs);
    }

    @Override
    public List<FeedbackTrend> getFeedbackTrend(Long userId, int days) {
        if (userId == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户 ID 不能为空");
        }
        
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(days);
        
        List<ConversationLog> feedbackLogs = conversationLogDao.getFeedbackInDateRange(
            userId, java.sql.Date.valueOf(startDate), new java.sql.Date(System.currentTimeMillis()));
        
        Map<String, List<ConversationLog>> groupedByDate = feedbackLogs.stream()
            .collect(Collectors.groupingBy(log -> {
                LocalDate logDate = new Date(log.getCreatedAt().getTime()).toInstant()
                    .atZone(java.time.ZoneId.systemDefault())
                    .toLocalDate();
                return logDate.format(DateTimeFormatter.ISO_LOCAL_DATE);
            }));
        
        List<FeedbackTrend> trendList = new ArrayList<>();
        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
            String dateStr = date.format(DateTimeFormatter.ISO_LOCAL_DATE);
            List<ConversationLog> logsOfDay = groupedByDate.getOrDefault(dateStr, Collections.emptyList());
            
            FeedbackTrend trend = FeedbackTrend.builder()
                .date(dateStr)
                .feedbackCount(logsOfDay.size())
                .averageRating(calculateAverageRating(logsOfDay))
                .positiveCount(countPositiveFeedbacks(logsOfDay))
                .negativeCount(countNegativeFeedbacks(logsOfDay))
                .build();
            
            trendList.add(trend);
        }
        
        return trendList;
    }

    @Override
    public List<FeedbackStatistics.LowRatingFeedback> getLowRatingFeedbacks(Long agentId, int limit) {
        if (agentId == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "智能体 ID 不能为空");
        }
        
        List<ConversationLog> lowRatingLogs = conversationLogDao.getLowRatingFeedbacks(agentId, limit);
        
        return lowRatingLogs.stream()
            .map(log -> FeedbackStatistics.LowRatingFeedback.builder()
                .logId(log.getId())
                .userId(log.getUserId())
                .agentId(log.getAgentId())
                .userQuery(log.getUserQuery())
                .finalResponse(log.getFinalResponse())
                .rating(log.getUserRating())
                .feedback(log.getUserFeedback())
                .intentType(log.getIntentType())
                .errorMessage(log.getErrorMessage())
                .createdAt(log.getCreatedAt())
                .build())
            .collect(Collectors.toList());
    }

    /**
     * 构建反馈统计数据
     */
    private FeedbackStatistics buildFeedbackStatistics(List<ConversationLog> feedbackLogs) {
        if (feedbackLogs.isEmpty()) {
            return FeedbackStatistics.builder()
                .totalFeedbacks(0L)
                .averageRating(0.0)
                .positiveFeedbacks(0L)
                .neutralFeedbacks(0L)
                .negativeFeedbacks(0L)
                .positiveRate(0.0)
                .negativeRate(0.0)
                .distribution(FeedbackStatistics.RatingDistribution.builder()
                    .oneStar(0)
                    .twoStar(0)
                    .threeStar(0)
                    .fourStar(0)
                    .fiveStar(0)
                    .build())
                .build();
        }
        
        long total = feedbackLogs.size();
        
        double avgRating = feedbackLogs.stream()
            .mapToInt(ConversationLog::getUserRating)
            .average()
            .orElse(0.0);
        
        long positive = feedbackLogs.stream()
            .filter(log -> log.getUserRating() >= 4)
            .count();
        
        long neutral = feedbackLogs.stream()
            .filter(log -> log.getUserRating() == 3)
            .count();
        
        long negative = feedbackLogs.stream()
            .filter(log -> log.getUserRating() <= 2)
            .count();
        
        FeedbackStatistics.RatingDistribution distribution = 
            FeedbackStatistics.RatingDistribution.builder()
                .oneStar(countByRating(feedbackLogs, 1))
                .twoStar(countByRating(feedbackLogs, 2))
                .threeStar(countByRating(feedbackLogs, 3))
                .fourStar(countByRating(feedbackLogs, 4))
                .fiveStar(countByRating(feedbackLogs, 5))
                .build();
        
        return FeedbackStatistics.builder()
            .totalFeedbacks(total)
            .averageRating(Math.round(avgRating * 100.0) / 100.0)
            .positiveFeedbacks(positive)
            .neutralFeedbacks(neutral)
            .negativeFeedbacks(negative)
            .positiveRate(Math.round((positive * 100.0 / total) * 100.0) / 100.0)
            .negativeRate(Math.round((negative * 100.0 / total) * 100.0) / 100.0)
            .distribution(distribution)
            .build();
    }

    /**
     * 统计指定评分的数量
     */
    private int countByRating(List<ConversationLog> logs, int rating) {
        return (int) logs.stream()
            .filter(log -> log.getUserRating() == rating)
            .count();
    }

    /**
     * 计算平均评分
     */
    private Double calculateAverageRating(List<ConversationLog> logs) {
        if (logs.isEmpty()) {
            return 0.0;
        }
        return Math.round(logs.stream()
            .mapToInt(ConversationLog::getUserRating)
            .average()
            .orElse(0.0) * 100.0) / 100.0;
    }

    /**
     * 统计好评数量
     */
    private Integer countPositiveFeedbacks(List<ConversationLog> logs) {
        return (int) logs.stream()
            .filter(log -> log.getUserRating() >= 4)
            .count();
    }

    /**
     * 统计差评数量
     */
    private Integer countNegativeFeedbacks(List<ConversationLog> logs) {
        return (int) logs.stream()
            .filter(log -> log.getUserRating() <= 2)
            .count();
    }

    /**
     * 生成会话 ID
     */
    private String generateSessionId(Long userId) {
        return "session_" + userId + "_" + System.currentTimeMillis();
    }

    /**
     * 解析模型 ID（从模型名称字符串）
     */
    private Long parseModelId(String modelName) {
        if (modelName == null || modelName.isEmpty()) {
            return null;
        }
        
        // 根据模型名称查询实际的模型 ID
        AiModel model = aiModelDao.getByModelName(modelName);
        if (model != null) {
            return model.getId();
        }
        
        // 如果未找到，记录警告日志
        log.warn("未找到模型：{}", modelName);
        return null;
    }

    /**
     * 计算 Token 数和成本
     */
    private void calculateTokensAndCost(ConversationLog log, DecisionExecutionResult result) {
        // 从决策路径中提取 Token 信息并累加
        int totalInputTokens = 0;
        int totalOutputTokens = 0;
        
        if (result.getDecisionPath() != null) {
            // 遍历所有决策步骤，累加 Token 数
            for (DecisionStep step : result.getDecisionPath()) {
                TokenCount tokenCount = extractTokensFromStep(step);
                totalInputTokens += tokenCount.inputTokens;
                totalOutputTokens += tokenCount.outputTokens;
            }
        }
        
        // 设置 Token 数
        log.setTotalInputTokens(totalInputTokens);
        log.setTotalOutputTokens(totalOutputTokens);
        
        // 设置总成本（从结果中获取）
        log.setTotalCost(result.getTotalCost() != null ?
                BigDecimal.valueOf(result.getTotalCost()) : BigDecimal.ZERO);
    }
    
    /**
     * 从决策步骤中提取 Token 数
     */
    private TokenCount extractTokensFromStep(DecisionStep step) {
        int inputTokens = 0;
        int outputTokens = 0;
        
        // 从 inputData 中提取输入文本并计算 Token
        if (step.getInputData() != null) {
            String inputText = extractTextFromData(step.getInputData());
            if (inputText != null) {
                inputTokens = tokenCounter.countTokens(inputText);
            }
        }
        
        // 从 outputData 中提取输出文本并计算 Token
        if (step.getOutputData() != null) {
            String outputText = extractTextFromObject(step.getOutputData());
            if (outputText != null) {
                outputTokens = tokenCounter.countTokens(outputText);
            }
        }
        
        // 如果有子步骤，递归提取
        if (step.getSubSteps() != null && !step.getSubSteps().isEmpty()) {
            for (DecisionStep subStep : step.getSubSteps()) {
                TokenCount subTokenCount = extractTokensFromStep(subStep);
                inputTokens += subTokenCount.inputTokens;
                outputTokens += subTokenCount.outputTokens;
            }
        }
        
        return new TokenCount(inputTokens, outputTokens);
    }
    
    /**
     * 从 Map 数据中提取文本
     */
    private String extractTextFromData(Map<String, Object> data) {
        if (data == null || data.isEmpty()) {
            return null;
        }
        
        StringBuilder text = new StringBuilder();
        
        // 尝试从常见字段中提取文本
        Object prompt = data.get("prompt");
        if (prompt instanceof String) {
            text.append(prompt).append(" ");
        }
        
        Object messages = data.get("messages");
        if (messages instanceof java.util.List) {
            for (Object msg : (java.util.List<?>) messages) {
                if (msg instanceof Map) {
                    Object content = ((Map<?, ?>) msg).get("content");
                    if (content instanceof String) {
                        text.append(content).append(" ");
                    }
                } else if (msg instanceof String) {
                    text.append(msg).append(" ");
                }
            }
        }
        
        Object input = data.get("input");
        if (input instanceof String) {
            text.append(input).append(" ");
        }
        
        return text.toString().trim();
    }
    
    /**
     * 从对象中提取文本
     */
    private String extractTextFromObject(Object obj) {
        switch (obj) {
            case null -> {
                return null;
            }
            case String s -> {
                return s;
            }
            case Map map -> {
                StringBuilder text = new StringBuilder();

                Object content = map.get("content");
                if (content instanceof String) {
                    text.append(content);
                }

                Object response = map.get("response");
                if (response instanceof String) {
                    text.append(response);
                }

                Object output = map.get("output");
                if (output instanceof String) {
                    text.append(output);
                }

                return text.toString().trim();
            }
            default -> {
            }
        }

        // 其他类型转换为字符串
        return obj.toString();
    }
    
    /**
     * Token 计数内部类
     */
    private static class TokenCount {
        int inputTokens;
        int outputTokens;
        
        TokenCount(int inputTokens, int outputTokens) {
            this.inputTokens = inputTokens;
            this.outputTokens = outputTokens;
        }
    }
}
