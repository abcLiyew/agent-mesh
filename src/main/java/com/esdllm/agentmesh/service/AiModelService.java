package com.esdllm.agentmesh.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.esdllm.agentmesh.model.domain.AiModel;
import com.esdllm.agentmesh.model.domain.User;
import com.esdllm.agentmesh.model.dto.request.AiModelRequest;
import com.esdllm.agentmesh.model.dto.response.AiModelResponse;
import jakarta.servlet.http.HttpSession;

import java.util.List;

/**
 * AI 模型服务接口
 */
public interface AiModelService {
    
    /**
     * 添加 AI 模型
     * @param request 模型请求参数
     * @param userId 用户 ID
     * @return 模型 ID
     */
    Long addAiModel(AiModelRequest request, Long userId);
    
    /**
     * 更新 AI 模型
     * @param request 模型请求参数
     * @param userId 用户 ID
     * @return 是否成功
     */
    Boolean updateAiModel(AiModelRequest request, Long userId);
    
    /**
     * 删除 AI 模型（逻辑删除）
     * @param modelId 模型 ID
     * @param userId 用户 ID
     * @return 是否成功
     */
    Boolean deleteAiModel(Long modelId, Long userId);
    
    /**
     * 获取用户的所有 AI 模型列表
     * @param userId 用户 ID
     * @return 模型列表
     */
    List<AiModelResponse> getMyAiModelList(Long userId);
    
    /**
     * 获取所有 AI 模型列表（管理员视角）
     * @return 模型列表
     */
    List<AiModelResponse> getAllAiModelList();

    /**
     * 获取默认的聊天模型
     * @param userId 用户 ID
     * @return 聊天模型
     */
    AiModel getDefaultChatModel(Long userId);

    /**
     * 根据类型获取模型列表
     * @param userId 用户 ID
     * @param modelType 模型类型
     * @return 模型列表
     */
    List<AiModel> getModelsByType(Long userId, String modelType);

    List<AiModelResponse> getActiveList(HttpSession session);
    
    /**
     * 获取所有 AI 模型分页列表（管理员功能）
     * @param page 页码
     * @param pageSize 每页数量
     * @return 模型分页数据
     */
    Page<AiModel> getAiModelsPage(int page, int pageSize);
    
    /**
     * 更新 AI 模型状态（管理员功能）
     * @param modelId 模型 ID
     * @param status 状态（0=禁用，1=启用）
     * @param loginUser 登录用户
     * @return 是否成功
     */
    Boolean updateAiModelStatus(Long modelId, Integer status, User loginUser);
}
