package com.esdllm.agentmesh.controller;

import com.esdllm.agentmesh.common.BaseResponse;
import com.esdllm.agentmesh.common.ResultUtils;
import com.esdllm.agentmesh.service.VectorSearchService;
import com.esdllm.agentmesh.repository.dao.AiModelDao;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.annotation.Resource;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 系统健康检查 Controller
 */
@RestController
@RequestMapping("/api/system")
@Slf4j
public class SystemHealthController {

    @Value("${spring.application.name:agent-mesh}")
  private String applicationName;

    @Resource
    private JdbcTemplate jdbcTemplate;

    @Resource
    private AiModelDao aiModelDao;

    /**
     * 健康检查信息
     */
  @Data
   public static class HealthInfo {
     private String status;
     private String applicationName;
     private LocalDateTime timestamp;
     private Map<String, String> components;
    }

    /**
     * 系统健康检查
     */
    @GetMapping("/health")
   public BaseResponse<HealthInfo> getHealth() {
     log.info("收到健康检查请求");
        
       HealthInfo healthInfo = new HealthInfo();
        healthInfo.setStatus("UP");
        healthInfo.setApplicationName(applicationName);
        healthInfo.setTimestamp(LocalDateTime.now());
        
       Map<String, String> components = new HashMap<>();
        components.put("database", checkDatabaseHealth());
        components.put("vector-store", checkVectorStoreHealth());
        components.put("model-api", checkModelApiHealth());
        
        healthInfo.setComponents(components);
        
        boolean allHealthy = components.values().stream().allMatch("UP"::equals);
        healthInfo.setStatus(allHealthy ? "UP" : "DEGRADED");
        
        return ResultUtils.success(healthInfo);
    }

    /**
     * 检查数据库健康状态
     */
    private String checkDatabaseHealth() {
        try {
            jdbcTemplate.execute("SELECT 1");
            return "UP";
        } catch (Exception e) {
            log.error("数据库健康检查失败", e);
            return "DOWN";
        }
    }

    /**
     * 检查向量库健康状态
     */
    private String checkVectorStoreHealth() {
        try {
            jdbcTemplate.execute("SELECT COUNT(*) FROM information_schema.tables WHERE table_name LIKE 'kb_%_vectors'");
            return "UP";
        } catch (Exception e) {
            log.error("向量库健康检查失败", e);
            return "DOWN";
        }
    }

    /**
     * 检查模型 API 健康状态
     */
    private String checkModelApiHealth() {
        try {
            aiModelDao.count();
            return "UP";
        } catch (Exception e) {
            log.error("模型 API 健康检查失败", e);
            return "DOWN";
        }
    }

    /**
     * 系统信息
     */
    @GetMapping("/info")
   public BaseResponse<Map<String, String>> getSystemInfo() {
       Map<String, String> info = new HashMap<>();
        info.put("application", applicationName);
        info.put("version", "1.0.0");
        info.put("description", "智能体决策引擎系统");
        info.put("javaVersion", System.getProperty("java.version"));
        info.put("serverTime", LocalDateTime.now().toString());
        
        return ResultUtils.success(info);
    }
}
