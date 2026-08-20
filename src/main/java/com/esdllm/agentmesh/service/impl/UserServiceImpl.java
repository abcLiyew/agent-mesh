package com.esdllm.agentmesh.service.impl;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.BCrypt;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.esdllm.agentmesh.common.ErrorCode;
import com.esdllm.agentmesh.exception.BusinessException;
import com.esdllm.agentmesh.model.domain.AiModel;
import com.esdllm.agentmesh.model.domain.ModelProvider;
import com.esdllm.agentmesh.model.domain.User;
import com.esdllm.agentmesh.model.dto.request.UserLoginRequest;
import com.esdllm.agentmesh.model.dto.request.UserRegisterRequest;
import com.esdllm.agentmesh.model.dto.response.AiOptimizeResponse;
import com.esdllm.agentmesh.model.dto.response.UserResponse;
import com.esdllm.agentmesh.repository.dao.AiModelDao;
import com.esdllm.agentmesh.repository.dao.ModelProviderDao;
import com.esdllm.agentmesh.repository.dao.UserDao;
import com.esdllm.agentmesh.service.UserService;
import com.esdllm.agentmesh.service.agent.support.AiModelSupport;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 用户服务实现类
 */
@Service
@Slf4j
public class UserServiceImpl implements UserService {

    @Resource
    UserDao userDao;
    @Resource
    private AiModelDao aiModelDao;
    @Resource
    private AiModelSupport aiModelSupport;
    @Resource
    private ModelProviderDao modelProviderDao;

