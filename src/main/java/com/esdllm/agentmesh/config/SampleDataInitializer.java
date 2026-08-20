package com.esdllm.agentmesh.config;

import com.esdllm.agentmesh.model.domain.AiModel;
import com.esdllm.agentmesh.model.domain.ModelProvider;
import com.esdllm.agentmesh.model.domain.Tools;
import com.esdllm.agentmesh.repository.dao.AiModelDao;
import com.esdllm.agentmesh.repository.dao.ModelProviderDao;
import com.esdllm.agentmesh.repository.dao.ToolsDao;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

/**
 * 示例数据初始化配置
 * 用于开发环境快速搭建测试数据
 */
@Configuration
@Slf4j
public class SampleDataInitializer {

    @Resource
  private ToolsDao toolsDao;

    @Resource
  private ModelProviderDao modelProviderDao;

    @Resource
  private AiModelDao aiModelDao;

    /**
     * 创建日期计算工具的输入 Schema
     */
  private Object createDateCalculatorSchema() {
      String schema = """
            {
              "type": "object",
              "properties": {
                "operation": {
                  "type": "string",
                  "enum": ["add", "subtract", "diff"],
                  "description": "操作类型：add-加法，subtract-减法，diff-计算差值"
                },
                "date": {
                  "type": "string",
                  "format": "date",
                  "description": "基准日期，格式：yyyy-MM-dd"
                },
                "days": {
                  "type": "integer",
                  "description": "天数"
                }
              },
              "required": ["operation", "date"]
            }
            """;
        return schema;
    }

    @PostConstruct
    public void init() {
        log.info("开始初始化示例数据...");

        try {
            // 初始化示例工具
            initSampleTools();

            // 初始化示例模型提供商
            initSampleProviders();

            // 初始化示例模型
            initSampleModels();

            log.info("示例数据初始化完成");
        } catch (Exception e) {
            log.error("示例数据初始化失败", e);
        }
    }

    /**
     * 初始化示例工具
     */
    private void initSampleTools() {
        // 检查是否已存在系统工具
        long count = toolsDao.count();
        if (count > 0) {
            log.info("已存在工具数据，跳过初始化");
            return;
        }

        // 创建日期计算工具
        Tools dateTool = new Tools();
        dateTool.setOwnerId(null); // 系统工具
        dateTool.setSourceType("SYSTEM");
        dateTool.setToolCodeName("date_calculator");
        dateTool.setDisplayName("日期计算器");
        dateTool.setDescription("计算日期差、日期加减等工具");
        dateTool.setInputSchema(createDateCalculatorSchema());
        dateTool.setIsEnabled(true);
        dateTool.setIsDelete(0);
        dateTool.setCreatedAt(new Date());
        dateTool.setUpdatedAt(new Date());

        toolsDao.save(dateTool);

        log.info("创建系统工具：日期计算器");

        // 创建天气查询工具（示例）
        Tools weatherTool = new Tools();
        weatherTool.setOwnerId(null);
        weatherTool.setSourceType("SYSTEM");
        weatherTool.setToolCodeName("weather_query");
        weatherTool.setDisplayName("天气查询");
        weatherTool.setDescription("查询城市天气信息");
        weatherTool.setInputSchema(createWeatherQuerySchema());
        weatherTool.setCustomEndpointUrl("https://api.weather.com/v1/current");
        weatherTool.setIsEnabled(true);
        weatherTool.setIsDelete(0);
        weatherTool.setCreatedAt(new Date());
        weatherTool.setUpdatedAt(new Date());

        toolsDao.save(weatherTool);

        log.info("创建系统工具：天气查询");

        // 创建产品查询工具（示例）
        Tools productTool = new Tools();
        productTool.setOwnerId(null);
        productTool.setSourceType("SYSTEM");
        productTool.setToolCodeName("product_search");
        productTool.setDisplayName("产品搜索");
        productTool.setDescription("搜索产品信息和库存状态");
        productTool.setInputSchema(createProductSearchSchema());
        productTool.setIsEnabled(true);
        productTool.setIsDelete(0);
        productTool.setCreatedAt(new Date());
        productTool.setUpdatedAt(new Date());

        toolsDao.save(productTool);

        log.info("创建系统工具：产品搜索");
    }

    /**
     * 初始化示例模型提供商
     */
    private void initSampleProviders() {
        // 检查是否已存在提供商
        long count= modelProviderDao.count();
        if (count > 0) {
            log.info("已存在模型提供商数据，跳过初始化");
            return;
        }

        // 创建 Ollama 提供商示例
        ModelProvider ollamaProvider = new ModelProvider();
        ollamaProvider.setUserId(1L); // 默认管理员
        ollamaProvider.setProviderName("本地 Ollama");
        ollamaProvider.setProviderCode("ollama");
        ollamaProvider.setBaseUrl("http://localhost:11434");
        ollamaProvider.setApiKeyEncrypted("not-needed");
        ollamaProvider.setStatus(1);
        ollamaProvider.setIsDelete(0);
        ollamaProvider.setCreatedAt(new Date());
        ollamaProvider.setUpdatedAt(new Date());

        modelProviderDao.save(ollamaProvider);

        log.info("创建模型提供商：本地 Ollama");

        // 创建 Dashscope 提供商示例
        ModelProvider dashscopeProvider = new ModelProvider();
        dashscopeProvider.setUserId(1L);
        dashscopeProvider.setProviderName("阿里云 Dashscope");
        dashscopeProvider.setProviderCode("dashscope");
        dashscopeProvider.setBaseUrl("https://dashscope.aliyuncs.com/api/v1");
        dashscopeProvider.setApiKeyEncrypted("sk-your-api-key-here");
        dashscopeProvider.setStatus(1);
        dashscopeProvider.setIsDelete(0);
        dashscopeProvider.setCreatedAt(new Date());
        dashscopeProvider.setUpdatedAt(new Date());

        modelProviderDao.save(dashscopeProvider);

        log.info("创建模型提供商：阿里云 Dashscope");
    }

