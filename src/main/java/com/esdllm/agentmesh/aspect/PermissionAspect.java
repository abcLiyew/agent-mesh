package com.esdllm.agentmesh.aspect;

import com.esdllm.agentmesh.common.ErrorCode;
import com.esdllm.agentmesh.emun.PermissionType;
import com.esdllm.agentmesh.emun.ResourceVisibility;
import com.esdllm.agentmesh.exception.BusinessException;
import com.esdllm.agentmesh.model.domain.*;
import com.esdllm.agentmesh.annotation.RequirePermission;
import com.esdllm.agentmesh.model.dto.response.UserResponse;
import com.esdllm.agentmesh.repository.dao.*;
import com.esdllm.agentmesh.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.annotation.Resource;
import java.lang.reflect.Method;

/**
 * 权限检查切面
 */
@Aspect
@Component
@Slf4j
public class PermissionAspect {

    @Resource
    private UserService userService;
    
    @Resource
    private AgentDao agentDao;
    
    @Resource
    private KnowledgeBaseDao knowledgeBaseDao;
    
    @Resource
    private ToolsDao toolsDao;
    
    @Resource
    private AiModelDao aiModelDao;
    
    @Resource
    private ModelProviderDao modelProviderDao;
    
    @Resource
    private KnowledgeBaseDocumentDao knowledgeBaseDocumentDao;

    @Around("@annotation(requirePermission)")
    public Object checkPermission(ProceedingJoinPoint joinPoint, RequirePermission requirePermission) throws Throwable {
        // 获取当前登录用户（从 Session 或其他认证机制）
        User loginUser = getLoginUserFromContext();
        if (loginUser == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }

        log.info("开始权限检查，user: {}, resourceType: {}, permission: {}", 
                loginUser.getUsername(), requirePermission.resourceType(), requirePermission.permission());

        // 获取资源 ID（从方法参数中）
        Long resourceId = getResourceId(joinPoint, requirePermission.resourceIdParam());
        
        // 如果是 READ 权限且允许所有者，自动通过
        if (requirePermission.allowOwner() && requirePermission.permission() == PermissionType.READ) {
            if (resourceId != null) {
                // 检查资源是否存在并获取所有者信息
                ResourceOwner ownerInfo = getResourceOwner(requirePermission.resourceType(), resourceId);
                if (ownerInfo != null && ownerInfo.userId().equals(loginUser.getId())) {
                    log.debug("用户是资源所有者，自动通过权限检查");
                    return joinPoint.proceed();
                }
            }
        }

        // 获取资源信息
        ResourceOwner ownerInfo = getResourceOwner(requirePermission.resourceType(), resourceId);
        if (ownerInfo == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, 
                    "资源不存在：" + requirePermission.resourceType() + " - " + resourceId);
        }

        // 检查资源的可见性
        ResourceVisibility visibility = getResourceVisibility(ownerInfo);
        
        // 根据可见性和权限类型进行验证
        boolean hasPermission = checkResourcePermission(loginUser, ownerInfo, visibility, 
                requirePermission.permission(), resourceId);

        if (!hasPermission) {
            log.warn("权限不足，user: {}, resourceType: {}, resourceId: {}, requiredPermission: {}", 
                    loginUser.getUsername(), requirePermission.resourceType(), resourceId, requirePermission.permission());
            throw new BusinessException(ErrorCode.NO_AUTH, "没有操作权限");
        }

