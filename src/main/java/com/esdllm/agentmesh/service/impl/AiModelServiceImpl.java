package com.esdllm.agentmesh.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.esdllm.agentmesh.common.ErrorCode;
import com.esdllm.agentmesh.exception.BusinessException;
import com.esdllm.agentmesh.model.domain.AiModel;
import com.esdllm.agentmesh.model.domain.User;
import com.esdllm.agentmesh.model.dto.request.AiModelRequest;
import com.esdllm.agentmesh.model.dto.response.AiModelResponse;
import com.esdllm.agentmesh.repository.dao.AiModelDao;
import com.esdllm.agentmesh.service.AiModelService;
import com.esdllm.agentmesh.service.UserService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * AI 模型服务实现类
 */
@Service
@Slf4j
public class AiModelServiceImpl implements AiModelService {

    @Resource
    private AiModelDao aiModelDao;
    @Resource
    private UserService userService;

    @Transactional(rollbackFor = Exception.class)
    @Override
    public Long addAiModel(AiModelRequest request, Long userId) {
        // 1. 基础参数校验
        validateBasicParams(request);

        // 2. 转换为实体对象
        AiModel aiModel = convertToEntity(request, userId);

        // 3. 保存到数据库
        boolean saved = aiModelDao.save(aiModel);
        if (!saved) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "创建 AI 模型失败");
        }

        log.info("创建 AI 模型成功，modelId: {}, userId: {}", aiModel.getId(), userId);
        return aiModel.getId();
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public Boolean updateAiModel(AiModelRequest request, Long userId) {
        // 1. 基础参数校验
        validateBasicParams(request);
        AiModel existingModel = aiModelDao.getByModelName(request.getModelName());

        // 2. 查询模型是否存在且属于当前用户
        if (existingModel == null) {
            throw new BusinessException(ErrorCode.MODEL_NOT_FOUND, "模型不存在");
        }

        if (!existingModel.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.NO_AUTH, "无权限修改该模型");
        }

        // 3. 更新模型信息
        AiModel updateModel = convertToEntity(request, userId);
        updateModel.setId(existingModel.getId());
        
        boolean updated = aiModelDao.updateById(updateModel);
        if (!updated) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "更新 AI 模型失败");
        }

        log.info("更新 AI 模型成功，modelId: {}, userId: {}", updateModel.getId(), userId);
        return true;
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public Boolean deleteAiModel(Long modelId, Long userId) {
        // 1. 查询模型是否存在
        AiModel existingModel = aiModelDao.getById(modelId);
        if (existingModel == null) {
            throw new BusinessException(ErrorCode.MODEL_NOT_FOUND, "模型不存在");
        }

        // 2. 验证权限
        if (!existingModel.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.NO_AUTH, "无权限删除该模型");
        }

        boolean deleted = aiModelDao.removeById(modelId);
        if (!deleted) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "删除 AI 模型失败");
        }

        log.info("删除 AI 模型成功，modelId: {}, userId: {}", modelId, userId);
        return true;
    }

    @Override
    public List<AiModelResponse> getMyAiModelList(Long userId) {
        List<AiModel> modelList = aiModelDao.listByMap(
            java.util.Map.of("user_id", userId, "is_delete", 0)
        );
        
        return modelList.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<AiModelResponse> getAllAiModelList() {
        List<AiModel> modelList = aiModelDao.listByMap(
            java.util.Map.of("is_delete", 0)
        );
        
        return modelList.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<AiModelResponse> getActiveList(HttpSession session) {
        User loginUser = userService.getLoginUser(session);
        if (loginUser == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }
        
        List<AiModel> modelList = aiModelDao.listByMap(
            java.util.Map.of("is_active", true, "is_delete", 0)
        );
        
        return modelList.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public Page<AiModel> getAiModelsPage(int page, int pageSize) {
        // 查询所有 AI 模型（排除已删除的）
        return aiModelDao.lambdaQuery()
                .eq(AiModel::getIsDelete, 0)
                .orderByDesc(AiModel::getCreatedAt)
                .page(new Page<>(page, pageSize));
    }

    /**
     * 验证基础参数
     */
    private void validateBasicParams(AiModelRequest request) {
        // 提供商 ID 不能为空
        if (request.getProviderId() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "提供商 ID 不能为空");
        }

        // 模型代码名不能为空
        if (StrUtil.isBlank(request.getModelName())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "模型代码名不能为空");
        }

        // 模型代码名长度限制
        if (request.getModelName().length() < 2 || request.getModelName().length() > 100) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "模型代码名长度应在 2-100 个字符之间");
        }

        // 模型显示名不能为空
        if (StrUtil.isBlank(request.getModelDisplayName())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "模型显示名不能为空");
        }

        // 模型类型校验
        if (StrUtil.isNotBlank(request.getModelType())) {
            List<String> validTypes = Arrays.asList("CHAT", "EMBEDDING", "IMAGE");
            if (!validTypes.contains(request.getModelType().toUpperCase())) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, 
                    "无效的模型类型：" + request.getModelType() + "，有效值为：CHAT, EMBEDDING, IMAGE");
            }
        }

        // 上下文窗口校验
        if (request.getContextWindow() != null && request.getContextWindow() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "上下文窗口必须大于 0");
        }

        // 最大输出长度校验
        if (request.getMaxTokens() != null && request.getMaxTokens() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "最大输出长度必须大于 0");
        }

        // 成本校验
        if (request.getInputCostPer1k() != null && request.getInputCostPer1k().compareTo(java.math.BigDecimal.ZERO) < 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "输入成本不能为负数");
        }

        if (request.getOutputCostPer1k() != null && request.getOutputCostPer1k().compareTo(java.math.BigDecimal.ZERO) < 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "输出成本不能为负数");
        }

        // 货币单位校验
        if (StrUtil.isNotBlank(request.getCurrencyType())) {
            List<String> validCurrencies = Arrays.asList("CNY", "USD");
            if (!validCurrencies.contains(request.getCurrencyType().toUpperCase())) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, 
                    "无效的货币单位：" + request.getCurrencyType() + "，有效值为：CNY, USD");
            }
        }
    }

    /**
     * 将请求对象转换为实体对象
     */
    private AiModel convertToEntity(AiModelRequest request, Long userId) {
        AiModel aiModel = new AiModel();
        aiModel.setUserId(userId);
        aiModel.setProviderId(request.getProviderId());
        aiModel.setModelName(request.getModelName());
        aiModel.setModelDisplayName(request.getModelDisplayName());
        aiModel.setModelType(request.getModelType() != null ? request.getModelType().toUpperCase() : "CHAT");
        aiModel.setContextWindow(request.getContextWindow());
        aiModel.setMaxTokens(request.getMaxTokens());
        aiModel.setInputCostPer1k(request.getInputCostPer1k());
        aiModel.setOutputCostPer1k(request.getOutputCostPer1k());
        aiModel.setCurrencyType(request.getCurrencyType() != null ? request.getCurrencyType().toUpperCase() : "CNY");
        aiModel.setIsActive(request.getIsActive() != null ? request.getIsActive() : true);
        aiModel.setIsDelete(0);
        aiModel.setCreatedAt(new Date());
        aiModel.setUpdatedAt(new Date());

        return aiModel;
    }

    /**
     * 将实体对象转换为响应对象
     */

    private AiModelResponse convertToResponse(AiModel aiModel) {
        AiModelResponse response = new AiModelResponse();
        response.setId(aiModel.getId());
        response.setUserId(aiModel.getUserId());
        response.setProviderId(aiModel.getProviderId());
        response.setModelName(aiModel.getModelName());
        response.setModelDisplayName(aiModel.getModelDisplayName());
        response.setModelType(aiModel.getModelType());
        response.setContextWindow(aiModel.getContextWindow());
        response.setMaxTokens(aiModel.getMaxTokens());
        response.setInputCostPer1k(aiModel.getInputCostPer1k());
        response.setOutputCostPer1k(aiModel.getOutputCostPer1k());
        response.setCurrencyType(aiModel.getCurrencyType());
        response.setIsActive(aiModel.getIsActive());
        response.setCreatedAt(aiModel.getCreatedAt());
        response.setUpdatedAt(aiModel.getUpdatedAt());

        return response;
    }

    @Override
    public AiModel getDefaultChatModel(Long userId) {
        if (userId == null) {
            return null;
        }

        // 优先使用用户自己的模型，如果没有则使用公共模型(user_id=1)
        LambdaQueryWrapper<AiModel> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.and(w -> w.eq(AiModel::getUserId, userId)
                             .or()
                             .eq(AiModel::getUserId, 1))  // 公共模型
                .eq(AiModel::getModelType, "CHAT")
                .eq(AiModel::getIsActive, true)
                .eq(AiModel::getIsDelete, 0)
                .orderByDesc(AiModel::getUserId)  // 优先用户自己的模型
                .orderByAsc(AiModel::getId)
                .last("LIMIT 1");

        return aiModelDao.getOne(queryWrapper);
    }

    @Override
    public List<AiModel> getModelsByType(Long userId, String modelType) {
        if (StrUtil.isBlank(modelType)) {
            return new ArrayList<>();
        }

        // 返回用户自己的模型 + 公共模型(user_id=1)
        LambdaQueryWrapper<AiModel> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.and(w -> w.eq(AiModel::getUserId, userId)
                             .or()
                             .eq(AiModel::getUserId, 1))  // 公共模型
                .eq(AiModel::getModelType, modelType)
                .eq(AiModel::getIsActive, true)
                .eq(AiModel::getIsDelete, 0)
                .orderByDesc(AiModel::getUserId)  // 优先用户自己的模型
                .orderByDesc(AiModel::getCreatedAt);

        return aiModelDao.list(queryWrapper);
    }

    @Override
    public Boolean updateAiModelStatus(Long modelId, Integer status, com.esdllm.agentmesh.model.domain.User loginUser) {
        if (modelId == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "模型 ID 不能为空");
        }

        if (status == null || (status != 0 && status != 1)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "状态值必须为 0 或 1");
        }

        // 查询模型是否存在
        AiModel existingModel = aiModelDao.getById(modelId);
        if (existingModel == null) {
            throw new BusinessException(ErrorCode.MODEL_NOT_FOUND, "模型不存在");
        }

        // 更新模型状态
        existingModel.setIsActive(status == 1);
        existingModel.setUpdatedAt(new Date());
        
        boolean updated = aiModelDao.updateById(existingModel);
        if (!updated) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "更新模型状态失败");
        }

        log.info("更新 AI 模型状态成功，modelId: {}, status: {}, userId: {}", modelId, status, loginUser.getId());
        return true;
    }
}

