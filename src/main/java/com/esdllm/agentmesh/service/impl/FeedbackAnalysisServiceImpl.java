package com.esdllm.agentmesh.service.impl;

import com.esdllm.agentmesh.model.dto.FeedbackStatistics;
import com.esdllm.agentmesh.service.ConversationLogService;
import com.esdllm.agentmesh.service.FeedbackAnalysisService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 反馈分析服务实现
 */
@Service
@Slf4j
public class FeedbackAnalysisServiceImpl implements FeedbackAnalysisService {

    private final ConversationLogService conversationLogService;

    public FeedbackAnalysisServiceImpl(ConversationLogService conversationLogService) {
        this.conversationLogService = conversationLogService;
    }

    @Override
    public FeedbackAnalysisReport analyzeAgentFeedback(Long agentId) {
        if (agentId == null) {
            throw new IllegalArgumentException("智能体 ID 不能为空");
        }

        FeedbackStatistics stats = conversationLogService.getAgentFeedbackStats(agentId);
        List<FeedbackStatistics.LowRatingFeedback> lowRatingFeedbacks = 
            conversationLogService.getLowRatingFeedbacks(agentId, 50);

        String mainIssue = identifyMainIssue(lowRatingFeedbacks);
        List<String> problemCategories = categorizeProblems(lowRatingFeedbacks);
        List<OptimizationSuggestion> suggestions = generateOptimizationSuggestions(agentId);

        return new FeedbackAnalysisReport(
            stats.getAverageRating(),
            stats.getTotalFeedbacks(),
            mainIssue,
            problemCategories,
            suggestions
        );
    }

    @Override
    public List<OptimizationSuggestion> generateOptimizationSuggestions(Long agentId) {
        List<FeedbackStatistics.LowRatingFeedback> lowRatingFeedbacks = 
            conversationLogService.getLowRatingFeedbacks(agentId, 50);

        List<OptimizationSuggestion> suggestions = new ArrayList<>();

        Map<String, Integer> issueCounts = new HashMap<>();
        
        for (FeedbackStatistics.LowRatingFeedback feedback : lowRatingFeedbacks) {
            if (feedback.getErrorMessage() != null && !feedback.getErrorMessage().isEmpty()) {
                issueCounts.merge("系统错误", 1, Integer::sum);
            }
            
            if (feedback.getIntentType() == null || "UNKNOWN".equals(feedback.getIntentType())) {
                issueCounts.merge("意图识别不准确", 1, Integer::sum);
            }
            
            if (feedback.getFeedback() != null && feedback.getFeedback().contains("慢")) {
                issueCounts.merge("响应速度慢", 1, Integer::sum);
            }
            
            if (feedback.getFeedback() != null && 
                (feedback.getFeedback().contains("不准确") || feedback.getFeedback().contains("错误"))) {
                issueCounts.merge("回答质量差", 1, Integer::sum);
            }
        }

        if (issueCounts.getOrDefault("系统错误", 0) > 0) {
            suggestions.add(new OptimizationSuggestion(
                "稳定性",
                "检查并修复工具调用和 API 集成中的错误",
                "检测到 " + issueCounts.get("系统错误") + " 条错误相关的反馈",
                1
            ));
        }

        if (issueCounts.getOrDefault("意图识别不准确", 0) > 0) {
            suggestions.add(new OptimizationSuggestion(
                "意图识别",
                "优化意图识别模型或调整意图匹配阈值",
                "检测到 " + issueCounts.get("意图识别不准确") + " 条意图识别相关的反馈",
                2
            ));
        }

        if (issueCounts.getOrDefault("响应速度慢", 0) > 0) {
            suggestions.add(new OptimizationSuggestion(
                "性能优化",
                "优化工具调用链路或增加缓存机制",
                "检测到 " + issueCounts.get("响应速度慢") + " 条响应速度相关的反馈",
                3
            ));
        }

        if (issueCounts.getOrDefault("回答质量差", 0) > 0) {
            suggestions.add(new OptimizationSuggestion(
                "回答质量",
                "优化提示词工程或升级使用的 AI 模型",
                "检测到 " + issueCounts.get("回答质量差") + " 条回答质量相关的反馈",
                4
            ));
        }

        return suggestions;
    }

