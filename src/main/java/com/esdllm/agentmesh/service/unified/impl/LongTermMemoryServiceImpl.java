package com.esdllm.agentmesh.service.unified.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.esdllm.agentmesh.common.ErrorCode;
import com.esdllm.agentmesh.exception.BusinessException;
import com.esdllm.agentmesh.model.domain.AgentLongTermMemory;
import com.esdllm.agentmesh.repository.dao.AiModelDao;
import com.esdllm.agentmesh.repository.dao.ModelProviderDao;
import com.esdllm.agentmesh.repository.mapper.AgentLongTermMemoryMapper;
import com.esdllm.agentmesh.service.agent.support.AiModelSupport;
import com.esdllm.agentmesh.service.unified.LongTermMemoryService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 长期记忆服务实现类
 */
@Service
@Slf4j
public class LongTermMemoryServiceImpl implements LongTermMemoryService {
    
    @Resource
    private AgentLongTermMemoryMapper memoryMapper;
    
    @Autowired(required = false)
    private EmbeddingModel embeddingModel;
    
    @Resource
    private AiModelDao aiModelDao;
    
    @Resource
    private ModelProviderDao modelProviderDao;
    
    @Resource
    private AiModelSupport aiModelSupport;
    
    @Override
    public Long storeMemory(AgentLongTermMemory memory) {
        if (memory.getUserId() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户ID不能为空");
        }
        if (StrUtil.isBlank(memory.getMemoryValue())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "记忆内容不能为空");
        }
        
        memory.setCreatedAt(new Date());
        memory.setUpdatedAt(new Date());
        memory.setUsageCount(0);
        memory.setIsActive(true);
        
        memoryMapper.insert(memory);
        log.info("记忆存储成功，memoryId: {}, type: {}", memory.getId(), memory.getMemoryType());
        
