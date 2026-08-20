package com.esdllm.agentmesh.service;

import com.esdllm.agentmesh.common.ErrorCode;
import com.esdllm.agentmesh.exception.BusinessException;
import com.esdllm.agentmesh.config.FileUploadProperties;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.util.Arrays;
import java.util.Date;
import java.util.UUID;

@Service
@Slf4j
public class FileUploadService {

    @Resource
   private FileUploadProperties uploadProperties;

    /**
     * 上传文件
     * @param file 上传的文件
     * @param userId 用户 ID
     * @return 文件访问 URL
     */
    public String uploadFile(MultipartFile file, Long userId) {
        // 1. 校验文件
        validateFile(file);

        // 2. 生成文件名
        String originalFilename = file.getOriginalFilename();
        String extension = getFileExtension(originalFilename);
        String fileName = UUID.randomUUID() + "_" +
                         System.currentTimeMillis() + "." + extension;

        // 3. 创建上传目录（使用标准化路径）
        String normalizedUploadDir = uploadProperties.getNormalizedUploadDir();
        String uploadPath = Paths.get(normalizedUploadDir, 
                                     String.valueOf(userId), 
                                     String.valueOf(new Date().getYear() + 1900),
                                     String.valueOf(new Date().getMonth() + 1))
                                .toAbsolutePath().normalize().toString();
        
        try {
            Files.createDirectories(Paths.get(uploadPath));
            log.info("创建上传目录成功：{}", uploadPath);
        } catch (IOException e) {
          log.error("创建上传目录失败，路径：{}", uploadPath, e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "文件上传失败");
        }

        // 4. 保存文件
        String filePath = Paths.get(uploadPath, fileName).toString();
        try {
            file.transferTo(new java.io.File(filePath));
            log.info("文件保存成功：{}, 大小：{} bytes", filePath, file.getSize());
        } catch (IOException e) {
          log.error("保存文件失败，路径：{}", filePath, e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "文件上传失败");
        }

      log.info("文件上传成功，userId: {}, fileName: {}, 保存路径：{}", userId, originalFilename, filePath);
        
        // 5. 返回访问 URL（相对路径）
        return "/files/" + userId + "/" + new Date().getYear() + 1900 + "/" + 
               (new Date().getMonth() + 1) + "/" + fileName;
    }

    /**
     * 校验文件
     */
   private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "上传文件不能为空");
        }

        // 检查文件大小
        if (file.getSize() > uploadProperties.getMaxFileSize()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, 
                "文件大小超过限制：" + uploadProperties.getMaxFileSize() / 1024 / 1024 + "MB");
        }

        // 检查文件扩展名
        String originalFilename = file.getOriginalFilename();
        String extension = getFileExtension(originalFilename);
        
        if (!isAllowedExtension(extension)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, 
                "不支持的文件类型：" + extension);
        }

        // 检查文件内容（防止恶意上传）
        if (!isValidFileContent(file)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "文件内容不合法");
        }
    }

    /**
     * 获取文件扩展名
     */
   private String getFileExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf(".") + 1).toLowerCase();
    }

    /**
     * 检查扩展名是否允许
     */
   private boolean isAllowedExtension(String extension) {
        return Arrays.stream(uploadProperties.getAllowedExtensions())
                    .anyMatch(ext -> ext.equalsIgnoreCase(extension));
    }

    /**
     * 验证文件内容（简单检查）
     */
   private boolean isValidFileContent(MultipartFile file) {
        try {
            byte[] bytes = file.getBytes();
            return bytes.length > 0;
        } catch (IOException e) {
          log.error("读取文件内容失败", e);
            return false;
        }
    }

    /**
     * 删除文件
     */
    public void deleteFile(String filePath, Long userId) {
        if (filePath == null || filePath.isEmpty()) {
            return;
        }
        if (userId == null){
            return;
        }

        try {
            Path path = Paths.get(uploadProperties.getUploadDir(), filePath.replace("/files/", ""));
            Files.deleteIfExists(path);
          log.info("文件删除成功，filePath: {}", filePath);
        } catch (IOException e) {
          log.error("删除文件失败，filePath: {}", filePath, e);
        }
    }

    /**
     * 获取文件的绝对路径
     */
    public String getAbsoluteFilePath(String relativePath) {
        if (relativePath == null || relativePath.isEmpty()) {
            log.warn("文件路径为空");
            return null;
        }
        
        if (!relativePath.startsWith("/files/")) {
            log.warn("文件路径格式不正确：{}", relativePath);
            return null;
        }
        
        // 使用标准化的上传目录
        String normalizedUploadDir = uploadProperties.getNormalizedUploadDir();
        log.debug("标准化上传目录：{}", normalizedUploadDir);
        
        String absolutePath = Paths.get(normalizedUploadDir, 
                        relativePath.replace("/files/", ""))
                   .toAbsolutePath().normalize().toString();
        
        log.debug("文件相对路径：{}, 转换后的绝对路径：{}", relativePath, absolutePath);
        
        // 检查文件是否存在
        File file = new File(absolutePath);
        if (!file.exists()) {
            log.error("文件不存在！相对路径：{}, 绝对路径：{}, 标准化上传目录：{}", 
                     relativePath, absolutePath, normalizedUploadDir);
            
            // 尝试查找可能的文件位置（处理年份目录错误的情况）
            String path = relativePath.replace("/files/", "");
            String[] pathParts = path.split("/");
            if (pathParts.length >= 3) {
                String userId = pathParts[0];
                String yearOrWrongValue = pathParts[1];
                String month = pathParts[2];
                String fileName = pathParts.length > 3 ? pathParts[3] : null;
                
                // 如果第二部分不是合理的年份（1900-2100），可能是旧数据的 bug
                try {
                    int yearValue = Integer.parseInt(yearOrWrongValue);
                    if (yearValue < 1900 || yearValue > 2100) {
                        // 这可能是毫秒时间戳或其他错误值，尝试使用正确的年份
                        String correctYear = String.valueOf(new Date().getYear() + 1900);
                        String alternativePath = Paths.get(normalizedUploadDir, 
                                                        userId, correctYear, month, fileName)
                                                   .toAbsolutePath().normalize().toString();
                        File alternativeFile = new File(alternativePath);
                        if (alternativeFile.exists()) {
                            log.info("找到替代文件路径：{}", alternativePath);
                            return alternativePath;
                        }
                    }
                } catch (NumberFormatException e) {
                    // 不是数字，忽略
                }
            }
            
            log.info("尝试列出目录内容以诊断问题...");
            try {
                File parentDir = file.getParentFile();
                if (parentDir != null && parentDir.exists()) {
                    File[] files = parentDir.listFiles();
                    if (files != null && files.length > 0) {
                        log.info("父目录 {} 中存在 {} 个文件:", parentDir.getAbsolutePath(), files.length);
                        for (File f : files) {
                            log.info("  - {}", f.getName());
                        }
                    } else {
                        log.info("父目录 {} 为空", parentDir.getAbsolutePath());
                    }
                } else {
                    log.info("父目录不存在");
                }
            } catch (Exception e) {
                log.error("列出目录内容失败", e);
            }
        } else {
            log.info("文件存在，大小：{} bytes", file.length());
        }
        
        return absolutePath;
    }

    /**
     * 获取文件上传目录
     */
    public String getUploadDir() {
        return uploadProperties.getUploadDir();
    }

}
