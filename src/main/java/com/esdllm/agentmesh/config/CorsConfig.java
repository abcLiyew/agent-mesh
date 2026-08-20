package com.esdllm.agentmesh.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

/**
 * 全局跨域配置
 */
@Configuration
public class CorsConfig {

    @Bean
    public CorsFilter corsFilter() {
        CorsConfiguration config = new CorsConfiguration();
        
        // 允许的源 - 开发环境明确指定前端地址，不使用通配符
        config.addAllowedOriginPattern("http://localhost:3000");
        
        // 允许凭证信息 (cookie、authorization headers 等)
        config.setAllowCredentials(true);
        
        // 允许的请求方法
        config.addAllowedMethod("GET");
        config.addAllowedMethod("POST");
        config.addAllowedMethod("PUT");
        config.addAllowedMethod("DELETE");
        config.addAllowedMethod("OPTIONS");
        
        // 允许的请求头
        config.addAllowedHeader("*");
        
        // 暴露响应头 - 确保暴露必要的头信息
        config.addExposedHeader("Content-Type");
        config.addExposedHeader("Authorization");
        
        // 预检请求缓存时间 (秒)
        config.setMaxAge(3600L);
        
        // 配置源
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        
        return new CorsFilter(source);
    }
}
