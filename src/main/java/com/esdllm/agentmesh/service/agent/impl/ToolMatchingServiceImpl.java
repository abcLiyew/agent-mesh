package com.esdllm.agentmesh.service.agent.impl;

import cn.hutool.core.util.StrUtil;
import com.esdllm.agentmesh.common.ErrorCode;
import com.esdllm.agentmesh.config.RagProperties;
import com.esdllm.agentmesh.exception.BusinessException;
import com.esdllm.agentmesh.model.domain.KnowledgeBase;
import com.esdllm.agentmesh.model.domain.Tools;
import com.esdllm.agentmesh.repository.dao.ToolsDao;
import com.esdllm.agentmesh.service.KnowledgeBaseService;
import com.esdllm.agentmesh.service.ToolMatchingService;
import com.esdllm.agentmesh.model.dto.VectorSearchResult;
import com.esdllm.agentmesh.service.VectorSearchService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 工具匹配服务实现类
 */
@Service
@Slf4j
public class ToolMatchingServiceImpl implements ToolMatchingService {

    @Resource
  private ToolsDao toolsDao;

    @Resource
    private VectorSearchService vectorSearchService;

    @Resource
    private KnowledgeBaseService knowledgeBaseService;

    @Resource
    private RagProperties ragProperties;

    private final Map<String, List<Tools>> toolCache = new ConcurrentHashMap<>();

    @Override
    public List<Tools> matchToolsByIntent(String intentType, String query, Long userId) {
        if (StrUtil.isBlank(intentType)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "意图类型不能为空");
        }

      log.info("开始匹配工具，intentType: {}, query: {}", intentType, query);

       List<Tools> availableTools= getAvailableTools(userId);
        if (availableTools.isEmpty()) {
          log.info("用户暂无可用工具");
            return new ArrayList<>();
        }

       List<Tools> filteredTools= filterByIntent(availableTools, intentType);

        if (StrUtil.isNotBlank(query)) {
            if (Boolean.TRUE.equals(ragProperties.getEnableToolRecommendation())) {
                List<Tools> ragRecommendedTools = recommendToolsByRAG(query, userId, availableTools);
                
                if (!ragRecommendedTools.isEmpty()) {
                   filteredTools= mergeAndRankTools(filteredTools, ragRecommendedTools, query);
                } else {
                   filteredTools= rankByRelevance(filteredTools, query);
                }
            } else {
               filteredTools= rankByRelevance(filteredTools, query);
            }
        }

