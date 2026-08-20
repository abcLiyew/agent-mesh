package com.esdllm.agentmesh.service;

import com.esdllm.agentmesh.model.dto.IntentRecognitionResult;

import java.util.List;

/**
 * 意图识别服务
 */
public interface IntentRecognitionService {
    
    /**
     * 识别用户意图
     * @param query 用户问题
     * @param userId 用户 ID
     * @return 意图识别结果
     */
    IntentRecognitionResult recognizeIntent(String query, Long userId);
    
    /**
     * 带智能体上下文的意图识别
     * @param query 用户问题
     * @param agentId 智能体 ID
     * @param userId 用户 ID
     * @return 意图识别结果
     */
    IntentRecognitionResult recognizeIntentWithContext(String query, Long agentId, Long userId);
    
    /**
     * 批量识别意图（用于统计分析）
     * @param queries 问题列表
     * @param userId 用户 ID
     * @return 意图识别结果列表
     */
    List<IntentRecognitionResult> batchRecognize(List<String> queries, Long userId);
    
    /**
     * 添加意图样本（用于优化识别准确率）
     * @param query 用户问题
     * @param intentType 意图类型
     * @param userId 用户 ID
     */
    void addTrainingSample(String query, String intentType, Long userId);
}
