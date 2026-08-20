package com.esdllm.agentmesh.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.esdllm.agentmesh.common.ErrorCode;
import cn.hutool.core.util.StrUtil;
import com.esdllm.agentmesh.emun.UserRoleEnum;
import com.esdllm.agentmesh.exception.BusinessException;
import com.esdllm.agentmesh.model.domain.ModelProvider;
import com.esdllm.agentmesh.model.domain.User;
import com.esdllm.agentmesh.model.dto.request.ModelProviderRequest;
import com.esdllm.agentmesh.model.dto.response.ModelProviderResponse;
import com.esdllm.agentmesh.repository.dao.ModelProviderDao;
import com.esdllm.agentmesh.repository.mapper.ModelProviderMapper;
import com.esdllm.agentmesh.service.ModelProviderService;
import com.esdllm.agentmesh.service.UserService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
public class ModelProviderServiceImpl implements ModelProviderService {
    @Resource
    private UserService userService;
    @Resource
    private ModelProviderMapper modelProviderMapper;
    @Resource
    private ModelProviderDao modelProviderDao;
    @Override
    public Long addModelProvider(ModelProviderRequest modelProviderRequest, HttpSession session) {
        // 参数校验
        if (modelProviderRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求参数不能为空");
        }
        
        if (StrUtil.isBlank(modelProviderRequest.getProviderName())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "提供商名称不能为空");
        }
        
