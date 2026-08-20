package com.esdllm.agentmesh.service;


import com.esdllm.agentmesh.model.domain.KnowledgeBase;
import com.esdllm.agentmesh.model.dto.request.BatchImportDocumentsRequest;
import com.esdllm.agentmesh.repository.dao.KnowledgeBaseDao;
import com.esdllm.agentmesh.repository.dao.KnowledgeBaseDocumentDao;
import com.esdllm.agentmesh.service.impl.BatchOperationServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * BatchOperationService 单元测试
 */
class BatchOperationServiceTest {
    
    @Mock
    private KnowledgeBaseDocumentDao knowledgeBaseDocumentDao;
    
    @Mock
    private KnowledgeBaseDao knowledgeBaseDao;
    
    @InjectMocks
    private BatchOperationServiceImpl batchOperationService;
    
    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }
    
    @Test
    void testBatchImportDocuments_Success() {
        // 准备测试数据
        Long kbId = 1L;
        Long userId = 100L;
        
        KnowledgeBase mockKb = new KnowledgeBase();
        mockKb.setId(kbId);
        mockKb.setUserId(userId);
        
        BatchImportDocumentsRequest request = new BatchImportDocumentsRequest();
        BatchImportDocumentsRequest.DocumentImportRequest docReq1 = 
            new BatchImportDocumentsRequest.DocumentImportRequest();
        docReq1.setDocName("文档 1.pdf");
        docReq1.setDocType("PDF");
        
        BatchImportDocumentsRequest.DocumentImportRequest docReq2 = 
            new BatchImportDocumentsRequest.DocumentImportRequest();
        docReq2.setDocName("文档 2.md");
        docReq2.setDocType("MARKDOWN");
        
        request.setDocuments(Arrays.asList(docReq1, docReq2));
        request.setAsync(true);

        // 模拟依赖行为
        when(knowledgeBaseDao.getById(kbId)).thenReturn(mockKb);
        when(knowledgeBaseDocumentDao.save(any())).thenReturn(true);

        // 执行测试
        var result = batchOperationService.batchImportDocuments(kbId, request, userId);

        // 验证结果
        assertNotNull(result);
        assertEquals(2, result.successCount());
        assertEquals(0, result.failCount());
    }
}
