package com.esdllm.agentmesh.controller;

import com.esdllm.agentmesh.common.BaseResponse;
import com.esdllm.agentmesh.common.ErrorCode;
import com.esdllm.agentmesh.common.ResultUtils;
import com.esdllm.agentmesh.emun.UserRoleEnum;
import com.esdllm.agentmesh.model.domain.User;
import com.esdllm.agentmesh.model.dto.request.ModelProviderRequest;
import com.esdllm.agentmesh.model.dto.response.ModelProviderResponse;
import com.esdllm.agentmesh.service.ModelProviderService;
import com.esdllm.agentmesh.service.UserService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/model-provider")
public class ModelProviderController {
    @Resource
    private ModelProviderService modelProviderService;
    @Resource
    private UserService userService;

    /**
     * 添加模型提供商
     * @return 模型提供商 id
     */
    @PostMapping("/add")
    public BaseResponse<Long> addModelProvider(@RequestBody ModelProviderRequest modelProvider, HttpSession session) {
        User user = userService.getLoginUser(session);
        if (user == null) {
            return ResultUtils.error(ErrorCode.NOT_LOGIN_ERROR);
        }
        
        // 参数校验
        if (modelProvider == null || 
            modelProvider.getProviderName() == null || 
            modelProvider.getProviderName().trim().isEmpty()) {
            return ResultUtils.error(ErrorCode.PARAMS_ERROR, "提供商名称不能为空");
        }
        
        Long result = modelProviderService.addModelProvider(modelProvider, session);
        return ResultUtils.success(result);
    }
    @PutMapping("/update")
    public BaseResponse<Boolean> updateModelProvider(@RequestBody ModelProviderRequest modelProvider,Long id, HttpSession session) {
        User user = userService.getLoginUser(session);
        if (user == null) {
            return ResultUtils.error(ErrorCode.NOT_LOGIN_ERROR);
        }
        Boolean result = modelProviderService.updateModelProvider(modelProvider, id,session);
        return ResultUtils.success(result);
    }
    @DeleteMapping("/delete")
    public BaseResponse<Boolean> deleteModelProvider(Long id, HttpSession session) {
        User user = userService.getLoginUser(session);
        if (user == null) {
            return ResultUtils.error(ErrorCode.NOT_LOGIN_ERROR);
        }
        Boolean result = modelProviderService.deleteModelProvider(id,session);
        return ResultUtils.success(result);
    }
    @GetMapping("/my-list")
    BaseResponse<List<ModelProviderResponse>> getMyModelProviderList(HttpSession session) {
        User user = userService.getLoginUser(session);
        if (user == null) {
            return ResultUtils.error(ErrorCode.NOT_LOGIN_ERROR);
        }
        List<ModelProviderResponse> result = modelProviderService.getMyModelProviderList(user.getId());
        return ResultUtils.success(result);
    }
    @GetMapping("/list")
    BaseResponse<List<ModelProviderResponse>> getModelProviderList(HttpSession session) {
        User user = userService.getLoginUser(session);
        if (user == null) {
            return ResultUtils.error(ErrorCode.NOT_LOGIN_ERROR);
        }
        if (user.getUserRole() != UserRoleEnum.ADMIN.getCode()) {
            return ResultUtils.error(ErrorCode.NO_AUTH);
        }
        List<ModelProviderResponse> result = modelProviderService.getModelProviderList();
        return ResultUtils.success(result);
    }
    @GetMapping("/public-list")
    BaseResponse<List<ModelProviderResponse>> getPublicModelProviderList() {
        List<ModelProviderResponse> result = modelProviderService.getPublicModelProviderList();
        return ResultUtils.success(result);
    }
    @GetMapping("/info/{id}")
    BaseResponse<ModelProviderResponse> getModelProviderById(@PathVariable Long id) {
        ModelProviderResponse result = modelProviderService.getModelProviderById(id);
        return ResultUtils.success(result);
    }
}