    @Override
    public Map<String, Integer> getFeedbackKeywords(Long agentId, int limit) {
        List<FeedbackStatistics.LowRatingFeedback> lowRatingFeedbacks = 
            conversationLogService.getLowRatingFeedbacks(agentId, 100);

        Map<String, Integer> keywordFrequency = new HashMap<>();
        
        Set<String> stopwords = new HashSet<>(Arrays.asList(
            "的", "了", "是", "在", "就", "都", "而", "及", "与", "着",
            "一个", "没有", "我们", "你们", "他们", "它", "这", "那"
        ));

        Pattern pattern = Pattern.compile("[\\u4e00-\\u9fa5a-zA-Z]{2,}");

        for (FeedbackStatistics.LowRatingFeedback feedback : lowRatingFeedbacks) {
            String text = "";
            if (feedback.getFeedback() != null) {
                text += feedback.getFeedback();
            }
            if (feedback.getUserQuery() != null) {
                text += " " + feedback.getUserQuery();
            }

            Matcher matcher = pattern.matcher(text);
            while (matcher.find()) {
                String word = matcher.group();
                if (!stopwords.contains(word)) {
                    keywordFrequency.merge(word, 1, Integer::sum);
                }
            }
        }

        return keywordFrequency.entrySet().stream()
            .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
            .limit(limit)
            .collect(Collectors.toMap(
                Map.Entry::getKey,
                Map.Entry::getValue,
                (e1, e2) -> e1,
                LinkedHashMap::new
            ));
    }

    /**
     * 识别主要问题
     */
    private String identifyMainIssue(List<FeedbackStatistics.LowRatingFeedback> feedbacks) {
        if (feedbacks.isEmpty()) {
            return "暂无足够数据进行分析";
        }

        Map<String, Integer> issueCounts = new HashMap<>();
        
        for (FeedbackStatistics.LowRatingFeedback feedback : feedbacks) {
            if (feedback.getErrorMessage() != null && !feedback.getErrorMessage().isEmpty()) {
                issueCounts.merge("系统错误", 1, Integer::sum);
            } else if (feedback.getFeedback() != null) {
                if (feedback.getFeedback().contains("慢") || feedback.getFeedback().contains("卡")) {
                    issueCounts.merge("响应速度", 1, Integer::sum);
                } else if (feedback.getFeedback().contains("不准确") || feedback.getFeedback().contains("错误")) {
                    issueCounts.merge("准确性", 1, Integer::sum);
                } else if (feedback.getFeedback().contains("态度") || feedback.getFeedback().contains("语气")) {
                    issueCounts.merge("服务质量", 1, Integer::sum);
                }
            }
        }

        if (issueCounts.isEmpty()) {
            return "未明确具体问题";
        }

        String mainIssue = issueCounts.entrySet().stream()
            .max(Map.Entry.comparingByValue())
            .map(Map.Entry::getKey)
            .orElse("未知问题");

        return "主要问题：" + mainIssue + "（共 " + issueCounts.get(mainIssue) + " 次反馈）";
    }

    /**
     * 问题分类
     */
    private List<String> categorizeProblems(List<FeedbackStatistics.LowRatingFeedback> feedbacks) {
        Set<String> categories = new HashSet<>();

        for (FeedbackStatistics.LowRatingFeedback feedback : feedbacks) {
            if (feedback.getErrorMessage() != null && !feedback.getErrorMessage().isEmpty()) {
                categories.add("系统错误");
            }
            
            if (feedback.getFeedback() != null) {
                if (feedback.getFeedback().contains("慢")) {
                    categories.add("性能问题");
                }
                if (feedback.getFeedback().contains("不准确") || feedback.getFeedback().contains("错误")) {
                    categories.add("准确性问题");
                }
                if (feedback.getFeedback().contains("不好用") || feedback.getFeedback().contains("难用")) {
                    categories.add("易用性问题");
                }
            }
        }

        return new ArrayList<>(categories);
    }
}
