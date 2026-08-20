package com.esdllm.agentmesh.service;

import cn.hutool.core.util.StrUtil;
import com.esdllm.agentmesh.common.ErrorCode;
import com.esdllm.agentmesh.exception.BusinessException;
import com.esdllm.agentmesh.model.domain.AiModel;
import com.esdllm.agentmesh.model.domain.KnowledgeBase;
import com.esdllm.agentmesh.model.domain.KnowledgeBaseDocument;
import com.esdllm.agentmesh.repository.dao.AiModelDao;
import com.esdllm.agentmesh.repository.dao.KnowledgeBaseDao;
import com.esdllm.agentmesh.repository.dao.KnowledgeBaseDocumentDao;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.TextReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.core.io.FileSystemResource;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;

@Service
@Slf4j
public class DocumentProcessingService {

    @Resource
   private KnowledgeBaseDocumentDao knowledgeBaseDocumentDao;

    @Resource
   private KnowledgeBaseDao knowledgeBaseDao;

    @Resource
    private FileUploadService fileUploadService;

    @Resource
    private AiModelDao aiModelDao;

    @Resource
    private VectorSearchService vectorSearchService;

    /**
     * 处理上传的文档
     */
    @Transactional(rollbackFor = Exception.class)
    public void processUploadedDocument(Long docId, String filePath) {
        log.info("开始处理上传文档，docId: {}, 文件路径：{}", docId, filePath);
        
        try {
            // 1. 查询文档信息
            KnowledgeBaseDocument document = knowledgeBaseDocumentDao.getById(docId);
            if (document == null) {
                log.error("文档不存在，docId: {}", docId);
                throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "文档不存在");
            }
            
            log.debug("文档信息：{}, 类型：{}, 知识库 ID: {}, 源 URL: {}", 
                document.getDocName(), document.getDocType(), document.getKbId(), document.getSourceUrl());

            // 2. 获取知识库信息
            KnowledgeBase kb = knowledgeBaseDao.getById(document.getKbId());
            if (kb == null) {
                log.error("知识库不存在，kbId: {}", document.getKbId());
                throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "知识库不存在");
            }

            // 3. 使用 Spring AI 读取并处理文档
            log.debug("准备使用 Spring AI 读取文件：{}", filePath);
            
