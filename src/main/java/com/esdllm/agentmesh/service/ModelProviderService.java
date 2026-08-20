package com.esdllm.agentmesh.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.esdllm.agentmesh.model.domain.ModelProvider;
import com.esdllm.agentmesh.model.dto.request.ModelProviderRequest;
import com.esdllm.agentmesh.model.dto.response.ModelProviderResponse;
import com.esdllm.agentmesh.model.domain.User;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public interface ModelProviderService {
    Long addModelProvider(ModelProviderRequest modelProviderRequest, HttpSession session);

    Boolean updateModelProvider(ModelProviderRequest modelProvider,Long id, HttpSession session);

    Boolean deleteModelProvider(Long id, HttpSession session);

    List<ModelProviderResponse> getMyModelProviderList(Long userId);

    List<ModelProviderResponse> getModelProviderList();

    List<ModelProviderResponse> getPublicModelProviderList();

    ModelProviderResponse getModelProviderById(Long id);
    
    /**
     * 获取所有模型提供商分页列表（管理员功能）
     * @param page 页码
     * @param pageSize 每页数量
     * @return 模型提供商分页数据
     */
    Page<ModelProvider> getModelProvidersPage(int page, int pageSize);
    
    /**
     * 更新模型提供商状态（管理员功能）
     * @param providerId 提供商 ID
     * @param status 状态（0=禁用，1=启用）
     * @param loginUser 登录用户
     * @return 是否成功
     */
    Boolean updateModelProviderStatus(Long providerId, Integer status, User loginUser);
}
