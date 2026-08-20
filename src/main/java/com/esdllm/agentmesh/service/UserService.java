package com.esdllm.agentmesh.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.esdllm.agentmesh.model.domain.User;
import com.esdllm.agentmesh.model.dto.request.UserLoginRequest;
import com.esdllm.agentmesh.model.dto.request.UserRegisterRequest;
import com.esdllm.agentmesh.model.dto.response.AiOptimizeResponse;
import com.esdllm.agentmesh.model.dto.response.UserResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public interface UserService {
    String USER_LOGIN_STATUS = "userLoginState";

    UserResponse register(UserRegisterRequest request);

    UserResponse login(UserLoginRequest userRequest, HttpServletRequest request);

    UserResponse getCurrentUser(HttpServletRequest request);

    UserResponse updateCurrentUser(UserRegisterRequest updateUser, HttpServletRequest request);

    User getLoginUser(HttpSession  session);

    /**
     * 根据 ID 获取用户信息
     * @param userId 用户 ID
     * @return 用户信息
     */
    UserResponse getUserById(Long userId);

    /**
     * 修改用户密码
     * @param userId 用户 ID
     * @param oldPassword 旧密码
     * @param newPassword 新密码
     * @return 是否成功
     */
    Boolean changePassword(Long userId, String oldPassword, String newPassword);

    /**
     * 删除用户（逻辑删除）
     * @param userId 用户 ID
     * @return 是否成功
     */
    Boolean deleteUser(Long userId);

    /**
     * 获取所有用户列表（分页）
     * @param page 页码
     * @param pageSize 每页数量
     * @return 用户列表
     */
    List<UserResponse> getAllUsers(int page, int pageSize);

    /**
     * 分页获取所有用户列表（管理员功能）
     * @param page 页码
     * @param pageSize 每页数量
     * @return 用户分页数据
     */
    Page<User> getUsersPage(int page, int pageSize);

    /**
     * 使用 AI 辅助优化智能体描述和系统提示词
     * @param userId 用户 ID
     * @param optimizeType 优化类型：description=仅优化描述，system_prompt=仅优化系统提示词，both=两者都优化
     * @param currentDescription 当前描述
     * @param currentSystemPrompt 当前系统提示词
     * @param optimizationGoal 优化目标
     * @return 优化后的建议
     */
    AiOptimizeResponse aiOptimizeSuggestion(
        Long userId, 
        String optimizeType,
        String currentDescription, 
        String currentSystemPrompt, 
        String optimizationGoal
    );

}
