package com.esdllm.agentmesh.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * SSE认证拦截器
 * 确保SSE请求能够正确携带Session Cookie
 */
@Component
@Slf4j
public class SseAuthInterceptor implements HandlerInterceptor {
    
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String uri = request.getRequestURI();
        
        // 只拦截SSE相关的请求
        if (uri.contains("/execute-stream")) {
            // 检查是否有JSESSIONID Cookie
            boolean hasSessionCookie = false;
            if (request.getCookies() != null) {
                for (var cookie : request.getCookies()) {
                    if ("JSESSIONID".equals(cookie.getName())) {
                        hasSessionCookie = true;
                        log.debug("SSE请求包含Session Cookie: {}", cookie.getValue());
                        break;
                    }
                }
            }
            
            if (!hasSessionCookie) {
                log.warn("SSE请求缺少Session Cookie，可能导致认证失败");
                // 不直接拒绝，让Controller层处理并返回友好的错误信息
            }
            
            // 设置CORS头，确保前端可以携带Cookie
            response.setHeader("Access-Control-Allow-Credentials", "true");
            response.setHeader("Access-Control-Allow-Origin", request.getHeader("Origin"));
        }
        
        return true;
    }
}
