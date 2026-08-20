package com.esdllm.agentmesh.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.esdllm.agentmesh.common.ErrorCode;
import com.esdllm.agentmesh.exception.BusinessException;
import com.esdllm.agentmesh.model.domain.Agent;
import com.esdllm.agentmesh.model.domain.AgentToolRelation;
import com.esdllm.agentmesh.model.domain.KnowledgeBase;
import com.esdllm.agentmesh.model.domain.Tools;
import com.esdllm.agentmesh.model.dto.VectorSearchResult;
import com.esdllm.agentmesh.model.dto.response.AgentToolResponse;
import com.esdllm.agentmesh.repository.dao.AgentDao;
import com.esdllm.agentmesh.repository.dao.AgentToolRelationDao;
import com.esdllm.agentmesh.repository.dao.ToolsDao;
import com.esdllm.agentmesh.service.*;
import com.esdllm.agentmesh.service.unified.UnifiedAgentEngine;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 智能体工具服务实现类
 */
@Service
@Slf4j
public class AgentToolServiceImpl implements AgentToolService {

    @Resource
  private ToolsDao toolsDao;

    @Resource
  private AgentDao agentDao;

    @Resource
    @Lazy
    private UnifiedAgentEngine unifiedAgentEngine;

    @Resource
    private AgentToolRelationDao agentToolRelationDao;

    @Resource
    private VectorSearchService vectorSearchService;

    @Resource
    private KnowledgeBaseService knowledgeBaseService;

    @Transactional(rollbackFor = Exception.class)
    @Override
    public Long registerAgentAsTool(Long agentId, String toolCodeName,
                                    String displayName, String description, Long userId) {
    long startTime = System.currentTimeMillis();
    
    log.info("开始注册智能体为工具，agentId: {}, toolCodeName: {}", agentId, toolCodeName);
    
    try {
        // 1. 验证智能体存在性和权限
        Agent agent = agentDao.getById(agentId);
        if (agent == null) {
            throw new BusinessException(ErrorCode.AGENT_NOT_FOUND, "智能体不存在");
        }
        
        if (!agent.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.NO_AUTH, "无权限操作该智能体");
        }
        
        // 2. 检查是否已注册为工具
        List<Tools> existingTools = toolsDao.list(new LambdaQueryWrapper<Tools>()
                .eq(Tools::getSourceType, "USER_AGENT")
                .eq(Tools::getMcpServerId, agentId)
                .eq(Tools::getOwnerId, userId));
        
        if (!existingTools.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, 
                    "该智能体已注册为工具：" + existingTools.getFirst().getToolCodeName());
        }
        
        // 3. 创建工具记录
        Tools tool = new Tools();
        tool.setOwnerId(userId);
        tool.setSourceType("USER_AGENT");
        tool.setToolCodeName(toolCodeName);
        tool.setDisplayName(displayName);
        tool.setDescription(description);
        tool.setIsEnabled(true);
        tool.setIsDelete(0);
        
        // 4. 构建输入输出 Schema
        Map<String, Object> inputSchema = buildAgentToolInputSchema(agent);
        tool.setInputSchema(inputSchema);
        
        Map<String, Object> outputSchema = new HashMap<>();
        outputSchema.put("type", "object");
        Map<String, Object> properties = new HashMap<>();
        properties.put("result", Map.of("type", "string", "description", "智能体返回的结果"));
        outputSchema.put("properties", properties);
        tool.setOutputSchema(outputSchema);
        
        boolean saved = toolsDao.save(tool);
        if (!saved) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "注册工具失败");
        }
        
        // 5. 添加到智能体 - 工具关联表
        AgentToolRelation relation = new AgentToolRelation();
        relation.setAgentId(agentId);
        relation.setToolType("USER_AGENT");
        relation.setToolRefId(tool.getId());
        relation.setSortOrder(0);
        relation.setIsDelete(0);
        agentToolRelationDao.save(relation);
        
        log.info("智能体注册为工具成功，toolId: {}, time: {}ms", 
                tool.getId(), System.currentTimeMillis() - startTime);
        
        return tool.getId();
        
    } catch (BusinessException e) {
        throw e;
    } catch (Exception e) {
        log.error("注册智能体工具失败", e);
        throw new BusinessException(ErrorCode.SYSTEM_ERROR, "注册失败：" + e.getMessage());
    }
}

/**
 * 构建智能体工具的输入 Schema
 */