            // 验证文件路径
            if (filePath == null || filePath.isEmpty()) {
                log.error("文件路径为空，docId: {}", docId);
                throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "文件路径不能为空");
            }
            
            File file = new File(filePath);
            if (!file.exists()) {
                log.error("文件不存在！docId: {}, 文件路径：{}, 绝对路径：{}", 
                         docId, filePath, file.getAbsolutePath());
                throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "文件不存在：" + filePath);
            }
            
            if (!file.canRead()) {
                log.error("文件无法读取！docId: {}, 文件路径：{}", docId, filePath);
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "文件无法读取：" + filePath);
            }
            
            log.debug("文件验证通过，大小：{} bytes", file.length());
            
            // 使用 Spring AI 的 TextReader 读取文档
            List<Document> documents = readDocumentWithSpringAI(filePath, document.getDocType());
            if (documents.isEmpty()) {
                log.error("无法从文件中读取文档内容，docId: {}, 文件路径：{}", docId, filePath);
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "无法从文件中读取文档内容");
            }
            
            // 合并所有文档内容为单个字符串（用于计算哈希）
            String content = mergeDocumentContent(documents);
            log.debug("文件内容读取成功，文档数量：{}, 总长度：{} 字符", documents.size(), content.length());

            // 4. 计算内容哈希（用于去重）
            String contentHash = calculateContentHash(content);
            document.setContentHash(contentHash);

            // 5. 使用 Spring AI 进行文本分块
            List<String> chunks = splitDocumentsWithSpringAI(documents, kb.getChunkSize(), kb.getChunkOverlap());
            document.setChunkCount(chunks.size());

            // 6. 调用 Embedding 模型生成向量并存储
            try {
                List<String> vectorIds = storeVectors(chunks, kb);
                document.setVectorIds(vectorIds);
                log.info("文档向量化成功，docId: {}, 生成 {} 个向量", docId, vectorIds.size());
            } catch (Exception e) {
                log.error("文档向量化失败，docId: {}", docId, e);
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "文档向量化失败：" + e.getMessage(), e);
            }
            // 7. 更新文档状态
            document.setStatus(1); // 处理完成
            document.setUpdatedAt(new java.util.Date());
            
            knowledgeBaseDocumentDao.updateById(document);

          log.info("文档处理成功，docId: {}, chunkCount: {}", docId, chunks.size());

        } catch (Exception e) {
          log.error("文档处理失败，docId: {}", docId, e);
            // 更新状态为失败
            KnowledgeBaseDocument document = new KnowledgeBaseDocument();
            document.setId(docId);
            document.setStatus(-1);
            document.setUpdatedAt(new java.util.Date());
            knowledgeBaseDocumentDao.updateById(document);
            
            // 根据异常类型提供更具体的错误信息
            String errorMessage = buildDetailedErrorMessage(e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, errorMessage, e);
        }
    }

    /**
     * 构建详细的错误消息
     */
    private String buildDetailedErrorMessage(Exception e) {
        if (e instanceof BusinessException) {
            return e.getMessage();
        }
        
        if (e.getCause() != null && e.getCause() instanceof BusinessException) {
            return e.getCause().getMessage();
        }
        
        // 根据异常类型提供更多信息
        if (e instanceof java.io.FileNotFoundException) {
            return "文件不存在：" + e.getMessage();
        } else if (e instanceof java.io.IOException) {
            // 检查是否是 PDF 格式错误
            Throwable cause = e.getCause();
            while (cause != null) {
                String causeName = cause.getClass().getName();
                if (causeName.contains("COSFormatException") ||
                        causeName.contains("InvalidPasswordException") ||
                        causeName.contains("CryptedPdfOperationException")) {
                    return "PDF 文件格式错误或已加密：" + e.getMessage();
                }
                cause = cause.getCause();
            }
            return "文件读取失败：" + e.getMessage();
        } else if (e instanceof org.apache.poi.openxml4j.exceptions.OpenXML4JException) {
            return "Word 文件格式错误：" + e.getMessage();
        }

        return "文档处理失败：" + e.getMessage();
    }

    /**
     * 异步处理文档（提取文本、分块、向量化）
     */
    @Async
    public void processDocumentAsync(Long docId) {
        try {
            KnowledgeBaseDocument document = knowledgeBaseDocumentDao.getById(docId);
            if (document == null) {
                log.error("文档不存在，docId: {}", docId);
                return;
            }

            // 获取所属知识库
            KnowledgeBase kb = knowledgeBaseDao.getById(document.getKbId());
            if (kb == null) {
                log.error("知识库不存在，kbId: {}", document.getKbId());
                return;
            }

            // 获取文件绝对路径
            String absolutePath = fileUploadService.getAbsoluteFilePath(document.getSourceUrl());
            if (absolutePath == null) {
                throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "文件不存在");
            }

            // 使用 Spring AI 读取文档
            List<Document> documents = readDocumentWithSpringAI(absolutePath, document.getDocType());
            if (documents.isEmpty()) {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "无法从文件中读取文档内容");
            }

            // 合并内容用于计算哈希
            String content = mergeDocumentContent(documents);

            // 计算内容哈希（用于去重）
            String contentHash = calculateContentHash(content);
            document.setContentHash(contentHash);

            // 使用 Spring AI 进行文本分块
            List<String> chunks = splitDocumentsWithSpringAI(documents, kb.getChunkSize(), kb.getChunkOverlap());
            document.setChunkCount(chunks.size());

            // 调用 Embedding 模型生成向量并存储
            try {
                List<String> vectorIds = storeVectors(chunks, kb);
                document.setVectorIds(vectorIds);
                log.info("文档向量化成功，docId: {}, 生成 {} 个向量", docId, vectorIds.size());
            } catch (Exception e) {
                log.error("文档向量化失败，docId: {}", docId, e);
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "文档向量化失败：" + e.getMessage());
            }

            // 更新文档状态
            document.setStatus(1); // 处理完成
            document.setUpdatedAt(new java.util.Date());
            
            knowledgeBaseDocumentDao.updateById(document);

            log.info("文档异步处理成功，docId: {}, chunkCount: {}", docId, chunks.size());

        } catch (Exception e) {
            log.error("文档异步处理失败，docId: {}, 错误类型：{}", docId, e.getClass().getSimpleName(), e);
            // 更新状态为失败
            KnowledgeBaseDocument document = new KnowledgeBaseDocument();
            document.setId(docId);
            document.setStatus(-1);
            document.setUpdatedAt(new java.util.Date());
            knowledgeBaseDocumentDao.updateById(document);
            
            // 记录详细的错误原因，便于排查问题
            String rootCause = getRootCauseMessage(e);
            log.error("文档处理失败原因：{}, docId: {}", rootCause, docId);
        }
    }

    /**
     * 使用 Spring AI 读取文档
     * @param filePath 文件路径
     * @param docType 文档类型
     * @return Document 列表
     */
    private List<Document> readDocumentWithSpringAI(String filePath, String docType) {
        try {
            log.debug("使用 Spring AI TextReader 读取文件：{}, 类型：{}", filePath, docType);
            
            // 创建 FileSystemResource
            FileSystemResource resource = new FileSystemResource(filePath);
            
            // 创建 TextReader 并读取文档
            TextReader textReader = new TextReader(resource);
            textReader.getCustomMetadata().put("filename", resource.getFilename());
            textReader.getCustomMetadata().put("docType", docType);
            
            List<Document> documents = textReader.read();
            
            log.info("Spring AI 读取文档成功，文件：{}, 文档数量：{}", filePath, documents.size());
            return documents;
            
        } catch (Exception e) {
            log.error("使用 Spring AI 读取文档失败：{}", filePath, e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "读取文档失败：" + e.getMessage(), e);
        }
    }

    /**
     * 合并多个 Document 的内容
     */
    private String mergeDocumentContent(List<Document> documents) {
        StringBuilder content = new StringBuilder();
        for (Document doc : documents) {
            if (StrUtil.isNotBlank(doc.getText())) {
                content.append(doc.getText()).append("\n\n");
            }
        }
        return content.toString().trim();
    }

    /**
     * 使用 Spring AI 进行文本分块
     * @param documents 原始文档列表
     * @param chunkSize 分块大小（token 数）
     * @param chunkOverlap 重叠大小（token 数）
     * @return 分块后的文本列表
     */
    private List<String> splitDocumentsWithSpringAI(List<Document> documents, int chunkSize, int chunkOverlap) {
        if (documents == null || documents.isEmpty()) {
            return Collections.emptyList();
        }
        
        try {
            log.debug("使用 Spring AI TokenTextSplitter 分块，文档数量：{}, chunkSize: {}, overlap: {}", 
                     documents.size(), chunkSize, chunkOverlap);
            
            // 使用 TokenTextSplitter 进行分块
            TokenTextSplitter splitter = new TokenTextSplitter(
                chunkSize,           // defaultChunkSize - 每个块的目标大小（token）
                Math.min(chunkSize / 2, 350),  // minChunkSizeChars - 最小字符数（默认 350）
                5,                   // minChunkLengthToEmbed - 最小嵌入长度（默认 5）
                10000,               // maxNumChunks - 最大分块数量（默认 10000）
                true                 // keepSeparator - 保留分隔符
            );
            
            List<Document> chunks = splitter.apply(documents);
            
            // 提取分块内容
            List<String> result = new ArrayList<>();
            for (Document chunk : chunks) {
                result.add(chunk.getText());
            }
            
            log.debug("Spring AI 分块成功，原始文档数：{}, 分块数量：{}", documents.size(), result.size());
            return result;
            
        } catch (Exception e) {
            log.warn("Spring AI 分块失败，回退到自定义分块方法", e);
            // 如果 Spring AI 分块失败，回退到原来的方法
            String mergedContent = mergeDocumentContent(documents);
            return splitIntoChunks(mergedContent, chunkSize, chunkOverlap);
        }
    }

    /**
     * 调用嵌入模型生成向量并存储到向量数据库
     */
    private List<String> storeVectors(List<String> chunks, KnowledgeBase kb) {
        if (chunks.isEmpty()) {
            return Collections.emptyList();
        }

        // 获取嵌入模型
        AiModel embeddingModel = aiModelDao.getById(kb.getEmbeddingModelId());
        if (embeddingModel == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR,
                    "知识库的嵌入模型不存在，kbId: " + kb.getId());
        }

        // 使用 VectorSearchService 进行向量化存储
        try {
            // 直接调用 VectorSearchService 的批量存储方法
            // Spring AI 1.1.0 会在内部处理 Document 转换和向量化
            List<String> vectorIds = vectorSearchService.storeDocuments(
                    kb.getId(),
                    chunks,
                    embeddingModel.getId()
            );

            log.debug("批量存储向量成功，kbId: {}, 存储数量：{}", kb.getId(), vectorIds.size());
            return vectorIds;

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("批量存储向量失败", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "向量存储失败：" + e.getMessage());
        }
    }

    /**
     * 文本分块（备用方法）
     */
    private List<String> splitIntoChunks(String text, int chunkSize, int chunkOverlap) {
        if (text == null || text.isEmpty()) {
            return Collections.emptyList();
        }
        
        List<String> chunks = new ArrayList<>();
        
        // 1. 先按段落分割（双换行符或多个换行）
        List<String> paragraphs = splitByParagraphs(text);
        
        // 2. 对每个段落进行处理
        for (String paragraph : paragraphs) {
            String trimmedParagraph = paragraph.trim();
            if (trimmedParagraph.isEmpty()) {
                continue;
            }
            
            // 如果段落长度小于 chunkSize，直接作为一个 chunk
            if (trimmedParagraph.length() <= chunkSize) {
                chunks.add(trimmedParagraph);
            } else {
                // 段落过长，需要进一步分割
                List<String> subChunks = splitParagraphIntoChunks(trimmedParagraph, chunkSize, chunkOverlap);
                chunks.addAll(subChunks);
            }
        }
        
        // 3. 合并过小的 chunk（可选优化）
        chunks = mergeSmallChunks(chunks, chunkSize);
        
        return chunks;
    }
    
    /**
     * 按段落分割文本
     */
    private List<String> splitByParagraphs(String text) {
        List<String> paragraphs = new ArrayList<>();
        
        // 按多个换行符分割段落
        String[] splits = text.split("\\n\\s*\\n");
        for (String split : splits) {
            if (!split.trim().isEmpty()) {
                paragraphs.add(split);
            }
        }
        
        return paragraphs;
    }
    
    /**
     * 将长段落分割成合适的块
     */
    private List<String> splitParagraphIntoChunks(String paragraph, int chunkSize, int chunkOverlap) {
        List<String> chunks = new ArrayList<>();
        int start = 0;
        
        while (start < paragraph.length()) {
            int end = Math.min(start + chunkSize, paragraph.length());
            String chunk = paragraph.substring(start, end);
            
            // 尝试在句子边界处切分
            if (end < paragraph.length()) {
                int splitPoint = findBestSplitPoint(chunk, paragraph, start, end);
                
                if (splitPoint > start && splitPoint <= end) {
                    chunk = paragraph.substring(start, splitPoint);
                    end = splitPoint;
                }
            }
            
            String trimmedChunk = chunk.trim();
            if (!trimmedChunk.isEmpty()) {
                chunks.add(trimmedChunk);
            }
            
            // 移动起始位置，考虑重叠部分
            start = end - chunkOverlap;
            
            if (start >= paragraph.length()) {
                break;
            }
        }
        
        return chunks;
    }
    
    /**
     * 寻找最佳分割点
     */
    private int findBestSplitPoint(String chunk, String fullText, int start, int end) {
        // 优先级顺序查找分割点
        
        // 1. 句号、问号、感叹号等句子结束符号
        String sentenceEndings = ".!?。！？";
        for (int i = chunk.length() - 1; i >= chunk.length() / 2; i--) {
            char c = chunk.charAt(i);
            if (sentenceEndings.indexOf(c) >= 0) {
                // 检查是否是缩写（如 Mr., Dr. 等）
                if (!isAbbreviation(chunk, i)) {
                    return start + i + 1;
                }
            }
        }
        
        // 2. 分号、冒号
        String semiColonEndings = ";:；:";
        for (int i = chunk.length() - 1; i >= chunk.length() / 2; i--) {
            char c = chunk.charAt(i);
            if (semiColonEndings.indexOf(c) >= 0) {
                return start + i + 1;
            }
        }
        
        // 3. 逗号
        String commaEndings = ",，";
        for (int i = chunk.length() - 1; i >= chunk.length() / 2; i--) {
            char c = chunk.charAt(i);
            if (commaEndings.indexOf(c) >= 0) {
                return start + i + 1;
            }
        }
        
        // 4. 空格（单词边界）
        int lastSpace = chunk.lastIndexOf(' ');
        if (lastSpace > chunk.length() / 2) {
            return start + lastSpace + 1;
        }
        
        // 5. 如果没有找到合适的分割点，返回原 end
        return end;
    }
    
    /**
     * 检查是否是缩写（如 Mr., Dr., U.S.A. 等）
     */
    private boolean isAbbreviation(String text, int position) {
        // 简单判断：如果是大写字母后跟点，且后面还有小写字母，可能是缩写
        if (position > 0 && position < text.length() - 1) {
            char beforeDot = text.charAt(position - 1);
            char afterDot = text.charAt(position + 1);
            
            // 如果点前面是大写字母，后面是小写字母或空格，可能是缩写
            return Character.isUpperCase(beforeDot) &&
                    (Character.isLowerCase(afterDot) || afterDot == ' ');
        }
        return false;
    }
    
    /**
     * 合并过小的块（优化策略）
     */
    private List<String> mergeSmallChunks(List<String> chunks, int minSize) {
        if (chunks.size() <= 1) {
            return chunks;
        }
        
        List<String> mergedChunks = new ArrayList<>();
        StringBuilder currentMerge = new StringBuilder();
        
        for (String chunk : chunks) {
            if (currentMerge.isEmpty()) {
                currentMerge.append(chunk);
            } else if (currentMerge.length() + chunk.length() + 2 <= minSize * 1.5) {
                // 如果合并后不超过最小大小的 1.5 倍，继续合并
                currentMerge.append("\n\n").append(chunk);
            } else {
                // 否则保存当前合并结果，开始新的合并
                mergedChunks.add(currentMerge.toString());
                currentMerge = new StringBuilder(chunk);
            }
        }
        
        // 添加最后一个合并块
        if (!currentMerge.isEmpty()) {
            mergedChunks.add(currentMerge.toString());
        }
        
        return mergedChunks;
    }
    
    /**
     * 计算内容哈希
     */
   private String calculateContentHash(String content) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(content.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder(2 * hash.length);
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
          log.error("计算哈希失败", e);
            return null;
        }
    }
    
    /**
     * 获取根本原因的异常消息
     */
    private String getRootCauseMessage(Throwable e) {
        Throwable rootCause = e;
        while (rootCause.getCause() != null) {
            rootCause = rootCause.getCause();
        }
        return rootCause.getClass().getSimpleName() + ": " + rootCause.getMessage();
    }
}
