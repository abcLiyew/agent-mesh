package com.esdllm.agentmesh;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@MapperScan("com.esdllm.agentmesh.repository.mapper")
public class AgentMeshApplication {

    public static void main(String[] args) {
        SpringApplication.run(AgentMeshApplication.class, args);
    }

}
