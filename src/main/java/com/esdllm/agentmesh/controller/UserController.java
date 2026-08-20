package com.esdllm.agentmesh.controller;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.esdllm.agentmesh.common.BaseResponse;
import com.esdllm.agentmesh.common.ErrorCode;
import com.esdllm.agentmesh.common.ResultUtils;
import com.esdllm.agentmesh.model.dto.request.AiOptimizeRequest;
import com.esdllm.agentmesh.model.dto.request.UserLoginRequest;
import com.esdllm.agentmesh.model.dto.request.UserRegisterRequest;
import com.esdllm.agentmesh.model.dto.response.AiOptimizeResponse;
import com.esdllm.agentmesh.model.dto.response.UserResponse;
import com.esdllm.agentmesh.exception.BusinessException;
import com.esdllm.agentmesh.model.domain.User;
import com.esdllm.agentmesh.service.UserService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 用户注册登录管理接口
 */
@RestController
@RequestMapping("/user")
public class UserController {
    @Resource
    UserService userService;
    /**
     * 用户注册
     * @param request 注册请求
     * @return 注册结果
     */
    @PostMapping("/register")
    public BaseResponse<UserResponse> register(@RequestBody UserRegisterRequest request) {
        // 参数校验
        if (ObjectUtil.isEmpty(request)){
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        if (!request.getCheckPassword().equals(request.getPassword())){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"两次输入的密码不一致");
        }
        UserResponse responseUser = userService.register(request);

        return ResultUtils.success(responseUser);
    }
    /**
     * 用户登录
     * @param request 登录请求
     * @return 登录结果
     */
    @PostMapping("/login")
    public BaseResponse<UserResponse> login(@RequestBody UserLoginRequest userRequest, HttpServletRequest request) {
        // 登录校验
        if (ObjectUtil.isEmpty(userRequest)){
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        UserResponse result = userService.login(userRequest, request);

        return ResultUtils.success(result);
    }
    /**
     * 获取当前登录用户
     * @param request 请求
     * @return 当前登录用户
     */
    @GetMapping("/current")
    public BaseResponse<UserResponse> getCurrentUser(HttpServletRequest request) {
        UserResponse result = userService.getCurrentUser(request);
        return ResultUtils.success(result);
    }
    /**
     * 退出登录
     */
    @DeleteMapping("/logout")
    public BaseResponse<Integer> logout(HttpServletRequest request) {
        if (userService.getLoginUser(request.getSession()) == null) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "未登录");
        }
        request.getSession().removeAttribute(UserService.USER_LOGIN_STATUS);
        return ResultUtils.success(1);
    }
    /**
     * 更新当前用户信息
     */
    @PutMapping("/update")
    public BaseResponse<UserResponse> updateCurrentUser(@RequestBody UserRegisterRequest updateUser, HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request.getSession());;
        if (ObjectUtil.isEmpty(loginUser)) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "未登录");
        }
        UserResponse result = userService.updateCurrentUser(updateUser, request);

        return ResultUtils.success(result);
    }
    /**
     * 根据 ID 获取用户信息（管理员功能）
     */
    @GetMapping("/{userId}")
    public BaseResponse<UserResponse> getUserById(@PathVariable Long userId, HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request.getSession());
        if (loginUser == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }

        // 检查权限（管理员可以查看任何用户）
        if (!loginUser.getId().equals(userId) && loginUser.getUserRole() < 90) {
            throw new BusinessException(ErrorCode.NO_AUTH, "无权限查看其他用户信息");
        }

        UserResponse result = userService.getUserById(userId);
        return ResultUtils.success(result);
    }

    /**
     * 修改用户密码
     */
    @PutMapping("/change-password")
    public BaseResponse<Boolean> changePassword(
            @RequestParam String oldPassword,
            @RequestParam String newPassword,
            @RequestParam String checkPassword,
            HttpServletRequest request
    ) {
        if (StringUtils.isAnyEmpty(oldPassword, newPassword, checkPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数不能为空");
        }

        if (!newPassword.equals(checkPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "两次输入的密码不一致");
        }

        if (newPassword.length() < 8) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "密码长度不能小于 8 位");
        }

        User loginUser= userService.getLoginUser(request.getSession());
        Boolean result = userService.changePassword(loginUser.getId(), oldPassword, newPassword);
        return ResultUtils.success(result);
    }

    /**
     * 删除用户（管理员功能）
     */
    @DeleteMapping("/delete/{userId}")
    public BaseResponse<Boolean> deleteUser(@PathVariable Long userId, HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request.getSession());
        if (loginUser == null || loginUser.getUserRole() < 90) {
            throw new BusinessException(ErrorCode.NO_AUTH, "只有管理员才能删除用户");
        }

        Boolean result = userService.deleteUser(userId);
        return ResultUtils.success(result);
    }

    /**
     * 获取所有用户列表（管理员功能）
     */
    @GetMapping("/list")
    public BaseResponse<List<UserResponse>> getAllUsers(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            HttpServletRequest request
    ) {
        User loginUser = userService.getLoginUser(request.getSession());
        if (loginUser == null || loginUser.getUserRole() < 90) {
            throw new BusinessException(ErrorCode.NO_AUTH, "只有管理员才能查看用户列表");
        }

        List<UserResponse> userList = userService.getAllUsers(page, pageSize);
        return ResultUtils.success(userList);
    }

    /**
     * 使用 AI 辅助优化智能体描述和系统提示词
     * @param request 优化请求
     * @param httpRequest 请求
     * @return 优化建议
     */
    @PostMapping("/ai-optimize")
    public BaseResponse<AiOptimizeResponse> aiOptimize(@RequestBody AiOptimizeRequest request, HttpServletRequest httpRequest) {
        // 参数校验
        if (ObjectUtil.isEmpty(request)){
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        
        if (StrUtil.isEmpty(request.getOptimizeType())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "优化类型不能为空");
        }
        
        if (!List.of("description", "system_prompt", "both").contains(request.getOptimizeType())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "优化类型必须是 description、system_prompt 或 both");
        }
        
        User loginUser = userService.getLoginUser(httpRequest.getSession());
        if (loginUser == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }
        
        AiOptimizeResponse response = userService.aiOptimizeSuggestion(
            loginUser.getId(),
            request.getOptimizeType(),
            request.getCurrentDescription(),
            request.getCurrentSystemPrompt(),
            request.getOptimizationGoal()
        );
        
        return ResultUtils.success(response);
    }
}
