package com.esdllm.agentmesh.service.impl;


import com.esdllm.agentmesh.common.ErrorCode;
import com.esdllm.agentmesh.exception.BusinessException;
import com.esdllm.agentmesh.model.domain.Agent;
import com.esdllm.agentmesh.model.domain.KnowledgeBase;
import com.esdllm.agentmesh.model.domain.KnowledgeBaseDocument;
import com.esdllm.agentmesh.model.dto.request.AgentAddRequest;
import com.esdllm.agentmesh.model.dto.request.BatchImportDocumentsRequest;
import com.esdllm.agentmesh.repository.dao.AgentDao;
import com.esdllm.agentmesh.repository.dao.KnowledgeBaseDao;
import com.esdllm.agentmesh.repository.dao.KnowledgeBaseDocumentDao;
import com.esdllm.agentmesh.service.BatchOperationService;
import com.esdllm.agentmesh.service.DocumentProcessingService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * 批量操作服务实现类
 */
@Service
@Slf4j
public class BatchOperationServiceImpl implements BatchOperationService {
    
    @Resource
    private KnowledgeBaseDocumentDao knowledgeBaseDocumentDao;
    
    @Resource
    private KnowledgeBaseDao knowledgeBaseDao;
    
    @Resource
    private AgentDao agentDao;
    
    @Resource
    private DocumentProcessingService documentProcessingService;
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public BatchImportResult batchImportDocuments(Long kbId, BatchImportDocumentsRequest request, Long userId) {
        List<String> errorMessages = new ArrayList<>();
        List<Long> documentIds = new ArrayList<>();
        int successCount = 0;
        int failCount = 0;
        
        // 1. 验证知识库是否存在且属于当前用户
        KnowledgeBase kb = knowledgeBaseDao.getById(kbId);
        if (kb == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "知识库不存在");
        }
        if (!kb.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.NO_AUTH, "无权访问该知识库");
        }
        
        // 2. 批量导入文档
        for (BatchImportDocumentsRequest.DocumentImportRequest docReq : request.getDocuments()) {
            try {
                KnowledgeBaseDocument document = new KnowledgeBaseDocument();
                document.setKbId(kbId);
                document.setDocName(docReq.getDocName());
                document.setDocType(docReq.getDocType());
                document.setSourceUrl(docReq.getSourceUrl());
                document.setIsDelete(0);
                document.setStatus(0); // 处理中
                
                // 保存文档记录
                knowledgeBaseDocumentDao.save(document);
                documentIds.add(document.getId());
                
                // 如果异步处理，启动异步任务
                if (request.getAsync()) {
                    Long documentId = document.getId();
                    CompletableFuture.runAsync(() -> {
                        try {
                            // 调用异步文档处理方法
                            log.info("开始异步处理文档：{}", documentId);
                            
                            // 获取文档信息
                            KnowledgeBaseDocument doc = knowledgeBaseDocumentDao.getById(documentId);
                            if (doc != null && doc.getSourceUrl() != null) {
                                // 调用文档处理服务
                                documentProcessingService.processDocumentAsync(documentId);
                                log.info("异步处理文档完成：{}", documentId);
                            }
                            
                        } catch (Exception e) {
                            log.error("异步处理文档失败：{}", documentId, e);
                            // 更新文档状态为失败
                            KnowledgeBaseDocument failedDoc = new KnowledgeBaseDocument();
                            failedDoc.setId(documentId);
                            failedDoc.setStatus(-1);
                            failedDoc.setUpdatedAt(new Date());
                            knowledgeBaseDocumentDao.updateById(failedDoc);
                        }
                    });
                }
                
                successCount++;
            } catch (Exception e) {
                log.error("导入文档失败：{}", docReq.getDocName(), e);
                errorMessages.add("导入失败：" + docReq.getDocName() + " - " + e.getMessage());
                failCount++;
            }
        }
        
        log.info("批量导入文档完成，总数：{}, 成功：{}, 失败：{}", 
                request.getDocuments().size(), successCount, failCount);
        
        return new BatchImportResult(successCount, failCount, documentIds, errorMessages);
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public BatchUpdateResult batchUpdateAgents(List<Long> agentIds, Object updateConfig, Long userId) {
        List<String> errorMessages = new ArrayList<>();
        int successCount = 0;
        int failCount = 0;
        
        if (CollectionUtils.isEmpty(agentIds)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "智能体 ID 列表不能为空");
        }
        
        for (Long agentId : agentIds) {
            try {
                // 查询智能体是否存在且属于当前用户
                Agent agent = agentDao.getById(agentId);
                if (agent == null) {
                    throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "智能体不存在");
                }
                if (!agent.getUserId().equals(userId)) {
                    throw new BusinessException(ErrorCode.NO_AUTH, "无权修改该智能体");
                }
                
                // 根据 updateConfig 更新智能体字段
                if (updateConfig instanceof Map) {
                    Map<String, Object> config = (Map<String, Object>) updateConfig;
                    
                    // 更新基本信息
                    if (config.containsKey("name")) {
                        agent.setName((String) config.get("name"));
                    }
                    if (config.containsKey("description")) {
                        agent.setDescription((String) config.get("description"));
                    }
                    if (config.containsKey("systemPrompt")) {
                        agent.setSystemPrompt((String) config.get("systemPrompt"));
                    }
                    if (config.containsKey("roleDefinition")) {
                        agent.setRoleDefinition((String) config.get("roleDefinition"));
                    }
                    if (config.containsKey("decisionModelId")) {
                        agent.setDecisionModelId(((Number) config.get("decisionModelId")).longValue());
                    }
                    if (config.containsKey("responseModelId")) {
                        agent.setResponseModelId(((Number) config.get("responseModelId")).longValue());
                    }
                    if (config.containsKey("isToolEnabled")) {
                        agent.setIsToolEnabled((Boolean) config.get("isToolEnabled"));
                    }
                    if (config.containsKey("status")) {
                        agent.setStatus(((Number) config.get("status")).intValue());
                    }
                    
                } else if (updateConfig instanceof AgentAddRequest) {
                    // 如果是 AgentAddRequest 对象
                    AgentAddRequest req = (AgentAddRequest) updateConfig;
                    agent.setName(req.getName());
                    agent.setDescription(req.getDescription());
                    agent.setSystemPrompt(req.getSystemPrompt());
                    agent.setRoleDefinition(req.getRoleDefinition());
                    if (req.getDecisionModelId() != null) {
                        agent.setDecisionModelId(req.getDecisionModelId());
                    }
                    if (req.getResponseModelId() != null) {
                        agent.setResponseModelId(req.getResponseModelId());
                    }
                }
                
                agent.setUpdatedAt(new Date());
                agentDao.updateById(agent);
                
                successCount++;
            } catch (Exception e) {
                log.error("更新智能体失败：agentId={}", agentId, e);
                errorMessages.add("更新失败：ID=" + agentId + " - " + e.getMessage());
                failCount++;
            }
        }
        
        log.info("批量更新智能体完成，总数：{}, 成功：{}, 失败：{}", 
                agentIds.size(), successCount, failCount);
        
        return new BatchUpdateResult(successCount, failCount, errorMessages);
    }
}
