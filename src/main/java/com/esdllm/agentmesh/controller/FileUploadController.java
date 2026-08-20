package com.esdllm.agentmesh.controller;

import cn.hutool.core.util.StrUtil;
import com.esdllm.agentmesh.common.BaseResponse;
import com.esdllm.agentmesh.common.ResultUtils;
import com.esdllm.agentmesh.model.domain.KnowledgeBaseDocument;
import com.esdllm.agentmesh.model.domain.User;
import com.esdllm.agentmesh.repository.dao.KnowledgeBaseDocumentDao;
import com.esdllm.agentmesh.service.DocumentProcessingService;
import com.esdllm.agentmesh.service.FileUploadService;
import com.esdllm.agentmesh.service.KnowledgeBaseDocumentService;
import com.esdllm.agentmesh.service.UserService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/file")
@Slf4j
public class FileUploadController {

    @Resource
  private FileUploadService fileUploadService;

    @Resource
  private KnowledgeBaseDocumentService knowledgeBaseDocumentService;

    @Resource
  private com.esdllm.agentmesh.service.KnowledgeBaseService knowledgeBaseService;

    @Resource
  private DocumentProcessingService documentProcessingService;

    @Resource
  private UserService userService;
    @Resource
    private KnowledgeBaseDocumentDao knowledgeBaseDocumentDao;

    /**
     * 上传文件到知识库（带文档处理）
     */
    @PostMapping("/upload-to-kb/{kbId}")
    public BaseResponse<Map<String, Object>> uploadFileToKnowledgeBase(
        @PathVariable Long kbId,
        @RequestParam("file") MultipartFile file,
        @RequestParam(value = "docName", required = false) String docName,
        HttpServletRequest request
    ) {
        try {
            User loginUser= userService.getLoginUser(request.getSession());
            
            // 1. 上传文件
           String filePath = fileUploadService.uploadFile(file, loginUser.getId());
            
            // 2. 创建文档记录
            KnowledgeBaseDocument document = new KnowledgeBaseDocument();
            document.setDocName(docName != null ? docName : file.getOriginalFilename());
            document.setDocType(getDocTypeFromExtension(file.getOriginalFilename()));
            document.setSourceUrl(filePath);
            
            Long docId = knowledgeBaseDocumentService.createDocument(document, kbId);
            
            // 3. 异步处理文档（提取文本、分块、向量化）
           String absolutePath = fileUploadService.getAbsoluteFilePath(filePath);
            if (absolutePath != null) {
               processDocumentAsync(docId, absolutePath);
            }
            
            // 4. 返回结果
            Map<String, Object> result = new HashMap<>();
            result.put("docId", docId);
            result.put("fileName", file.getOriginalFilename());
            result.put("filePath", filePath);
            result.put("status", "processing");
            result.put("message", "文件上传成功，正在后台处理中...");
            
          log.info("文件上传成功，userId: {}, kbId: {}, docId: {}", loginUser.getId(), kbId, docId);
            return ResultUtils.success(result);
            
        } catch (Exception e) {
          log.error("上传文件失败", e);
            throw e;
        }
    }

    /**
     * 通用文件上传（不关联知识库）
     */
    @PostMapping("/upload")
    public BaseResponse<Map<String, String>> uploadFile(
        @RequestParam("file") MultipartFile file,
        HttpServletRequest request
    ) {
        User loginUser= userService.getLoginUser(request.getSession());
       String filePath = fileUploadService.uploadFile(file, loginUser.getId());
        
        Map<String, String> result = new HashMap<>();
        result.put("filePath", filePath);
        result.put("fileName", file.getOriginalFilename());
        result.put("fileSize", String.valueOf(file.getSize()));
        
        return ResultUtils.success(result);
    }

    /**
     * 删除文件
     */
    @DeleteMapping("/delete")
    public BaseResponse<Boolean> deleteFile(
        @RequestParam String filePath,
        HttpServletRequest request
    ) {
        User loginUser= userService.getLoginUser(request.getSession());
        fileUploadService.deleteFile(filePath, loginUser.getId());
        return ResultUtils.success(true);
    }

    /**
     * 重新处理文档
     */
    @PostMapping("/reprocess/{docId}")
    public BaseResponse<Boolean> reprocessDocument(
        @PathVariable Long docId,
        HttpServletRequest request
    ) {
        User loginUser= userService.getLoginUser(request.getSession());
        
        try {
            KnowledgeBaseDocument document= knowledgeBaseDocumentService.getDocumentById(docId);
            if (document == null) {
                throw new RuntimeException("文档不存在");
            }
            
            // 验证权限（检查是否属于该用户）
            com.esdllm.agentmesh.model.domain.KnowledgeBase knowledgeBase = knowledgeBaseService.getKnowledgeBaseById(document.getKbId(), loginUser.getId());
            if (knowledgeBase == null) {
                throw new RuntimeException("无权操作该文档");
            }
            
           String absolutePath = fileUploadService.getAbsoluteFilePath(document.getSourceUrl());
            if (absolutePath != null) {
               processDocumentAsync(docId, absolutePath);
            }
            
            return ResultUtils.success(true);
        } catch (Exception e) {
          log.error("重新处理文档失败，docId: {}", docId, e);
            throw e;
        }
    }

    /**
     * 异步处理文档
     */
  private void processDocumentAsync(Long docId, String filePath) {
        new Thread(() -> {
            try {
              log.info("开始异步处理文档，docId: {}, filePath: {}", docId, filePath);
              
              // 验证文件是否存在
              File file = new File(filePath);
              if (!file.exists()) {
                  log.error("异步处理失败：文件不存在，docId: {}, filePath: {}, 绝对路径：{}", 
                           docId, filePath, file.getAbsolutePath());
                  // 更新文档状态为失败
                  KnowledgeBaseDocument failedDoc = new KnowledgeBaseDocument();
                  failedDoc.setId(docId);
                  failedDoc.setStatus(-1);
                  failedDoc.setUpdatedAt(new Date());
                  knowledgeBaseDocumentDao.updateById(failedDoc);
                  return;
              }
              
                documentProcessingService.processUploadedDocument(docId, filePath);
              log.info("文档处理完成，docId: {}", docId);
            } catch (Exception e) {
              log.error("异步处理文档失败，docId: {}, filePath: {}", docId, filePath, e);
            }
        }).start();
    }

    /**
     * 根据文件扩展名判断文档类型
     */
  private String getDocTypeFromExtension(String filename) {
        if (StrUtil.isBlank(filename)) {
            return "TEXT";
        }
        
       String extension= getFileExtension(filename);
        return switch (extension.toLowerCase()) {
            case "pdf" -> "PDF";
            case "doc", "docx" -> "WORD";
            case "xls", "xlsx" -> "EXCEL";
            case "md", "markdown" -> "MARKDOWN";
            case "csv" -> "CSV";
            case "json" -> "JSON";
            default -> "TEXT";
        };
    }

    /**
     * 获取文件扩展名
     */
  private String getFileExtension(String filename) {
        if (!filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf(".") + 1);
    }
}