    @Override
    public UserResponse register(UserRegisterRequest request) {
        // 参数校验
        parameterValidation(ObjectUtil.isEmpty(request), request.getUsername(), request.getPassword());
        //邮箱格式验证
        if ((request.getEmail()!=null)&&!request.getEmail().matches("^[a-zA-Z0-9_-]+@[a-zA-Z0-9_-]+(\\.[a-zA-Z0-9_-]+)+$")){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"邮箱格式错误");
        }
        //账户不能包含特殊字符
        String validPattern = "[ `~!#$%^&*()+=|{}\\[\\]<>/?\\\\\"'；：，。、？]";
        Matcher matcher = Pattern.compile(validPattern).matcher(request.getUsername());
        if(matcher.find()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"账号包含特殊字符");
        }
        if (!request.getCheckPassword().equals(request.getPassword())){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"两次输入的密码不一致");
        }
        //检查账户是否已存在
        if (userDao.isExistByUsername(request.getUsername())){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"用户已存在");
        }
        //检查邮箱是否已经注册
        if (userDao.isExistByEmail(request.getEmail())){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"该邮箱已被注册");
        }
        request.setPassword(encryptPassword(request.getPassword()));
        User user = userDao.toUser(request);
        userDao.save(user);
        return userDao.toUserRegisterResponse(user);
    }

    @Override
    public UserResponse login(UserLoginRequest userRequest, jakarta.servlet.http.HttpServletRequest request) {
        // 参数校验
        parameterValidation(ObjectUtil.isEmpty(userRequest), userRequest.getUsername(), userRequest.getPassword());

        //验证密码 - 使用 DAO 层方法查询
        User user = userDao.getByUsername(userRequest.getUsername());
        if (ObjectUtil.isEmpty( user)){
            throw new BusinessException(ErrorCode.OPERATION_ERROR,"用户不存在");
        }
        if (validatePassword(userRequest.getPassword(), user.getPasswordHash())){
            throw new BusinessException(ErrorCode.OPERATION_ERROR,"密码错误");
        }
        user.setPasswordHash(null);
        request.getSession().setAttribute(USER_LOGIN_STATUS, user);
        return userDao.toUserRegisterResponse(user);
    }

    @Override
    public UserResponse getCurrentUser(jakarta.servlet.http.HttpServletRequest request) {
        User loginUser = getLoginUser(request.getSession());
        if (loginUser == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN, "用户未登录");
        }
        return userDao.toUserRegisterResponse(loginUser);
    }

    @Override
    public UserResponse updateCurrentUser(UserRegisterRequest updateUser, jakarta.servlet.http.HttpServletRequest request) {
        // 参数校验
        if (!StrUtil.isEmpty(updateUser.getUsername()) &&updateUser.getUsername().length()<4){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"用户名过短");
        }
        if (!StrUtil.isEmpty(updateUser.getEmail())&&updateUser.getEmail().length()<4){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"邮箱过短");
        }
        if (!StrUtil.isEmpty(updateUser.getEmail())&&!updateUser.getEmail().matches("^[a-zA-Z0-9_-]+@[a-zA-Z0-9_-]+(\\.[a-zA-Z0-9_-]+)+$")){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"邮箱格式错误");
        }
        if (!StrUtil.isEmpty(updateUser.getPassword())){
            if (!updateUser.getCheckPassword().equals(updateUser.getPassword())){
                throw new BusinessException(ErrorCode.PARAMS_ERROR,"两次输入的密码不一致");
            }
            if (updateUser.getPassword().length()<8){
                throw new BusinessException(ErrorCode.PARAMS_ERROR,"密码过短");
            }
            updateUser.setPassword(encryptPassword(updateUser.getPassword()));
        }
        //检查账户是否已存在
        if (updateUser.getUsername()!=null&&userDao.isExistByUsername(updateUser.getUsername())){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"用户已存在");
        }
        //检查邮箱是否已经注册
        if (updateUser.getEmail()!=null&&userDao.isExistByEmail(updateUser.getEmail())){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"该邮箱已被注册");
        }
        User user = userDao.toUser(updateUser);
        user.setId((getLoginUser(request.getSession()).getId()));
        boolean updated = userDao.updateById(user);
        if (!updated){
            throw new BusinessException(ErrorCode.OPERATION_ERROR,"更新失败");
        }
        return userDao.toUserRegisterResponse(user);
    }

    @Override
    public User getLoginUser(jakarta.servlet.http.HttpSession session) {
        return (User) session.getAttribute(USER_LOGIN_STATUS);
    }

    private void parameterValidation(boolean empty, String username, String password) {
        if (empty){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"参数为空");
        }
        if (username.length()<4){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"用户名过短");
        }
        if (password.length()<8){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"密码过短");
        }
    }

    @Override
    public UserResponse getUserById(Long userId) {
        if (userId == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户 ID 不能为空");
        }

        User user = userDao.getById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "用户不存在");
        }

        return userDao.toUserRegisterResponse(user);
    }

    @Override
    public Boolean changePassword(Long userId, String oldPassword, String newPassword) {
        if (userId == null || StrUtil.hasEmpty(oldPassword, newPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数不能为空");
        }

        // 查询用户
        User user= userDao.getById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "用户不存在");
        }

        // 验证旧密码
        if (validatePassword(oldPassword, user.getPasswordHash())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "原密码错误");
        }

        // 更新密码
        user.setPasswordHash(encryptPassword(newPassword));

        boolean updated = userDao.updateById(user);
        if (!updated) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "修改密码失败");
        }

        log.info("用户密码修改成功，userId: {}", userId);
        return true;
    }

    @Override
    public Boolean deleteUser(Long userId) {
        if (userId == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户 ID 不能为空");
        }

        // 查询用户
        User user= userDao.getById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "用户不存在");
        }

        // 使用 MyBatis-Plus 的逻辑删除（自动设置 is_delete=1）
        boolean deleted = userDao.removeById(userId);
        if (!deleted) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "删除用户失败");
        }

        log.info("用户删除成功，userId: {}", userId);
        return true;
    }

    @Override
    public List<UserResponse> getAllUsers(int page, int pageSize) {
        // 使用 DAO 层的分页查询方法
        var userPage = userDao.getActiveUsersPage(page, pageSize);

        return userPage.getRecords().stream()
                .map(userDao::toUserRegisterResponse)
                .toList();
    }

    @Override
    public Page<User> getUsersPage(int page, int pageSize) {
        return userDao.getActiveUsersPage(page, pageSize);
    }
    
    /**
     * 加密密码（使用 BCrypt）
     */
    public String encryptPassword(String password) {
        return BCrypt.hashpw(password, BCrypt.gensalt(12));
    }

    /**
     * 验证密码
     */
    public boolean validatePassword(String rawPassword, String encodedPassword) {
        return !BCrypt.checkpw(rawPassword, encodedPassword);
    }

    @Override
    public AiOptimizeResponse aiOptimizeSuggestion(Long userId, String optimizeType,
                                                   String currentDescription,
                                                   String currentSystemPrompt, String optimizationGoal) {
        // 获取系统内管理员创建的活跃模型（userId=null 或使用系统管理员 ID）
        AiModel aiModel = getSystemDefaultChatModel();
        
        if (aiModel == null) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "系统未配置可用的 AI 模型");
        }
        
        // 获取模型提供商配置
        ModelProvider provider = modelProviderDao.getById(aiModel.getProviderId());
        if (provider == null) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "模型提供商配置不存在");
        }
        
        // 根据优化类型构建不同的提示词
        String prompt = buildOptimizationPrompt(optimizeType, currentDescription, currentSystemPrompt, optimizationGoal);
        
        try {
            // 创建 ChatModel
            log.info("开始创建 ChatModel - userId: {}, optimizeType: {}, modelId: {}, providerId: {}", 
                    userId, optimizeType, aiModel.getId(), aiModel.getProviderId());
            log.info("模型信息 - modelName: {}, providerCode: {}, baseUrl: {}", 
                    aiModel.getModelName(), provider.getProviderCode(), provider.getBaseUrl());
            
            ChatModel chatModel = aiModelSupport.createChatModel(aiModel, provider);
            
            // 调用 AI 模型生成优化建议
            log.info("开始调用 AI 模型，prompt 长度：{}", prompt.length());
            ChatClient chatClient = ChatClient.builder(chatModel).build();

            String response;
            try {
                response = chatClient.prompt()
                        .user(prompt)
                        .options(buildChatOptions(aiModel))
                        .call()
                        .content();
            } catch (org.springframework.web.client.ResourceAccessException e) {
                log.error("调用 AI 服务失败 - 连接超时或网络错误", e);
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, 
                        "AI 优化失败：无法连接到 AI 服务或请求超时。请检查：<br>" +
                        "1. AI 服务是否已启动并正常运行<br>" +
                        "2. 模型是否已下载并可用<br>" +
                        "3. 网络连接是否正常，防火墙是否阻止访问<br>" +
                        "4. 服务地址配置是否正确");
            } catch (org.springframework.ai.retry.NonTransientAiException e) {
                // 处理 404 等 HTTP 错误
                if (e.getCause() instanceof HttpClientErrorException.NotFound) {
                    log.error("调用 AI 服务失败 - 404 错误：服务端点不存在", e);
                    throw new BusinessException(ErrorCode.SYSTEM_ERROR, 
                            "AI 优化失败：API 端点不存在（404）。请检查：<br>" +
                            "1. 模型提供商的 Base URL 配置是否正确<br>" +
                            "2. 使用的模型名称是否正确<br>" +
                            "3. 如果是 Ollama，请访问 http://localhost:11434/api/tags 验证服务<br>" +
                            "4. 数据库中的 provider_code 是否为 'ollama'（如果是 Ollama 服务）");
                } else {
                    log.error("调用 AI 服务失败 - 非临时性异常：{}", e.getMessage(), e);
                    throw new BusinessException(ErrorCode.SYSTEM_ERROR, 
                            "AI 优化失败：" + e.getMessage() + 
                            "。请检查模型配置和网络连接");
                }
            } catch (org.springframework.web.client.HttpClientErrorException e) {
                // 处理其他 HTTP 客户端错误
                log.error("调用 AI 服务失败 - HTTP 客户端错误：{}", e.getStatusCode(), e);
                String errorMsg;
                if (e.getStatusCode().value() == 404) {
                    errorMsg = "API 端点不存在，请检查 Base URL 和模型名称配置";
                } else if (e.getStatusCode().value() == 401) {
                    errorMsg = "API Key 无效或已过期";
                } else if (e.getStatusCode().value() == 403) {
                    errorMsg = "没有权限访问该资源，请检查 API Key 权限";
                } else {
                    errorMsg = "HTTP 错误：" + e.getStatusCode();
                }
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, 
                        "AI 优化失败：" + errorMsg);
            } catch (Exception e) {
                log.error("调用 AI 服务失败 - 未知错误", e);
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, 
                        "AI 优化失败：" + e.getMessage());
            }
            
            log.info("AI 优化建议生成成功，userId: {}, optimizeType: {}, response 长度：{}", 
                    userId, optimizeType, response != null ? response.length() : 0);
            
            // 解析 AI 返回的结果
            return parseOptimizationResponse(response, optimizeType);
            
        } catch (Exception e) {
            log.error("AI 优化建议生成失败，userId: {}, optimizeType: {}, error: {}", 
                    userId, optimizeType, e.getMessage(), e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, 
                    "AI 优化失败：" + e.getMessage() + 
                    "。请检查：1.Ollama 服务是否启动 2.模型是否存在 3.网络连接是否正常");
        }
    }
    
    /**
     * 获取系统默认聊天模型（管理员创建的公共模型）
     */
    private AiModel getSystemDefaultChatModel() {
        // 方案 1：查询系统中所有用户的第一个活跃聊天模型（通常是管理员创建的）
        LambdaQueryWrapper<AiModel> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(AiModel::getModelType, "CHAT")
                .eq(AiModel::getIsActive, true)
                .orderByAsc(AiModel::getId)
                .last("LIMIT 1");
        
        return aiModelDao.getOne(queryWrapper);
    }
    
    /**
     * 根据优化类型构建提示词
     */
    private String buildOptimizationPrompt(String optimizeType, String description, String systemPrompt, String goal) {
        StringBuilder prompt = new StringBuilder();
        
        if ("description".equals(optimizeType)) {
            // 仅优化描述
            prompt.append("""
你是一个专业的 AI 智能体描述优化专家。请帮助用户优化智能体的描述文本。

任务要求：
1. 分析当前的描述内容
2. 提供简洁、清晰、有吸引力的描述
3. 突出智能体的核心功能和价值
4. 语言简练，控制在 100-200 字以内

""");
            
            if (description != null && !description.isEmpty()) {
                prompt.append("【当前描述】\n").append(description).append("\n\n");
            } else {
                prompt.append("【当前描述】\n（暂无描述，请根据以下优化目标创建）\n\n");
            }
            
            if (goal != null && !goal.isEmpty()) {
                prompt.append("【优化目标】\n").append(goal).append("\n\n");
            }
            
            prompt.append("""
请直接输出优化后的描述文本，不需要其他解释说明。
""");
            
        } else if ("system_prompt".equals(optimizeType)) {
            // 仅优化系统提示词
            prompt.append("""
你是一个专业的 AI 智能体提示词工程师。请帮助用户优化智能体的系统提示词（System Prompt）。

任务要求：
1. 分析当前的系统提示词
2. 提供结构清晰、指令明确的系统提示词
3. 包含角色定义、任务范围、行为准则等关键要素
4. 使用专业的提示词工程最佳实践

""");
            
            if (systemPrompt != null && !systemPrompt.isEmpty()) {
                prompt.append("【当前系统提示词】\n").append(systemPrompt).append("\n\n");
            } else {
                prompt.append("【当前系统提示词】\n（暂无系统提示词，请根据以下优化目标创建）\n\n");
            }
            
            if (goal != null && !goal.isEmpty()) {
                prompt.append("【优化目标】\n").append(goal).append("\n\n");
            }
            
            prompt.append("""
请直接输出优化后的系统提示词，不需要其他解释说明。
""");
            
        } else {
            // 两者都优化
            prompt.append("""
你是一个专业的 AI 智能体配置优化助手。请帮助用户优化智能体的描述和系统提示词。

任务要求：
1. 分析当前的描述和系统提示词
2. 提供具体的优化建议
3. 给出优化后的版本
4. 解释优化的原因

""");
            
            if (description != null && !description.isEmpty()) {
                prompt.append("【当前描述】\n").append(description).append("\n\n");
            } else {
                prompt.append("【当前描述】\n（暂无描述）\n\n");
            }
            
            if (systemPrompt != null && !systemPrompt.isEmpty()) {
                prompt.append("【当前系统提示词】\n").append(systemPrompt).append("\n\n");
            } else {
                prompt.append("【当前系统提示词】\n（暂无系统提示词）\n\n");
            }
            
            if (goal != null && !goal.isEmpty()) {
                prompt.append("【优化目标】\n").append(goal).append("\n\n");
            }
            
            prompt.append("""
请按照以下格式回复：

【优化后的描述】
（这里填写优化后的完整描述）

【优化后的系统提示词】
（这里填写优化后的完整系统提示词）

【优化说明】
（简要解释主要改动和原因，100 字以内）
""");
        }
        
        return prompt.toString();
    }
    
    /**
     * 解析 AI 返回的优化结果
     */
    private AiOptimizeResponse parseOptimizationResponse(String response, String optimizeType) {
        AiOptimizeResponse result = new AiOptimizeResponse();
        
        if (response == null || response.isEmpty()) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "AI 返回结果为空");
        }
        
        if ("description".equals(optimizeType)) {
            // 仅优化描述，直接返回整个响应
            result.setOptimizedDescription(response.trim());
            result.setOptimizedSystemPrompt(null);
            result.setOptimizationExplanation(null);
            
        } else if ("system_prompt".equals(optimizeType)) {
            // 仅优化系统提示词，直接返回整个响应
            result.setOptimizedDescription(null);
            result.setOptimizedSystemPrompt(response.trim());
            result.setOptimizationExplanation(null);
            
        } else {
            // 两者都优化，需要解析响应
            String optimizedDesc = extractSection(response, "【优化后的描述】", "【优化后的系统提示词】");
            String optimizedPrompt = extractSection(response, "【优化后的系统提示词】", "【优化说明】");
            String explanation = extractSection(response, "【优化说明】", null);
            
            result.setOptimizedDescription(optimizedDesc != null ? optimizedDesc.trim() : null);
            result.setOptimizedSystemPrompt(optimizedPrompt != null ? optimizedPrompt.trim() : null);
            result.setOptimizationExplanation(explanation != null ? explanation.trim() : null);
        }
        
        return result;
    }
    
    /**
     * 从响应文本中提取指定章节的内容
     */
    private String extractSection(String text, String startMarker, String endMarker) {
        if (text == null || text.isEmpty() || startMarker == null) {
            return null;
        }
        
        int startIndex = text.indexOf(startMarker);
        if (startIndex == -1) {
            return null;
        }
        
        startIndex += startMarker.length();
        
        int endIndex = endMarker != null ? text.indexOf(endMarker, startIndex) : text.length();
        if (endIndex == -1) {
            endIndex = text.length();
        }
        
        return text.substring(startIndex, endIndex).trim();
    }
    
    /**
     * 构建 ChatOptions
     */
    private ChatOptions buildChatOptions(AiModel aiModel) {
        return org.springframework.ai.chat.prompt.ChatOptions.builder()
                .model(aiModel.getModelName())
                .temperature(0.7)
                .maxTokens(aiModel.getMaxTokens() != null ? aiModel.getMaxTokens() : 8192)
                .build();
    }

}
