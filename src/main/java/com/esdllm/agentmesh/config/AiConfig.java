package com.esdllm.agentmesh.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Duration;

@Configuration
public class AiConfig {
    @Bean
    public RestClient.Builder customRestClientBuilder(@Value("${spring.ai.dashscope.read-timeout}") Long readTimeout){
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5000);
        factory.setReadTimeout(Duration.ofMillis(readTimeout));

        return RestClient.builder()
                .requestFactory(factory);
    }

}