    /**
     * 初始化示例模型
     */
    private void initSampleModels() {
        // 检查是否已存在模型
        long count = aiModelDao.count();
        if (count > 0) {
            log.info("已存在模型数据，跳过初始化");
            return;
        }

        // 获取 Ollama 提供商
        ModelProvider ollamaProvider = getOllamaProvider();
        if (ollamaProvider == null) {
            log.warn("未找到 Ollama 提供商，跳过模型初始化");
            return;
        }

        // 创建 Qwen 聊天模型
        AiModel qwenPlus = new AiModel();
        qwenPlus.setUserId(1L);
        qwenPlus.setProviderId(ollamaProvider.getId());
        qwenPlus.setModelName("qwen-plus");
        qwenPlus.setModelDisplayName("通义千问 Plus");
        qwenPlus.setModelType("CHAT");
        qwenPlus.setContextWindow(32000);
        qwenPlus.setMaxTokens(8000);
        qwenPlus.setInputCostPer1k(new BigDecimal("0.004"));
        qwenPlus.setOutputCostPer1k(new BigDecimal("0.012"));
        qwenPlus.setCurrencyType("CNY");
        qwenPlus.setIsActive(true);
        qwenPlus.setIsDelete(0);
        qwenPlus.setCreatedAt(new Date());
        qwenPlus.setUpdatedAt(new Date());

        aiModelDao.save(qwenPlus);

        log.info("创建 AI 模型：通义千问 Plus");

        // 创建内部决策用小模型
        AiModel gemma = new AiModel();
        gemma.setUserId(1L);
        gemma.setProviderId(ollamaProvider.getId());
        gemma.setModelName("gemma:2b");
        gemma.setModelDisplayName("Gemma 2B (快速决策)");
        gemma.setModelType("CHAT");
        gemma.setContextWindow(8000);
        gemma.setMaxTokens(2000);
        gemma.setInputCostPer1k(new BigDecimal("0.001"));
        gemma.setOutputCostPer1k(new BigDecimal("0.001"));
        gemma.setCurrencyType("CNY");
        gemma.setIsActive(true);
        gemma.setIsDelete(0);
        gemma.setCreatedAt(new Date());
        gemma.setUpdatedAt(new Date());

        aiModelDao.save(gemma);

        log.info("创建 AI 模型：Gemma 2B (快速决策)");

        // 创建嵌入模型
        AiModel embeddingModel = new AiModel();
        embeddingModel.setUserId(1L);
        embeddingModel.setProviderId(ollamaProvider.getId());
        embeddingModel.setModelName("text-embedding-v2");
        embeddingModel.setModelDisplayName("文本嵌入 V2");
        embeddingModel.setModelType("EMBEDDING");
        embeddingModel.setContextWindow(512);
        embeddingModel.setMaxTokens(512);
        embeddingModel.setInputCostPer1k(new BigDecimal("0.0007"));
        embeddingModel.setOutputCostPer1k(new BigDecimal("0"));
        embeddingModel.setCurrencyType("CNY");
        embeddingModel.setIsActive(true);
        embeddingModel.setIsDelete(0);
        embeddingModel.setCreatedAt(new Date());
        embeddingModel.setUpdatedAt(new Date());

        aiModelDao.save(embeddingModel);

        log.info("创建 AI 模型：文本嵌入 V2");
    }

    /**
     * 获取 Ollama 提供商
     */
    private ModelProvider getOllamaProvider() {
        // 通过查询所有提供商来查找 Ollama
        List<ModelProvider> providers = modelProviderDao.list();
        if (providers.isEmpty()) {
            return null;
        }

        return providers.stream()
                .filter(p -> "ollama".equals(p.getProviderCode()) || "本地 Ollama".equals(p.getProviderName()))
                .findFirst()
                .orElse(null);
    }

    /**
     * 创建天气查询工具的输入 Schema
     */
    private Object createWeatherQuerySchema() {
        String schema = """
            {
              "type": "object",
              "properties": {
                "city": {
                  "type": "string",
                  "description": "城市名称"
                },
                "unit": {
                  "type": "string",
                  "enum": ["celsius", "fahrenheit"],
                  "default": "celsius",
                  "description": "温度单位"
                }
              },
              "required": ["city"]
            }
            """;
        return schema;
    }

    /**
     * 创建产品搜索工具的输入 Schema
     */
    private Object createProductSearchSchema() {
        String schema = """
            {
              "type": "object",
              "properties": {
                "keyword": {
                  "type": "string",
                  "description": "搜索关键词"
                },
                "category": {
                  "type": "string",
                  "description": "产品分类"
                },
                "limit": {
                  "type": "integer",
                  "default": 10,
                  "description": "返回结果数量"
                }
              },
              "required": ["keyword"]
            }
            """;
        return schema;
    }
}
