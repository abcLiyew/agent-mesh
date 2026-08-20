package com.esdllm.agentmesh.annotation;


import com.esdllm.agentmesh.emun.PermissionType;
import com.esdllm.agentmesh.emun.ResourceVisibility;

import java.lang.annotation.*;

/**
 * 权限检查注解
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
public @interface RequirePermission {
    
    /**
     * 资源类型（如：AGENT, KNOWLEDGE_BASE, TOOL）
     */
    String resourceType();
    
    /**
     * 资源 ID 参数名（从方法参数中获取）
     */
    String resourceIdParam() default "id";
    
    /**
     * 需要的权限类型
     */
    PermissionType permission() default PermissionType.READ;
    
    /**
     * 是否允许资源所有者自动通过
     */
    boolean allowOwner() default true;
}