      log.info("匹配到 {} 个工具", filteredTools.size());
        return filteredTools;
    }

    @Override
    public List<Tools> searchTools(String keyword, Long userId) {
        if (StrUtil.isBlank(keyword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "关键词不能为空");
        }

       List<Tools> availableTools= getAvailableTools(userId);
        
        return availableTools.stream()
                            .filter(tool -> {
                              String toolName= tool.getToolCodeName().toLowerCase();
                              String displayName = tool.getDisplayName().toLowerCase();
                              String description = tool.getDescription() != null ? 
                                                  tool.getDescription().toLowerCase() : "";
                               
                               return toolName.contains(keyword.toLowerCase()) ||
                                      displayName.contains(keyword.toLowerCase()) ||
                                      description.contains(keyword.toLowerCase());
                            })
                            .collect(Collectors.toList());
    }

    @Override
    public List<Tools> getAvailableTools(Long userId) {
        if (userId == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户 ID 不能为空");
        }

        // 获取系统工具 + 用户自定义工具
       List<Tools> systemTools= toolsDao.getSystemTools();
       List<Tools> userTools= toolsDao.getUserTools(userId);
        
       List<Tools> allTools = new ArrayList<>();
       allTools.addAll(systemTools);
       allTools.addAll(userTools);
        
        // 只返回启用的工具
        return allTools.stream()
                      .filter(Tools::getIsEnabled)
                      .filter(tool -> tool.getIsDelete() == 0)
                      .toList();
    }

    /**
     * 根据意图类型过滤工具
     */
  private List<Tools> filterByIntent(List<Tools> tools, String intentType) {
        return switch (intentType.toUpperCase()) {
            case "PRODUCT_QUERY" -> 
               tools.stream()
                    .filter(tool -> isProductRelated(tool))
                    .toList();
                    
            case "ORDER_QUERY" -> 
               tools.stream()
                    .filter(tool -> isOrderRelated(tool))
                    .toList();
                    
            case "KNOWLEDGE_QA" -> 
               tools.stream()
                    .filter(tool -> "SYSTEM".equals(tool.getSourceType()))
                    .toList();
                    
            default -> tools;
        };
    }

    /**
     * 根据相关性排序
     */
  private List<Tools> rankByRelevance(List<Tools> tools, String query) {
        return tools.stream()
                   .sorted((t1, t2) -> {
                      int score1= calculateRelevanceScore(t1, query);
                      int score2= calculateRelevanceScore(t2, query);
                      return Integer.compare(score2, score1);
                   })
                   .collect(Collectors.toList());
    }

    /**
     * 计算相关性得分
     */
  private int calculateRelevanceScore(Tools tool, String query) {
       int score= 0;
       
      String queryLower= query.toLowerCase();
      String nameLower= tool.getToolCodeName().toLowerCase();
      String displayLower = tool.getDisplayName().toLowerCase();
      String descLower = tool.getDescription() != null ? 
                        tool.getDescription().toLowerCase() : "";

        // 完全匹配
        if (nameLower.equals(queryLower)) {
           score+= 100;
        }
        
        // 包含匹配
        if (nameLower.contains(queryLower)) {
           score+= 50;
        }
        if (displayLower.contains(queryLower)) {
           score+= 30;
        }
        if (descLower.contains(queryLower)) {
           score+= 20;
        }

        return score;
    }

    /**
     * 判断是否为产品相关工具
     */
  private boolean isProductRelated(Tools tool) {
      String desc= tool.getDescription() != null ? tool.getDescription() : "";
        return desc.contains("产品") || desc.contains("商品") || 
               desc.contains("库存") || desc.contains("价格") ||
              tool.getToolCodeName().contains("product");
    }

    /**
     * 判断是否为订单相关工具
     */
  private boolean isOrderRelated(Tools tool) {
      String desc= tool.getDescription() != null ? tool.getDescription() : "";
        return desc.contains("订单") || desc.contains("物流") || 
               desc.contains("发货") || tool.getToolCodeName().contains("order");
    }

    private List<Tools> recommendToolsByRAG(String query, Long userId, List<Tools> availableTools) {
        long startTime = System.currentTimeMillis();
        
        try {
            String cacheKey = buildCacheKey(query, userId);
            List<Tools> cachedTools = toolCache.get(cacheKey);
            if (cachedTools != null && !cachedTools.isEmpty()) {
                log.debug("命中 RAG 工具推荐缓存");
                return cachedTools;
            }
            
            List<KnowledgeBase> knowledgeBases = knowledgeBaseService.getMyKnowledgeBases(userId, 1, 10);
            if (knowledgeBases.isEmpty()) {
                return new ArrayList<>();
            }
            
            List<Long> kbIds = knowledgeBases.stream()
                .filter(kb -> kb.getStatus() == 1)
                .map(KnowledgeBase::getId)
                .collect(Collectors.toList());
            
            if (kbIds.isEmpty()) {
                return new ArrayList<>();
            }
            
            int topK = ragProperties.getToolMatchingTopK() != null ? 
                      ragProperties.getToolMatchingTopK() : 3;
            double threshold = ragProperties.getToolMatchingThreshold() != null ? 
                              ragProperties.getToolMatchingThreshold() : 0.5;
            
            List<VectorSearchResult> results = vectorSearchService.batchSearch(
                query, kbIds, topK, threshold);
            
            if (results.isEmpty()) {
                log.debug("知识库检索无结果，跳过工具推荐");
                return new ArrayList<>();
            }
            
            Map<String, Double> toolScores = calculateToolScoresFromRAG(results, availableTools);
            
            if (toolScores.isEmpty()) {
                return new ArrayList<>();
            }
            
            double minScore = ragProperties.getMinScoreThreshold() != null ? 
                             ragProperties.getMinScoreThreshold() : 0.5;
            
            List<Tools> recommendedTools = availableTools.stream()
                .filter(tool -> {
                    String toolIdStr = tool.getId().toString();
                    return toolScores.containsKey(toolIdStr) && 
                           toolScores.get(toolIdStr) >= minScore;
                })
                .sorted(Comparator.comparingDouble(
                    tool -> -toolScores.getOrDefault(tool.getId().toString(), 0.0)
                ))
                .limit(5)
                .collect(Collectors.toList());
            
            long duration = System.currentTimeMillis() - startTime;
            log.info("基于 RAG 推荐 {} 个工具，耗时：{}ms", recommendedTools.size(), duration);
            
            if (!recommendedTools.isEmpty()) {
                toolCache.put(cacheKey, recommendedTools);
            }
            
            return recommendedTools;
            
        } catch (Exception e) {
            log.error("基于 RAG 的工具推荐失败", e);
            return new ArrayList<>();
        }
    }

    private Map<String, Double> calculateToolScoresFromRAG(List<VectorSearchResult> results, 
                                                           List<Tools> availableTools) {
        Map<String, Double> toolScores = new java.util.HashMap<>();
        int matchedByRelation = 0;
        int matchedByContent = 0;
        
        for (VectorSearchResult result : results) {
            Object metadata = result.metadata();
            if (metadata instanceof Map) {
                Map<?, ?> metaMap = (Map<?, ?>) metadata;
                
                Object relatedToolIds = metaMap.get("related_tool_ids");
                if (relatedToolIds instanceof List) {
                    List<?> toolIds = (List<?>) relatedToolIds;
                    double similarity = result.similarity() != null ? result.similarity() : 0.5;
                    double baseScore = similarity * 1.5;
                    
                    for (Object toolIdObj : toolIds) {
                        String toolIdStr = toolIdObj.toString();
                        
                        boolean exists = availableTools.stream()
                            .anyMatch(t -> t.getId().toString().equals(toolIdStr));
                        
                        if (exists) {
                            toolScores.merge(toolIdStr, baseScore, Double::sum);
                            matchedByRelation++;
                        }
                    }
                }
                
                Object content = result.content();
                if (content != null) {
                    String contentStr = content.toString().toLowerCase();
                    
                    for (Tools tool : availableTools) {
                        String toolName = tool.getToolCodeName().toLowerCase();
                        String toolDesc = tool.getDescription() != null ? 
                                        tool.getDescription().toLowerCase() : "";
                        
                        if (contentStr.contains(toolName) || contentStr.contains(toolDesc)) {
                            double contentScore = 0.3;
                            
                            if (contentStr.contains(toolName)) {
                                contentScore = 0.5;
                            }
                            
                            toolScores.merge(tool.getId().toString(), contentScore, Double::sum);
                            matchedByContent++;
                        }
                    }
                }
            }
        }
        
        log.debug("RAG 工具评分：通过关联匹配 {} 次，通过内容匹配 {} 次", matchedByRelation, matchedByContent);
        
        double minScore = ragProperties.getMinScoreThreshold() != null ? 
                         ragProperties.getMinScoreThreshold() : 0.5;
        
        return toolScores.entrySet().stream()
            .filter(e -> e.getValue() >= minScore)
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    private String buildCacheKey(String query, Long userId) {
        return "rag_tools:" + userId + ":" + query.toLowerCase().replaceAll("\\s+", "_");
    }

    private List<Tools> mergeAndRankTools(List<Tools> intentFilteredTools, 
                                          List<Tools> ragRecommendedTools, 
                                          String query) {
        Map<Long, Tools> toolMap = new java.util.HashMap<>();
        Map<Long, Double> toolScores = new java.util.HashMap<>();
        
        for (Tools tool : intentFilteredTools) {
            toolMap.put(tool.getId(), tool);
            double intentScore = calculateRelevanceScore(tool, query) / 100.0;
            toolScores.put(tool.getId(), intentScore);
        }
        
        for (Tools tool : ragRecommendedTools) {
            if (!toolMap.containsKey(tool.getId())) {
                toolMap.put(tool.getId(), tool);
                toolScores.put(tool.getId(), 0.8);
            } else {
                double currentScore = toolScores.get(tool.getId());
                toolScores.put(tool.getId(), currentScore + 0.5);
            }
        }
        
        return toolScores.entrySet().stream()
            .sorted(Map.Entry.<Long, Double>comparingByValue().reversed())
            .map(entry -> toolMap.get(entry.getKey()))
            .collect(Collectors.toList());
    }
}
