package com.esdllm.agentmesh.controller;

import com.esdllm.agentmesh.common.BaseResponse;
import com.esdllm.agentmesh.common.ResultUtils;
import com.esdllm.agentmesh.exception.BusinessException;
import com.esdllm.agentmesh.model.domain.User;
import com.esdllm.agentmesh.model.dto.request.AiModelRequest;
import com.esdllm.agentmesh.model.dto.response.AiModelResponse;
import com.esdllm.agentmesh.service.AiModelService;
import com.esdllm.agentmesh.service.UserService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/ai-model")
public class AiModelController {
    
    @Resource
    private AiModelService aiModelService;
    
    @Resource
    private UserService userService;

    @PostMapping("/add")
    public BaseResponse<Long> addAiModel(@RequestBody AiModelRequest aiModelRequest, HttpSession session) {
        User loginUser = getLoginUser(session);
        Long modelId = aiModelService.addAiModel(aiModelRequest, loginUser.getId());
        return ResultUtils.success(modelId);
    }
    
    @PutMapping("/update")
    public BaseResponse<Boolean> updateAiModel(@RequestBody AiModelRequest aiModelRequest, HttpSession session) {
        User loginUser = getLoginUser(session);
        Boolean result = aiModelService.updateAiModel(aiModelRequest, loginUser.getId());
        return ResultUtils.success(result);
    }
    
    @DeleteMapping("/delete/{modelId}")
    public BaseResponse<Boolean> deleteAiModel(@PathVariable Long modelId, HttpSession session) {
        User loginUser = getLoginUser(session);
        Boolean result = aiModelService.deleteAiModel(modelId, loginUser.getId());
        return ResultUtils.success(result);
    }

    @GetMapping("/my-list")
    public BaseResponse<List<AiModelResponse>> getMyAiModelList(HttpSession session) {
        User loginUser = getLoginUser(session);
        List<AiModelResponse> modelList = aiModelService.getMyAiModelList(loginUser.getId());
        return ResultUtils.success(modelList);
    }
    
    @GetMapping("/list")
    public BaseResponse<List<AiModelResponse>> getAiModelList(HttpSession session) {
        List<AiModelResponse> modelList = aiModelService.getAllAiModelList();
        return ResultUtils.success(modelList);
    }
    @GetMapping("/active-list")
    public BaseResponse<List<AiModelResponse>> getActiveAiModelList(HttpSession  session) {
        List<AiModelResponse> modelList = aiModelService.getActiveList(session);
        return ResultUtils.success(modelList);
    }
    
    /**
     * 获取当前登录用户
     */
    private User getLoginUser(HttpSession session) {
        User userObj = userService.getLoginUser(session);
        if (userObj == null) {
            throw new BusinessException(com.esdllm.agentmesh.common.ErrorCode.NOT_LOGIN_ERROR);
        }
        return userObj;
    }
}
