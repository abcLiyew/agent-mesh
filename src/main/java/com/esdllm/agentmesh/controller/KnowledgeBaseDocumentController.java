package com.esdllm.agentmesh.controller;

import com.esdllm.agentmesh.common.BaseResponse;
import com.esdllm.agentmesh.common.ResultUtils;
import com.esdllm.agentmesh.model.domain.KnowledgeBaseDocument;
import com.esdllm.agentmesh.model.domain.User;
import com.esdllm.agentmesh.model.dto.request.BatchImportDocumentsRequest;
import com.esdllm.agentmesh.service.BatchOperationService;
import com.esdllm.agentmesh.service.KnowledgeBaseDocumentService;
import com.esdllm.agentmesh.service.UserService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/knowledge-base/document")
public class KnowledgeBaseDocumentController {
    
    @Resource
    private KnowledgeBaseDocumentService knowledgeBaseDocumentService;
    @Resource
    private UserService userService;
    @Resource
    private BatchOperationService batchOperationService;

    @PostMapping("/add/{kbId}")
    public BaseResponse<Long> addDocument(
        @PathVariable Long kbId,
        @RequestBody KnowledgeBaseDocument document
    ) {
        Long docId = knowledgeBaseDocumentService.createDocument(document, kbId);
        return ResultUtils.success(docId);
    }

    @PutMapping("/update")
    public BaseResponse<Boolean> updateDocument(@RequestBody KnowledgeBaseDocument document) {
        Boolean result = knowledgeBaseDocumentService.updateDocument(document);
        return ResultUtils.success(result);
    }

    @DeleteMapping("/delete/{docId}")
    public BaseResponse<Boolean> deleteDocument(@PathVariable Long docId) {
        Boolean result = knowledgeBaseDocumentService.deleteDocument(docId);
        return ResultUtils.success(result);
    }

    @GetMapping("/list/{kbId}")
    public BaseResponse<List<KnowledgeBaseDocument>> getDocumentsByKb(
        @PathVariable Long kbId,
        @RequestParam(defaultValue = "1") int page,
        @RequestParam(defaultValue = "20") int pageSize
    ) {
        List<KnowledgeBaseDocument> docList = knowledgeBaseDocumentService.getDocumentsByKb(kbId, page, pageSize);
        return ResultUtils.success(docList);
    }

    @GetMapping("/{docId}")
    public BaseResponse<KnowledgeBaseDocument> getDocument(@PathVariable Long docId) {
        KnowledgeBaseDocument document = knowledgeBaseDocumentService.getDocumentById(docId);
        return ResultUtils.success(document);
    }

    @PutMapping("/status/{docId}")
    public BaseResponse<Boolean> updateDocumentStatus(
        @PathVariable Long docId,
        @RequestParam Integer status,
        @RequestParam(required = false) Integer chunkCount,
        @RequestBody(required = false) Object vectorIds
    ) {
        Boolean result = knowledgeBaseDocumentService.updateDocumentStatus(docId, status, chunkCount, vectorIds);
        return ResultUtils.success(result);
    }

    /**
     * 获取文档处理进度
     */
    @GetMapping("/progress/{docId}")
    public BaseResponse<Map<String, Object>> getDocumentProgress(@PathVariable Long docId) {
        KnowledgeBaseDocument document = knowledgeBaseDocumentService.getDocumentById(docId);

        Map<String, Object> progress = new HashMap<>();
        progress.put("docId", document.getId());
        progress.put("docName", document.getDocName());
        progress.put("status", document.getStatus());
        progress.put("chunkCount", document.getChunkCount());

        // 根据状态返回友好提示
        String statusText = switch (document.getStatus()) {
            case 0 -> "处理中";
            case 1 -> "处理完成";
            case -1 -> "处理失败";
            default -> "未知状态";
        };
        progress.put("statusText", statusText);

        return ResultUtils.success(progress);
    }
    /**
     * 批量导入文档
     */
    @PostMapping("/batch-import/{kbId}")
    public BaseResponse<Map<String, Object>> batchImportDocuments(
            @PathVariable Long kbId,
            @RequestBody BatchImportDocumentsRequest request,
            HttpServletRequest httpRequest
    ) {
        User loginUser = userService.getLoginUser(httpRequest.getSession());

        Map<String, Object> result = new HashMap<>();
        var importResult = batchOperationService.batchImportDocuments(kbId, request, loginUser.getId());

        result.put("successCount", importResult.successCount());
        result.put("failCount", importResult.failCount());
        result.put("documentIds", importResult.documentIds());
        result.put("errorMessages", importResult.errorMessages());

        return ResultUtils.success(result);
    }
}