private Map<String, Object> buildAgentToolInputSchema(Agent agent) {
    Map<String, Object> schema = new HashMap<>();
    schema.put("type", "object");
    
    Map<String, Object> properties = new HashMap<>();
    
    // query 参数：用户的自然语言问题
    Map<String, Object> queryParam = new HashMap<>();
    queryParam.put("type", "string");
    queryParam.put("description", "用户的问题或请求");
    properties.put("query", queryParam);
    
    // 可选：其他动态参数
    Map<String, Object> additionalParams = new HashMap<>();
    additionalParams.put("type", "object");
    additionalParams.put("description", "额外的参数");
    additionalParams.put("required", false);
    properties.put("parameters", additionalParams);
    
    schema.put("properties", properties);
    schema.put("required", List.of("query"));
    
    return schema;
}

    @Override
    public AgentToolResponse invokeAgentTool(Long agentId, String query,
                                             Object parameters, Long userId) {
        long startTime = System.currentTimeMillis();

        log.info("开始调用智能体工具，agentId: {}, query: {}", agentId, query);

        try {
            Agent agent= agentDao.getById(agentId);
            if (agent == null) {
                throw new BusinessException(ErrorCode.AGENT_NOT_FOUND, "智能体不存在");
            }

            List<String> ragContext = enhanceQueryWithRAG(query, agent.getUserId());

            String enhancedQuery = buildEnhancedQuery(query, ragContext);

            var result = unifiedAgentEngine.execute(agentId, enhancedQuery, userId, null, null);

            long executionTime= System.currentTimeMillis() - startTime;

            double cost = result.getTotalCost() != null ? result.getTotalCost() : 0.1;

            log.info("智能体工具调用成功，time: {}ms, cost: {}", executionTime, cost);

            return new AgentToolResponse(
                    result.getFinalResponse(),
                    executionTime,
                    cost
            );

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("智能体工具调用失败", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "调用失败：" + e.getMessage());
        }
    }


    @Override
    public List<Tools> getAgentTools(Long userId) {
        if (userId == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户 ID 不能为空");
        }

        // 获取类型为 USER_AGENT 的工具
       LambdaQueryWrapper<Tools> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Tools::getOwnerId, userId)
                   .eq(Tools::getSourceType, "USER_AGENT")
                   .eq(Tools::getIsEnabled, true)
                   .eq(Tools::getIsDelete, 0);
        
        return toolsDao.list(queryWrapper);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void updateAgentToolStatus(Long agentId, Boolean isEnabled, Long userId) {
        if (agentId == null || isEnabled == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数不能为空");
        }

        // 1. 验证智能体权限
       Agent agent= agentDao.getById(agentId);
        if (agent == null || !agent.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.NO_AUTH, "无权限操作该智能体");
        }

        // 2. 查找对应的工具记录（通过描述匹配）
       LambdaQueryWrapper<Tools> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Tools::getSourceType, "USER_AGENT")
                   .eq(Tools::getOwnerId, userId)
                   .eq(Tools::getIsDelete, 0)
                   .eq(Tools::getMcpServerId, agentId);
        
       Tools tool = toolsDao.getOne(queryWrapper);
        if (tool == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "未找到对应的工具记录");
        }

        // 3. 更新状态
      tool.setIsEnabled(isEnabled);
      tool.setUpdatedAt(new Date());
        
      toolsDao.updateById(tool);

     log.info("智能体工具状态更新成功，agentId: {}, isEnabled: {}", agentId, isEnabled);
    }

    private List<String> enhanceQueryWithRAG(String query, Long userId) {
        try {
            List<KnowledgeBase> knowledgeBases = knowledgeBaseService.getMyKnowledgeBases(userId, 1, 10);
            if (knowledgeBases.isEmpty()) {
                log.debug("用户暂无可用知识库");
                return Collections.emptyList();
            }

            List<Long> kbIds = knowledgeBases.stream()
                    .filter(kb -> kb.getStatus() == 1)
                    .map(KnowledgeBase::getId)
                    .collect(Collectors.toList());

            if (kbIds.isEmpty()) {
                return Collections.emptyList();
            }

            List<VectorSearchResult> results = vectorSearchService.batchSearch(query, kbIds, 5, 0.5);

            if (results.isEmpty()) {
                log.debug("知识库检索无匹配结果");
                return Collections.emptyList();
            }

            List<String> contextList = results.stream()
                    .map(VectorSearchResult::content)
                    .filter(StrUtil::isNotBlank)
                    .collect(Collectors.toList());

            log.info("RAG 上下文增强成功，找到 {} 条相关知识", contextList.size());
            return contextList;

        } catch (Exception e) {
            log.error("RAG 上下文增强失败", e);
            return Collections.emptyList();
        }
    }

    private String buildEnhancedQuery(String originalQuery, List<String> ragContext) {
        if (ragContext.isEmpty()) {
            return originalQuery;
        }

        StringBuilder sb = new StringBuilder();
        sb.append("参考信息：\n");
        for (int i = 0; i < ragContext.size(); i++) {
            sb.append(i + 1).append(". ").append(ragContext.get(i)).append("\n");
        }
        sb.append("\n用户问题：").append(originalQuery);
        sb.append("\n\n请结合上述参考信息回答问题。如果参考信息与问题无关，请忽略。");

        return sb.toString();
    }

    @Override
    public Object getAgentToolConfigWithCache(Long agentId) {
        log.debug("从缓存获取智能体工具配置，agentId: {}", agentId);

        try {
            // 查询智能体的工具配置
            LambdaQueryWrapper<AgentToolRelation> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(AgentToolRelation::getAgentId, agentId)
                    .eq(AgentToolRelation::getIsDelete, 0);

            List<AgentToolRelation> relations = agentToolRelationDao.list(queryWrapper);

            if (relations.isEmpty()) {
                return null;
            }

            // 获取关联的工具信息
            List<Long> toolIds = relations.stream()
                    .map(AgentToolRelation::getToolRefId)
                    .collect(Collectors.toList());

            LambdaQueryWrapper<Tools> toolsQuery = new LambdaQueryWrapper<>();
            toolsQuery.in(Tools::getId, toolIds)
                    .eq(Tools::getIsEnabled, true)
                    .eq(Tools::getIsDelete, 0);

            return toolsDao.list(toolsQuery);

        } catch (Exception e) {
            log.error("获取智能体工具配置失败", e);
            return null;
        }
    }
    @Override
    public List<Tools> getAgentToolsWithCache(Long userId) {
        log.debug("从缓存获取用户工具列表，userId: {}", userId);
        return getAgentTools(userId);
    }
}
