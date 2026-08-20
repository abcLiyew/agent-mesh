package com.esdllm.agentmesh.controller;

import com.esdllm.agentmesh.common.BaseResponse;
import com.esdllm.agentmesh.common.ErrorCode;
import com.esdllm.agentmesh.common.ResultUtils;
import com.esdllm.agentmesh.model.domain.AgentLongTermMemory;
import com.esdllm.agentmesh.model.domain.User;
import com.esdllm.agentmesh.service.UserService;
import com.esdllm.agentmesh.service.unified.LongTermMemoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 长期记忆管理控制器
 */
@RestController
@RequestMapping("/api/memory")
@Tag(name = "长期记忆管理", description = "智能体长期记忆的存储、检索和管理")
@Slf4j
public class LongTermMemoryController {
    
    @Resource
    private LongTermMemoryService longTermMemoryService;
    
    @Resource
    private UserService userService;
    
    /**
     * 存储记忆
     */
    @PostMapping("/store")
    @Operation(summary = "存储记忆", description = "手动存储一条长期记忆")
    public BaseResponse<Long> storeMemory(
            @RequestBody AgentLongTermMemory memory,
            HttpSession session) {
        
        User loginUser = userService.getLoginUser(session);
        if (loginUser == null) {
            return ResultUtils.error(ErrorCode.NOT_LOGIN);
        }
        
        // 从Session获取userId
        memory.setUserId(loginUser.getId());
        
        log.info("存储记忆请求，userId: {}, memoryType: {}", 
                loginUser.getId(), memory.getMemoryType());
        
        try {
            Long memoryId = longTermMemoryService.storeMemory(memory);
            return ResultUtils.success(memoryId);
        } catch (Exception e) {
            log.error("存储记忆失败", e);
            return ResultUtils.error(ErrorCode.SYSTEM_ERROR, "存储记忆失败: " + e.getMessage());
        }
    }
    
    /**
     * 检索记忆（关键词匹配）
     */
    @GetMapping("/retrieve")
    @Operation(summary = "检索记忆", description = "基于关键词检索相关记忆")
    public BaseResponse<List<AgentLongTermMemory>> retrieveMemories(
            @Parameter(description = "智能体ID（可选）") @RequestParam(required = false) Long agentId,
            @Parameter(description = "查询文本（可选）") @RequestParam(required = false) String query,
            @Parameter(description = "记忆类型列表（可选，多个用逗号分隔）") @RequestParam(required = false) String memoryTypes,
            @Parameter(description = "返回数量限制") @RequestParam(defaultValue = "5") int limit,
            HttpSession session) {
        
        User loginUser = userService.getLoginUser(session);
        if (loginUser == null) {
            return ResultUtils.error(ErrorCode.NOT_LOGIN);
        }
        
        log.info("检索记忆请求，userId: {}, agentId: {}, query: {}", 
                loginUser.getId(), agentId, query);
        
        try {
            // 解析记忆类型
            List<String> typeList = null;
            if (memoryTypes != null && !memoryTypes.isEmpty()) {
                typeList = List.of(memoryTypes.split(","));
            }
            
            List<AgentLongTermMemory> memories = longTermMemoryService.retrieveMemories(
                loginUser.getId(), agentId, query, typeList, limit
            );
            
            return ResultUtils.success(memories);
        } catch (Exception e) {
            log.error("检索记忆失败", e);
            return ResultUtils.error(ErrorCode.SYSTEM_ERROR, "检索记忆失败: " + e.getMessage());
        }
    }
    
    /**
     * 向量相似度检索记忆
     */
    @PostMapping("/retrieve-by-similarity")
    @Operation(summary = "向量相似度检索", description = "基于向量相似度检索最相关的记忆")
    public BaseResponse<List<AgentLongTermMemory>> retrieveMemoriesBySimilarity(
            @Parameter(description = "智能体ID（可选）") @RequestParam(required = false) Long agentId,
            @RequestBody Map<String, Object> request,
            HttpSession session) {
        
        User loginUser = userService.getLoginUser(session);
        if (loginUser == null) {
            return ResultUtils.error(ErrorCode.NOT_LOGIN);
        }
        
        log.info("向量相似度检索记忆请求，userId: {}, agentId: {}", 
                loginUser.getId(), agentId);
        
        try {
            // 提取请求参数
            float[] embedding = extractEmbedding(request.get("embedding"));
            double similarityThreshold = request.containsKey("similarityThreshold") 
                ? ((Number) request.get("similarityThreshold")).doubleValue() 
                : 0.7;
            int limit = request.containsKey("limit") 
                ? ((Number) request.get("limit")).intValue() 
                : 5;
            
            List<AgentLongTermMemory> memories = longTermMemoryService.retrieveMemoriesBySimilarity(
                loginUser.getId(), agentId, embedding, similarityThreshold, limit
            );
            
            return ResultUtils.success(memories);
        } catch (Exception e) {
            log.error("向量相似度检索记忆失败", e);
            return ResultUtils.error(ErrorCode.SYSTEM_ERROR, "检索失败: " + e.getMessage());
        }
    }
    
    /**
     * 更新记忆
     */
    @PutMapping("/{memoryId}")
    @Operation(summary = "更新记忆", description = "更新记忆内容和元数据")
    public BaseResponse<Boolean> updateMemory(
            @PathVariable Long memoryId,
            @RequestBody Map<String, Object> request,
            HttpSession session) {
        
        User loginUser = userService.getLoginUser(session);
        if (loginUser == null) {
            return ResultUtils.error(ErrorCode.NOT_LOGIN);
        }
        
        log.info("更新记忆请求，memoryId: {}, userId: {}", memoryId, loginUser.getId());
        
        try {
            String content = (String) request.get("content");
            @SuppressWarnings("unchecked")
            Map<String, Object> metadata = (Map<String, Object>) request.get("metadata");
            
            longTermMemoryService.updateMemory(memoryId, content, metadata);
            return ResultUtils.success(true);
        } catch (Exception e) {
            log.error("更新记忆失败", e);
            return ResultUtils.error(ErrorCode.SYSTEM_ERROR, "更新记忆失败: " + e.getMessage());
        }
    }
    