        log.info("权限检查通过");
        return joinPoint.proceed();
    }
    
    /**
     * 从安全上下文中获取用户
     */
    private User getLoginUserFromContext() {
        try {
            // 从 RequestContextHolder 中获取 HttpServletRequest
            RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
            if (requestAttributes == null) {
                // 如果没有请求上下文，尝试从当前线程获取（异步场景）
                log.warn("未找到请求上下文");
                return null;
            }
            
            HttpServletRequest request = ((ServletRequestAttributes) requestAttributes).getRequest();
            HttpSession session = request.getSession(false);
            
            if (session != null) {
                // 从 Session 中获取用户（假设用户信息存储在 session 中）
                Object userObj = session.getAttribute("loginUser");
                if (userObj instanceof User) {
                    return (User) userObj;
                }
                
                // 或者从属性中获取用户 ID，然后查询用户信息
                Object userIdObj = userService.getLoginUser(session);
            }
            
            log.warn("未找到登录用户信息");
            return null;
            
        } catch (Exception e) {
            log.error("获取登录用户失败", e);
            return null;
        }
    }
    
    /**
     * 将 UserResponse 转换为 User
     */
    private User convertToUser(UserResponse userResponse) {
        if (userResponse == null) {
            return null;
        }
        
        User user = new User();
        user.setId(userResponse.getId());
        user.setUsername(userResponse.getUsername());
        user.setEmail(userResponse.getEmail());
        user.setUserRole(Integer.valueOf(userResponse.getUserRole()));

        return user;
    }
    
    /**
     * 从方法参数中获取资源 ID
     */
    private Long getResourceId(ProceedingJoinPoint joinPoint, String paramName) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();

        // 获取参数名和参数值
        String[] paramNames = signature.getParameterNames();
        Object[] args = joinPoint.getArgs();
        
        // 查找指定的参数
        for (int i = 0; i < paramNames.length; i++) {
            if (paramNames[i].equals(paramName)) {
                Object value = args[i];
                if (value instanceof Long) {
                    return (Long) value;
                } else if (value instanceof Number) {
                    return ((Number) value).longValue();
                }
            }
        }
        
        // 如果没找到，尝试从注解中获取
        log.warn("未找到资源 ID 参数：{}", paramName);
        return null;
    }
    
    /**
     * 获取资源的所有者信息
     */
    private ResourceOwner getResourceOwner(String resourceType, Long resourceId) {
        if (resourceId == null) {
            return null;
        }
        
        switch (resourceType.toUpperCase()) {
            case "AGENT":
                Agent agent = agentDao.getById(resourceId);
                return agent != null ? new ResourceOwner(agent.getId(), agent.getUserId()) : null;
                
            case "KNOWLEDGE_BASE":
                KnowledgeBase kb = knowledgeBaseDao.getById(resourceId);
                return kb != null ? new ResourceOwner(kb.getId(), kb.getUserId()) : null;
                
            case "TOOL":
                Tools tool = toolsDao.getById(resourceId);
                return tool != null ? new ResourceOwner(tool.getId(), tool.getOwnerId()) : null;
                
            case "AI_MODEL":
                AiModel aiModel = aiModelDao.getById(resourceId);
                return aiModel != null ? new ResourceOwner(aiModel.getId(), aiModel.getUserId()) : null;
                
            case "MODEL_PROVIDER":
                ModelProvider provider = modelProviderDao.getById(resourceId);
                // ModelProvider 可能没有 userId，返回 null 表示系统级资源
                return provider != null ? new ResourceOwner(provider.getId(), null) : null;
                
            case "DOCUMENT":
            case "KNOWLEDGE_BASE_DOCUMENT":
                KnowledgeBaseDocument doc = knowledgeBaseDocumentDao.getById(resourceId);
                if (doc != null) {
                    // 文档属于知识库，需要查询知识库的用户 ID
                    KnowledgeBase kb1 = knowledgeBaseDao.getById(doc.getKbId());
                    return kb1 != null ? new ResourceOwner(doc.getId(), kb1.getUserId()) : null;
                }
                return null;
                
            default:
                log.warn("未知的资源类型：{}", resourceType);
                return null;
        }
    }
    
    /**
     * 获取资源的可见性
     */
    private ResourceVisibility getResourceVisibility(ResourceOwner ownerInfo) {
        // 这里需要根据实际的资源对象获取可见性
        // 由于 ResourceOwner 是简化对象，需要重新查询资源
        // 实际使用时应该在 getResourceOwner 中同时返回可见性信息
        
        // 简化处理：默认返回 PRIVATE
        return ResourceVisibility.PRIVATE;
    }
    
    /**
     * 检查资源权限
     */
    private boolean checkResourcePermission(User loginUser, ResourceOwner ownerInfo, 
                                           ResourceVisibility visibility, 
                                           PermissionType requiredPermission, 
                                           Long resourceId) {
        // 1. 管理员拥有所有权限
        if (isAdmin(loginUser)) {
            return true;
        }
        
        // 2. 资源所有者自动拥有所有权限
        if (ownerInfo.userId() != null && ownerInfo.userId().equals(loginUser.getId())) {
            return true;
        }
        
        // 3. 根据可见性判断
        if (visibility == ResourceVisibility.PUBLIC) {
            // 公开资源：所有人都有读权限
            return requiredPermission == PermissionType.READ;
        }
        
        // 4. 私有资源：只有所有者可访问（已在上一步检查）
        return false;
    }
    
    /**
     * 判断用户是否为管理员
     */
    private boolean isAdmin(User user) {
        // 假设 User 中有 role 字段，值为 90 或 99 表示管理员
        return user.getUserRole() != null && (user.getUserRole() == 90 || user.getUserRole() == 99);
    }

    /**
         * 资源所有者信息
         */
        private record ResourceOwner(Long resourceId, Long userId) {

    }
}
