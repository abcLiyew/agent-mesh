package com.esdllm.agentmesh.config;

import jakarta.annotation.Resource;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web MVC配置
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {
    
    @Resource
    private SseAuthInterceptor sseAuthInterceptor;
    
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 注册SSE认证拦截器
        registry.addInterceptor(sseAuthInterceptor)
                .addPathPatterns("/api/unified-agent/execute-stream");
    }
}