    /**
     * 删除记忆（软删除）
     */
    @DeleteMapping("/{memoryId}")
    @Operation(summary = "删除记忆", description = "软删除记忆")
    public BaseResponse<Boolean> deleteMemory(
            @PathVariable Long memoryId,
            HttpSession session) {
        
        User loginUser = userService.getLoginUser(session);
        if (loginUser == null) {
            return ResultUtils.error(ErrorCode.NOT_LOGIN);
        }
        
        log.info("删除记忆请求，memoryId: {}, userId: {}", memoryId, loginUser.getId());
        
        try {
            longTermMemoryService.deleteMemory(memoryId);
            return ResultUtils.success(true);
        } catch (Exception e) {
            log.error("删除记忆失败", e);
            return ResultUtils.error(ErrorCode.SYSTEM_ERROR, "删除记忆失败: " + e.getMessage());
        }
    }
    
    /**
     * 记录记忆访问
     */
    @PostMapping("/{memoryId}/access")
    @Operation(summary = "记录记忆访问", description = "记录记忆被访问，用于统计和优化")
    public BaseResponse<Boolean> recordMemoryAccess(
            @PathVariable Long memoryId) {
        
        log.info("记录记忆访问，memoryId: {}", memoryId);
        
        try {
            longTermMemoryService.recordMemoryAccess(memoryId);
            return ResultUtils.success(true);
        } catch (Exception e) {
            log.error("记录记忆访问失败", e);
            return ResultUtils.error(ErrorCode.SYSTEM_ERROR, "记录访问失败: " + e.getMessage());
        }
    }
    
    /**
     * 从对话中提取并存储记忆
     */
    @PostMapping("/extract-from-conversation")
    @Operation(summary = "从对话提取记忆", description = "使用AI从对话内容中自动提取关键记忆")
    public BaseResponse<Map<String, Object>> extractFromConversation(
            @Parameter(description = "智能体ID") @RequestParam Long agentId,
            @RequestBody Map<String, Object> request,
            HttpSession session) {
        
        User loginUser = userService.getLoginUser(session);
        if (loginUser == null) {
            return ResultUtils.error(ErrorCode.NOT_LOGIN);
        }
        
        log.info("从对话提取记忆请求，userId: {}, agentId: {}", 
                loginUser.getId(), agentId);
        
        try {
            String conversationContent = (String) request.get("conversationContent");
            Object decisionPath = request.get("decisionPath");
            
            List<Long> memoryIds = longTermMemoryService.extractAndStoreMemoriesFromConversation(
                loginUser.getId(), agentId, conversationContent, decisionPath
            );
            
            Map<String, Object> response = new HashMap<>();
            response.put("extractedMemories", memoryIds.size());
            response.put("storedMemories", memoryIds);
            
            return ResultUtils.success(response);
        } catch (Exception e) {
            log.error("从对话提取记忆失败", e);
            return ResultUtils.error(ErrorCode.SYSTEM_ERROR, "提取记忆失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取用户画像
     */
    @GetMapping("/user-profile")
    @Operation(summary = "获取用户画像", description = "基于历史记忆聚合生成用户画像")
    public BaseResponse<Map<String, Object>> getUserProfile(HttpSession session) {
        
        User loginUser = userService.getLoginUser(session);
        if (loginUser == null) {
            return ResultUtils.error(ErrorCode.NOT_LOGIN);
        }
        
        log.info("获取用户画像请求，userId: {}", loginUser.getId());
        
        try {
            Map<String, Object> profile = longTermMemoryService.getUserProfile(loginUser.getId());
            return ResultUtils.success(profile);
        } catch (Exception e) {
            log.error("获取用户画像失败", e);
            return ResultUtils.error(ErrorCode.SYSTEM_ERROR, "获取用户画像失败: " + e.getMessage());
        }
    }
    
    /**
     * 清理过期记忆（管理员接口）
     */
    @PostMapping("/cleanup-expired")
    @Operation(summary = "清理过期记忆", description = "清理已过期的记忆（定时任务也会自动执行）")
    public BaseResponse<Integer> cleanupExpiredMemories(HttpSession session) {
        
        User loginUser = userService.getLoginUser(session);
        if (loginUser == null) {
            return ResultUtils.error(ErrorCode.NOT_LOGIN);
        }
        
        // TODO: 添加管理员权限验证
        
        log.info("清理过期记忆请求，userId: {}", loginUser.getId());
        
        try {
            int count = longTermMemoryService.cleanupExpiredMemories();
            return ResultUtils.success(count);
        } catch (Exception e) {
            log.error("清理过期记忆失败", e);
            return ResultUtils.error(ErrorCode.SYSTEM_ERROR, "清理失败: " + e.getMessage());
        }
    }
    
    /**
     * 辅助方法：从Object提取float数组
     */
    @SuppressWarnings("unchecked")
    private float[] extractEmbedding(Object embeddingObj) {
        if (embeddingObj instanceof List) {
            List<?> list = (List<?>) embeddingObj;
            float[] result = new float[list.size()];
            for (int i = 0; i < list.size(); i++) {
                result[i] = ((Number) list.get(i)).floatValue();
            }
            return result;
        }
        throw new IllegalArgumentException("Invalid embedding format");
    }
}
