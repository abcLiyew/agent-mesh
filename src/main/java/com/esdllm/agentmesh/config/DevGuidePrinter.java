package com.esdllm.agentmesh.config;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * 开发环境快速启动指南
 * 仅在 dev 环境下激活
 */
@Configuration
@Profile("dev")
@Slf4j
public class DevGuidePrinter {

    @Value("${server.port:8080}")
  private String serverPort;

    @PostConstruct
   public void printGuide() {
      String baseUrl = "http://localhost:" + serverPort;
        
      log.info("\n" + "=".repeat(60));
      log.info("🚀 智能体决策引擎系统启动成功！");
      log.info("=".repeat(60));
      log.info("");
      log.info("📍 访问地址:");
      log.info("   Swagger API 文档：{}/swagger-ui.html", baseUrl);
      log.info("   Knife4j 文档：{}/doc.html", baseUrl);
      log.info("   健康检查：{}/api/system/health", baseUrl);
      log.info("");
      log.info("🔧 快速测试:");
      log.info("   1. 访问 Swagger UI 查看 API 文档");
      log.info("  2. 使用 /api/system/health 检查系统状态");
      log.info("   3. 创建智能体后，调用 /api/decision/chat/{agentId} 测试对话");
      log.info("");
      log.info("📊 示例数据:");
      log.info("   系统已自动初始化示例工具、模型提供商和模型数据");
      log.info("   可直接使用这些数据进行测试");
      log.info("");
      log.info("💡 提示:");
      log.info("   - 生产环境请使用 prod profile");
      log.info("   - 本地开发使用 local profile");
      log.info("=".repeat(60));
    }
}
