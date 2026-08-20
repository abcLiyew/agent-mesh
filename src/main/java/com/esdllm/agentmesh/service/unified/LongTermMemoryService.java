package com.esdllm.agentmesh.service.unified;

import com.esdllm.agentmesh.model.domain.AgentLongTermMemory;

import java.util.List;
import java.util.Map;

/**
 * 长期记忆服务接口
 * 实现"龙虾"的跨会话、跨周期记忆能力
 */
public interface LongTermMemoryService {
    
    /**
     * 存储记忆
     * @param memory 记忆对象
     * @return 记忆ID
     */
    Long storeMemory(AgentLongTermMemory memory);
    
    /**
     * 检索相关记忆
     * @param userId 用户ID
     * @param agentId 智能体ID（可选）
     * @param query 查询内容
     * @param memoryTypes 记忆类型列表（可选）
     * @param limit 返回数量限制
     * @return 相关记忆列表
     */
    List<AgentLongTermMemory> retrieveMemories(Long userId, Long agentId, 
                                               String query, List<String> memoryTypes, 
                                               int limit);
    
    /**
     * 基于向量相似度检索记忆
     * @param userId 用户ID
     * @param agentId 智能体ID（可选）
     * @param embedding 查询向量
     * @param similarityThreshold 相似度阈值
     * @param limit 返回数量限制
     * @return 相关记忆列表
     */
    List<AgentLongTermMemory> retrieveMemoriesBySimilarity(Long userId, Long agentId,
                                                           float[] embedding,
                                                           double similarityThreshold,
                                                           int limit);
    
    /**
     * 更新记忆
     * @param memoryId 记忆ID
     * @param content 新内容
     * @param metadata 新元数据
     */
    void updateMemory(Long memoryId, String content, Map<String, Object> metadata);
    
    /**
     * 删除记忆
     * @param memoryId 记忆ID
     */
    void deleteMemory(Long memoryId);
    
    /**
     * 记录记忆访问（用于统计和优化）
     * @param memoryId 记忆ID
     */
    void recordMemoryAccess(Long memoryId);
    
    /**
     * 从对话中提取并存储记忆
     * @param userId 用户ID
     * @param agentId 智能体ID
     * @param conversationContent 对话内容
     * @param decisionPath 决策路径
     * @return 提取的记忆ID列表
     */
    List<Long> extractAndStoreMemoriesFromConversation(Long userId, Long agentId,
                                                       String conversationContent,
                                                       Object decisionPath);
    
    /**
     * 获取用户画像（基于历史记忆聚合）
     * @param userId 用户ID
     * @return 用户画像（JSON格式）
     */
    Map<String, Object> getUserProfile(Long userId);
    
    /**
     * 清理过期记忆
     * @return 清理的记忆数量
     */
    int cleanupExpiredMemories();
}
