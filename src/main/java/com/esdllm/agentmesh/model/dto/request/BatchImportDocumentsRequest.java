package com.esdllm.agentmesh.model.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import jakarta.validation.constraints.NotEmpty;
import java.util.List;

/**
 * 批量导入知识库文档请求
 */
@Data
@Schema(description = "批量导入知识库文档请求")
public class BatchImportDocumentsRequest {
    
    @NotEmpty(message = "文档列表不能为空")
    @Schema(description = "文档列表", required = true)
    private List<DocumentImportRequest> documents;
    
    @Schema(description = "是否异步处理", example = "true")
    private Boolean async = true;
    
    /**
     * 单个文档导入请求
     */
    @Data
    @Schema(description = "单个文档导入信息")
    public static class DocumentImportRequest {
        
        @Schema(description = "文档名称", required = true, example = "产品手册.pdf")
        private String docName;
        
        @Schema(description = "文档类型", required = true, example = "PDF")
        private String docType;
        
        @Schema(description = "源文件 URL 或路径", example = "/uploads/2026/03/product.pdf")
        private String sourceUrl;
        
        @Schema(description = "元数据 JSON 字符串", example = "{\"author\":\"张三\",\"version\":\"1.0\"}")
        private String metadataJson;
    }
}