        if (StrUtil.isBlank(modelProviderRequest.getProviderCode())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "提供商代码不能为空");
        }
        
        if (StrUtil.isBlank(modelProviderRequest.getBaseUrl())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "API 基础地址不能为空");
        }
        
        // API Key 校验：Ollama 不需要 API Key，其他提供商需要
        if (!"ollama".equalsIgnoreCase(modelProviderRequest.getProviderCode()) 
            && StrUtil.isBlank(modelProviderRequest.getApiKeyEncrypted())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "API Key 不能为空");
        }
        
        User loginUser = userService.getLoginUser(session);
        ModelProvider modelProvider = new ModelProvider();

        modelProvider.setUserId(loginUser.getId());
        copyTypeToModelProvider(modelProviderRequest, modelProvider);

        int insert = modelProviderMapper.insert(modelProvider);
        if (insert < 1){
            throw new RuntimeException("添加模型提供商失败");
        }
        return modelProvider.getId();

    }

    private static void copyTypeToModelProvider(ModelProviderRequest modelProviderRequest, ModelProvider modelProvider) {
        modelProvider.setProviderName(modelProviderRequest.getProviderName());
        modelProvider.setProviderCode(modelProviderRequest.getProviderCode());
        modelProvider.setBaseUrl(modelProviderRequest.getBaseUrl());
        modelProvider.setApiKeyEncrypted(modelProviderRequest.getApiKeyEncrypted());
        modelProvider.setApiSecretEncrypted(modelProviderRequest.getApiSecretEncrypted());
        modelProvider.setStatus(modelProviderRequest.getStatus());
    }

    @Override
    public Boolean updateModelProvider(ModelProviderRequest modelProvider,Long id, HttpSession session) {
        //登录检查
        User loginUser = userService.getLoginUser(session);
        if (loginUser== null){
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }
        //权限检查,仅管理员或创建者可修改
        ModelProvider modelProviderById = modelProviderDao.getModelProviderById(id);
        if ((UserRoleEnum.ADMIN.getCode() != loginUser.getUserRole()|| loginUser.getUserRole() != UserRoleEnum.SUPER_ADMIN.getCode())
                && !modelProviderById.getUserId().equals(loginUser.getId())){
            throw new BusinessException(ErrorCode.NO_AUTH);
        }
        copyTypeToModelProvider(modelProvider, modelProviderById);

        return modelProviderMapper.updateById(modelProviderById) > 0;
    }

    @Override
    public Boolean deleteModelProvider(Long id, HttpSession session) {
        //登录检查
        User loginUser = userService.getLoginUser(session);
        if (loginUser== null){
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }
        //权限检查，仅管理员或创建者可修改
        ModelProvider modelProviderById = modelProviderDao.getModelProviderById(id);
        if ((UserRoleEnum.ADMIN.getCode() != loginUser.getUserRole()|| loginUser.getUserRole() != UserRoleEnum.SUPER_ADMIN.getCode())
                && !modelProviderById.getUserId().equals(loginUser.getId())){
            throw new BusinessException(ErrorCode.NO_AUTH);
        }

        return modelProviderMapper.deleteById(id) > 0;
    }

    @Override
    public Page<ModelProvider> getModelProvidersPage(int page, int pageSize) {
        // 查询所有模型提供商（排除已删除的）
        return modelProviderDao.lambdaQuery()
                .eq(ModelProvider::getIsDelete, 0)
                .orderByDesc(ModelProvider::getCreatedAt)
                .page(new Page<>(page, pageSize));
    }

    @Override
    public Boolean updateModelProviderStatus(Long providerId, Integer status, User loginUser) {
        if (providerId == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "提供商 ID 不能为空");
        }

        if (status == null || (status != 0 && status != 1)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "状态值必须为 0 或 1");
        }

        // 查询提供商是否存在
        ModelProvider existingProvider = modelProviderDao.getById(providerId);
        if (existingProvider == null) {
            throw new BusinessException(ErrorCode.MODEL_NOT_FOUND, "模型提供商不存在");
        }

        // 更新提供商状态
        existingProvider.setStatus(status);
        existingProvider.setUpdatedAt(new java.util.Date());
        
        boolean updated = modelProviderDao.updateById(existingProvider);
        if (!updated) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "更新提供商状态失败");
        }

        log.info("更新模型提供商状态成功，providerId: {}, status: {}, userId: {}", providerId, status, loginUser.getId());
        return true;
    }

    @Override
    public List<ModelProviderResponse> getMyModelProviderList(Long userId) {
        List<ModelProvider> modelProviderList =  modelProviderDao.getModelProviderListByUserId(userId);
        return modelProviderList.stream().map(modelProvider -> {
            ModelProviderResponse modelProviderResponse = new ModelProviderResponse();
            copyModelProviderToModelProviderResponse(modelProvider, modelProviderResponse);
            return modelProviderResponse;
        }).toList();
    }

    @Override
    public List<ModelProviderResponse> getModelProviderList() {
        List<ModelProvider> modelProviderList = modelProviderDao.list();
        return modelProviderList.stream().map(modelProvider -> {
            ModelProviderResponse modelProviderResponse = new ModelProviderResponse();
            copyModelProviderToModelProviderResponse(modelProvider, modelProviderResponse);
            return modelProviderResponse;
        }).toList();
    }

    @Override
    public List<ModelProviderResponse> getPublicModelProviderList() {
        List<ModelProvider> modelProviderList = modelProviderDao.publicList();
        return modelProviderList.stream().map(modelProvider -> {
            ModelProviderResponse modelProviderResponse = new ModelProviderResponse();
            copyModelProviderToModelProviderResponse(modelProvider, modelProviderResponse);
            return modelProviderResponse;
        }).toList();
    }

    @Override
    public ModelProviderResponse getModelProviderById(Long id) {
        ModelProvider modelProvider = modelProviderDao.getModelProviderById(id);
        ModelProviderResponse modelProviderResponse = new ModelProviderResponse();
        copyModelProviderToModelProviderResponse(modelProvider, modelProviderResponse);
        return modelProviderResponse;
    }

    private static void copyModelProviderToModelProviderResponse(ModelProvider modelProvider,ModelProviderResponse modelProviderResponse) {

        modelProviderResponse.setId(modelProvider.getId());
        modelProviderResponse.setProviderName(modelProvider.getProviderName());
        modelProviderResponse.setProviderCode(modelProvider.getProviderCode());
        modelProviderResponse.setBaseUrl(modelProvider.getBaseUrl());
        modelProviderResponse.setStatus(modelProvider.getStatus());
    }
}
