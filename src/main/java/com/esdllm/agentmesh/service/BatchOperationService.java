package com.esdllm.agentmesh.service;


import com.esdllm.agentmesh.model.domain.KnowledgeBaseDocument;
import com.esdllm.agentmesh.model.dto.request.BatchImportDocumentsRequest;

import java.util.List;

/**
 * 批量操作服务接口
 */
public interface BatchOperationService {
    
    /**
     * 批量导入知识库文档
     * @param kbId 知识库 ID
     * @param request 批量导入请求
     * @param userId 用户 ID
     * @return 导入结果（成功数量、失败数量、文档 ID 列表）
     */
    BatchImportResult batchImportDocuments(Long kbId, BatchImportDocumentsRequest request, Long userId);
    
    /**
     * 批量更新智能体配置
     * @param agentIds 智能体 ID 列表
     * @param updateConfig 更新配置
     * @param userId 用户 ID
     * @return 更新结果（成功数量、失败数量）
     */
    BatchUpdateResult batchUpdateAgents(List<Long> agentIds, Object updateConfig, Long userId);
    
    /**
     * 批量导入结果
     */
    record BatchImportResult(
        Integer successCount,
        Integer failCount,
        List<Long> documentIds,
        List<String> errorMessages
    ) {}
    
    /**
     * 批量更新结果
     */
    record BatchUpdateResult(
        Integer successCount,
        Integer failCount,
        List<String> errorMessages
    ) {}
}