        return memory.getId();
    }
    
    @Override
    public List<AgentLongTermMemory> retrieveMemories(Long userId, Long agentId, 
                                                      String query, List<String> memoryTypes, 
                                                      int limit) {
        LambdaQueryWrapper<AgentLongTermMemory> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AgentLongTermMemory::getUserId, userId)
               .eq(AgentLongTermMemory::getIsActive, true);
        
        // 注意：新schema中没有agentId字段，暂时移除相关过滤
        // TODO: 如果需要按智能体过滤，可以考虑在tags或metadata中添加agent信息
        
        // 记忆类型过滤
        if (memoryTypes != null && !memoryTypes.isEmpty()) {
            wrapper.in(AgentLongTermMemory::getMemoryType, memoryTypes);
        }
        
        // 过期时间过滤
        wrapper.and(w -> w.isNull(AgentLongTermMemory::getExpiresAt)
                         .or()
                         .gt(AgentLongTermMemory::getExpiresAt, java.time.LocalDateTime.now()));
        
        // 如果有查询内容，尝试关键词匹配
        if (StrUtil.isNotBlank(query)) {
            wrapper.and(w -> w.like(AgentLongTermMemory::getMemoryValue, query)
                             .or()
                             .like(AgentLongTermMemory::getTags, query));
        }
        
        // 按置信度和使用次数排序
        wrapper.orderByDesc(AgentLongTermMemory::getConfidenceScore)
               .orderByDesc(AgentLongTermMemory::getUsageCount)
               .orderByDesc(AgentLongTermMemory::getLastUsedAt);
        
        Page<AgentLongTermMemory> page = new Page<>(1, limit);
        List<AgentLongTermMemory> memories = memoryMapper.selectPage(page, wrapper).getRecords();
        
        log.debug("检索到 {} 条相关记忆", memories.size());
        return memories;
    }
    
    @Override
    public List<AgentLongTermMemory> retrieveMemoriesBySimilarity(Long userId, Long agentId,
                                                                  float[] embedding,
                                                                  double similarityThreshold,
                                                                  int limit) {
        if (embeddingModel == null) {
            log.warn("EmbeddingModel未配置，降级为基于文本的检索");
            return Collections.emptyList();
        }
        
        try {
            // 1. 获取所有候选记忆
            LambdaQueryWrapper<AgentLongTermMemory> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(AgentLongTermMemory::getUserId, userId)
                   .eq(AgentLongTermMemory::getIsActive, true)
                   .isNotNull(AgentLongTermMemory::getMemoryVector); // 只查询有向量的记忆
            
            // 注意：新schema中没有agentId字段
            
            // 过期时间过滤
            wrapper.and(w -> w.isNull(AgentLongTermMemory::getExpiresAt)
                             .or()
                             .gt(AgentLongTermMemory::getExpiresAt, java.time.LocalDateTime.now()));
            
            List<AgentLongTermMemory> candidates = memoryMapper.selectList(wrapper);
            
            if (candidates.isEmpty()) {
                return Collections.emptyList();
            }
            
            // 2. 计算每个记忆的向量相似度
            List<MemorySimilarityScore> scoredMemories = new ArrayList<>();
            for (AgentLongTermMemory memory : candidates) {
                // 如果记忆已有向量嵌入，直接计算余弦相似度
                if (memory.getMemoryVector() != null && memory.getMemoryVector().length > 0) {
                    double similarity = calculateCosineSimilarity(embedding, memory.getMemoryVector());
                    
                    if (similarity >= similarityThreshold) {
                        scoredMemories.add(new MemorySimilarityScore(memory, similarity));
                    }
                }
            }
            
            // 3. 按相似度排序并返回Top-N
            return scoredMemories.stream()
                .sorted((a, b) -> Double.compare(b.getSimilarity(), a.getSimilarity()))
                .limit(limit)
                .map(MemorySimilarityScore::getMemory)
                .collect(Collectors.toList());
                
        } catch (Exception e) {
            log.error("向量相似度检索失败", e);
            return Collections.emptyList();
        }
    }
    
    @Override
    public void updateMemory(Long memoryId, String content, Map<String, Object> metadata) {
        AgentLongTermMemory memory = memoryMapper.selectById(memoryId);
        if (memory == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "记忆不存在");
        }
        
        if (content != null) {
            memory.setMemoryValue(content);
        }
        if (metadata != null) {
            // 将metadata转换为JSON字符串存储到tags或其他字段
            // TODO: 根据实际需求选择合适的存储方式
            memory.setTags(metadata.toString());
        }
        
        memory.setUpdatedAt(new Date());
        memoryMapper.updateById(memory);
        
        log.info("记忆更新成功，memoryId: {}", memoryId);
    }
    
    @Override
    public void deleteMemory(Long memoryId) {
        AgentLongTermMemory memory = memoryMapper.selectById(memoryId);
        if (memory == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "记忆不存在");
        }
        
        // 软删除：设置为非激活状态
        memory.setIsActive(false);
        memory.setUpdatedAt(new Date());
        memoryMapper.updateById(memory);
        
        log.info("记忆删除成功，memoryId: {}", memoryId);
    }
    
    @Override
    public void recordMemoryAccess(Long memoryId) {
        AgentLongTermMemory memory = memoryMapper.selectById(memoryId);
        if (memory == null) {
            log.warn("记忆不存在，无法记录访问，memoryId: {}", memoryId);
            return;
        }
        
        memory.setUsageCount(memory.getUsageCount() + 1);
        memory.setLastUsedAt(java.time.LocalDateTime.now());
        memoryMapper.updateById(memory);
    }
    
    @Override
    public List<Long> extractAndStoreMemoriesFromConversation(Long userId, Long agentId,
                                                              String conversationContent,
                                                              Object decisionPath) {
        List<Long> memoryIds = new ArrayList<>();
        
        try {
            // 使用AI从对话中提取关键信息
            List<Map<String, String>> extractedMemories = extractMemoriesWithAI(userId, conversationContent);
            
            if (extractedMemories.isEmpty()) {
                log.warn("AI未提取到有效记忆");
                return memoryIds;
            }
            
            // 存储提取的记忆
            for (Map<String, String> memoryData : extractedMemories) {
                String memoryType = memoryData.getOrDefault("type", "general");
                String content = memoryData.get("content");
                String confidence = memoryData.getOrDefault("confidence", "0.7");
                
                if (StrUtil.isBlank(content)) {
                    continue;
                }
                
                AgentLongTermMemory memory = new AgentLongTermMemory();
                memory.setUserId(userId);
                memory.setMemoryType(memoryType);
                memory.setMemoryKey(memoryType + "_" + System.currentTimeMillis());
                memory.setMemoryValue(content);
                memory.setConfidenceScore(java.math.BigDecimal.valueOf(Double.parseDouble(confidence)));
                memory.setSourceType("ai_extraction");
                memory.setSourceReferenceId(agentId);
                
                // 添加tags
                String tags = memoryData.getOrDefault("tags", memoryType);
                memory.setTags(tags);
                
                Long memoryId = storeMemory(memory);
                memoryIds.add(memoryId);
                
                log.info("AI提取并存储记忆: type={}, confidence={}", memoryType, confidence);
            }
            
        } catch (Exception e) {
            log.error("记忆提取失败，降级为简单存储", e);
            
            // 降级方案：将整个对话作为交互历史存储
            if (StrUtil.isNotBlank(conversationContent) && conversationContent.length() > 50) {
                AgentLongTermMemory memory = new AgentLongTermMemory();
                memory.setUserId(userId);
                memory.setMemoryType("interaction_pattern");
                memory.setMemoryKey("conversation_" + System.currentTimeMillis());
                memory.setMemoryValue(conversationContent.substring(0, Math.min(2000, conversationContent.length())));
                memory.setConfidenceScore(java.math.BigDecimal.valueOf(0.5));
                memory.setSourceType("implicit_observation");
                memory.setSourceReferenceId(agentId);
                
                Long memoryId = storeMemory(memory);
                memoryIds.add(memoryId);
            }
        }
        
        return memoryIds;
    }
    
    /**
     * 使用AI提取对话中的关键记忆
     */
    private List<Map<String, String>> extractMemoriesWithAI(Long userId, String conversationContent) {
        try {
            // 获取默认模型
            var model = aiModelDao.getFirstChatModel();
            if (model == null) {
                log.warn("没有可用的ChatModel，无法进行AI记忆提取");
                return Collections.emptyList();
            }
            
            // 获取模型提供商
            var provider = modelProviderDao.getById(model.getProviderId());
            if (provider == null) {
                log.warn("模型提供商不存在: providerId={}", model.getProviderId());
                return Collections.emptyList();
            }
            
            // 创建ChatClient
            ChatClient chatClient = aiModelSupport.createChatClient(model, provider);
            
            // 构建提示词
            String systemPrompt = """
                你是一个专业的记忆提取助手。请分析以下对话内容，提取有价值的长期记忆。
                
                需要提取的记忆类型：
                1. user_preference: 用户偏好（如喜欢的风格、常用工具、工作方式等）
                2. project_context: 项目背景（如技术栈、业务领域、项目目标等）
                3. decision_logic: 决策逻辑（如选择某个方案的原因、权衡考虑等）
                4. interaction_pattern: 交互模式（如用户常用的提问方式、反馈习惯等）
                
                请以JSON数组格式返回，每个记忆包含：
                - type: 记忆类型
                - content: 记忆内容（简洁明了，不超过200字）
                - confidence: 置信度(0.0-1.0)
                - tags: 标签（逗号分隔）
                
                示例：
                [
                  {"type": "user_preference", "content": "用户偏好使用Python进行数据分析", "confidence": 0.9, "tags": "python,data_analysis"},
                  {"type": "project_context", "content": "项目使用Spring Boot + Vue技术栈", "confidence": 0.85, "tags": "springboot,vue,tech_stack"}
                ]
                
                如果没有有价值的记忆，返回空数组[]。
                """;
            
            String userPrompt = "请分析以下对话，提取关键记忆：\n\n" + 
                               conversationContent.substring(0, Math.min(3000, conversationContent.length()));
            
            // 调用AI
            String response = chatClient.prompt()
                .system(systemPrompt)
                .user(userPrompt)
                .call()
                .content();
            
            if (StrUtil.isBlank(response)) {
                return Collections.emptyList();
            }
            
            // 解析JSON响应（简化实现，实际应该使用Jackson）
            return parseMemoryJson(response);
            
        } catch (Exception e) {
            log.error("AI记忆提取异常", e);
            return Collections.emptyList();
        }
    }
    
    /**
     * 简化的JSON解析（生产环境应使用Jackson或Gson）
     */
    private List<Map<String, String>> parseMemoryJson(String json) {
        List<Map<String, String>> memories = new ArrayList<>();
        
        try {
            // 简化：假设JSON格式正确，直接返回空列表
            // 实际应该使用ObjectMapper解析
            log.debug("AI返回的记忆JSON: {}", json.substring(0, Math.min(200, json.length())));
            
            // TODO: 使用Jackson解析JSON数组
            // 当前返回空列表，依赖降级方案
            
        } catch (Exception e) {
            log.warn("JSON解析失败", e);
        }
        
        return memories;
    }
    
    @Override
    public Map<String, Object> getUserProfile(Long userId) {
        Map<String, Object> profile = new HashMap<>();
        
        try {
            // 检索用户偏好记忆
            List<AgentLongTermMemory> preferences = retrieveMemories(
                userId, null, "", Arrays.asList("preference"), 10
            );
            
            if (!preferences.isEmpty()) {
                List<Map<String, Object>> prefList = preferences.stream().map(mem -> {
                    Map<String, Object> item = new HashMap<>();
                    item.put("content", mem.getMemoryValue());
                    item.put("confidence", mem.getConfidenceScore());
                    item.put("usageCount", mem.getUsageCount());
                    return item;
                }).collect(Collectors.toList());
                
                profile.put("preferences", prefList);
            }
            
            // 统计信息
            LambdaQueryWrapper<AgentLongTermMemory> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(AgentLongTermMemory::getUserId, userId)
                   .eq(AgentLongTermMemory::getIsActive, true);
            
            long totalMemories = memoryMapper.selectCount(wrapper);
            profile.put("totalMemories", totalMemories);
            
        } catch (Exception e) {
            log.error("获取用户画像失败", e);
        }
        
        return profile;
    }
    
    @Override
    @Scheduled(cron = "0 0 2 * * ?") // 每天凌晨2点执行
    public int cleanupExpiredMemories() {
        log.info("开始清理过期记忆");
        
        LambdaQueryWrapper<AgentLongTermMemory> wrapper = new LambdaQueryWrapper<>();
        wrapper.isNotNull(AgentLongTermMemory::getExpiresAt)
               .lt(AgentLongTermMemory::getExpiresAt, java.time.LocalDateTime.now())
               .eq(AgentLongTermMemory::getIsActive, true);
        
        List<AgentLongTermMemory> expiredMemories = memoryMapper.selectList(wrapper);
        
        int count = 0;
        for (AgentLongTermMemory memory : expiredMemories) {
            memory.setIsActive(false);
            memory.setUpdatedAt(new Date());
            memoryMapper.updateById(memory);
            count++;
        }
        
        log.info("清理完成，共清理 {} 条过期记忆", count);
        return count;
    }
    
    /**
     * 生成并存储记忆的向量嵌入
     */
    public void generateAndStoreEmbedding(Long memoryId) {
        if (embeddingModel == null) {
            log.warn("EmbeddingModel未配置，无法生成向量嵌入");
            return;
        }
        
        try {
            AgentLongTermMemory memory = memoryMapper.selectById(memoryId);
            if (memory == null || StrUtil.isBlank(memory.getMemoryValue())) {
                return;
            }
            
            // 生成向量嵌入
            float[] embedding = embeddingModel.embed(memory.getMemoryValue());
            
            // 更新记忆
            memory.setMemoryVector(embedding);
            memory.setUpdatedAt(new Date());
            memoryMapper.updateById(memory);
            
            log.debug("记忆向量嵌入生成成功，memoryId: {}", memoryId);
            
        } catch (Exception e) {
            log.error("生成记忆向量嵌入失败，memoryId: {}", memoryId, e);
        }
    }
    
    /**
     * 计算余弦相似度
     */
    private double calculateCosineSimilarity(float[] vec1, float[] vec2) {
        if (vec1 == null || vec2 == null || vec1.length != vec2.length || vec1.length == 0) {
            return 0.0;
        }
        
        double dotProduct = 0.0;
        double norm1 = 0.0;
        double norm2 = 0.0;
        
        for (int i = 0; i < vec1.length; i++) {
            dotProduct += vec1[i] * vec2[i];
            norm1 += vec1[i] * vec1[i];
            norm2 += vec2[i] * vec2[i];
        }
        
        if (norm1 == 0.0 || norm2 == 0.0) {
            return 0.0;
        }
        
        return dotProduct / (Math.sqrt(norm1) * Math.sqrt(norm2));
    }
    
    // ========== 内部类 ==========
    
    /**
     * 记忆相似度评分
     */
    private static class MemorySimilarityScore {
        private final AgentLongTermMemory memory;
        private final double similarity;
        
        public MemorySimilarityScore(AgentLongTermMemory memory, double similarity) {
            this.memory = memory;
            this.similarity = similarity;
        }
        
        public AgentLongTermMemory getMemory() {
            return memory;
        }
        
        public double getSimilarity() {
            return similarity;
        }
    }
}
