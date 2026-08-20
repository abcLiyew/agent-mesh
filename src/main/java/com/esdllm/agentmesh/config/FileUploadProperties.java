package com.esdllm.agentmesh.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.nio.file.Paths;

@Data
@Component
@ConfigurationProperties(prefix = "file.upload")
public class FileUploadProperties {
    
    /**
     * 文件上传根目录
     */
   private String uploadDir = "./uploads";
    
    /**
     * 单个文件最大大小（字节）
     */
   private long maxFileSize= 10 * 1024 * 1024; // 10MB
    
    /**
     * 允许上传的文件扩展名
     */
   private String[] allowedExtensions = new String[]{
        "txt", "pdf", "doc", "docx", "xls", "xlsx", 
        "md", "markdown", "csv", "json"
    };
    
    /**
     * 获取标准化的上传根目录（绝对路径）
     */
    public String getNormalizedUploadDir() {
        Path path = Paths.get(uploadDir);
        if (!path.isAbsolute()) {
            try {
                return path.toAbsolutePath().normalize().toString();
            } catch (Exception e) {
                return path.toFile().getAbsolutePath();
            }
        }
        return uploadDir;
    }
}
