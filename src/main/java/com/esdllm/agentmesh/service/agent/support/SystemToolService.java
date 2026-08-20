package com.esdllm.agentmesh.service.agent.support;

import cn.hutool.core.util.StrUtil;
import com.esdllm.agentmesh.common.ErrorCode;
import com.esdllm.agentmesh.exception.BusinessException;
import com.esdllm.agentmesh.model.domain.AiModel;
import com.esdllm.agentmesh.model.domain.ModelProvider;
import com.esdllm.agentmesh.model.dto.ToolInvocationContext;
import com.esdllm.agentmesh.repository.dao.AiModelDao;
import com.esdllm.agentmesh.repository.dao.ModelProviderDao;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 系统工具服务：统一管理所有系统内置工具的实现
 */
@Service
@Slf4j
public class SystemToolService {

    @Resource
    private AiModelDao aiModelDao;

    @Resource
    private ModelProviderDao modelProviderDao;

    @Resource
    private AiModelSupport aiModelSupport;

    /**
     * 调用系统工具
     */
    public String invokeSystemTool(String toolCodeName, ToolInvocationContext context) {
        try {
            return switch (toolCodeName) {
                case "date_calculator" -> calculateDate(context);
                case "unit_converter" -> convertUnit(context);
                case "translator" -> translateText(context);
                case "text_summarizer" -> summarizeText(context);
                case "optimize_prompt" -> optimizePrompt(context);
                case "re_reading" -> reReading(context);
                case "get_user_location" -> getUserLocation();
                case "weather_query" -> weatherQuery(context);
                case "product_search" -> productSearch(context);
                case "online_search" -> onlineSearch(context);
                default -> {
                    log.warn("未实现的系统工具：{}", toolCodeName);
                    yield "系统工具 [" + toolCodeName + "] 暂未实现";
                }
            };
        } catch (Exception e) {
            log.error("系统工具调用失败：{}", toolCodeName, e);
            return "工具执行失败：" + e.getMessage();
        }
    }

    /**
     * 日期计算器
     */
    public String calculateDate(ToolInvocationContext context) {
        try {
            String operation = (String) context.getParameters().get("operation");
            String dateStr = (String) context.getParameters().get("date");
            int days = Integer.parseInt(context.getParameters().getOrDefault("days", "0").toString());

            // 验证必需参数
            if (StrUtil.isBlank(operation)) {
                return "日期计算失败：缺少必需参数 operation（应为 'add' 或 'subtract'）";
            }

            LocalDate date = StrUtil.isNotBlank(dateStr) ?
                    LocalDate.parse(dateStr) : LocalDate.now();

            String result = switch (operation) {
                case "add" -> date.plusDays(days).toString();
                case "subtract" -> date.minusDays(days).toString();
                default -> {
                    log.warn("不支持的操作类型：{}，默认返回当前日期", operation);
                    yield date.toString();
                }
            };

            return "日期计算结果：" + result;
        } catch (NumberFormatException e) {
            log.error("参数格式错误", e);
            return "日期计算失败：days 参数必须是数字";
        } catch (Exception e) {
            log.error("日期计算失败", e);
            return "日期计算失败：" + e.getMessage();
        }
    }

    /**
     * 单位转换器
     */
    public String convertUnit(ToolInvocationContext context) {
        try {
            String valueStr = (String) context.getParameters().get("value");
            String fromUnit = (String) context.getParameters().get("fromUnit");
            String toUnit = (String) context.getParameters().get("toUnit");

            // 验证必需参数
            if (StrUtil.isBlank(valueStr)) {
                return "单位转换失败：缺少必需参数 value";
            }
            
            if (StrUtil.isBlank(fromUnit)) {
                return "单位转换失败：缺少必需参数 fromUnit";
            }
            
            if (StrUtil.isBlank(toUnit)) {
                return "单位转换失败：缺少必需参数 toUnit";
            }

            double value = Double.parseDouble(valueStr);
            double result = convertValue(value, fromUnit, toUnit);

            return String.format("单位转换结果：%.4f %s = %.4f %s",
                    value, fromUnit, result, toUnit);
        } catch (NumberFormatException e) {
            log.error("数值格式错误", e);
            return "单位转换失败：数值格式不正确（value 必须是数字）";
        } catch (IllegalArgumentException e) {
            log.error("单位不支持", e);
            return "单位转换失败：" + e.getMessage();
        } catch (Exception e) {
            log.error("单位转换失败", e);
            return "单位转换失败：" + e.getMessage();
        }
    }

    private double convertValue(double value, String fromUnit, String toUnit) {
        if (fromUnit.equals(toUnit)) {
            return value;
        }

        UnitConverter converter = new UnitConverter();
        return converter.convert(value, fromUnit, toUnit);
    }

    private static class UnitConverter {
        private static final Map<String, Double> TO_BASE_FACTOR = new HashMap<>();
        private static final Map<String, String> UNIT_GROUPS = new HashMap<>();

        static {
            // 长度单位 (基准：米)
            UNIT_GROUPS.put("m", "length");
            TO_BASE_FACTOR.put("m", 1.0);
            TO_BASE_FACTOR.put("km", 1000.0);
            TO_BASE_FACTOR.put("cm", 0.01);
            TO_BASE_FACTOR.put("mm", 0.001);
            TO_BASE_FACTOR.put("ft", 0.3048);
            TO_BASE_FACTOR.put("in", 0.0254);

            // 质量单位 (基准：千克)
            UNIT_GROUPS.put("kg", "mass");
            TO_BASE_FACTOR.put("kg", 1.0);
            TO_BASE_FACTOR.put("g", 0.001);
            TO_BASE_FACTOR.put("mg", 0.000001);
            TO_BASE_FACTOR.put("lb", 0.453592);
            TO_BASE_FACTOR.put("oz", 0.0283495);

            // 温度单位 (特殊处理)
            UNIT_GROUPS.put("°C", "temperature");
            UNIT_GROUPS.put("°F", "temperature");
            UNIT_GROUPS.put("K", "temperature");
        }

        private double convert(double value, String fromUnit, String toUnit) {
            String fromGroup = UNIT_GROUPS.get(fromUnit);
            String toGroup = UNIT_GROUPS.get(toUnit);

            if (fromGroup == null || toGroup == null) {
                throw new IllegalArgumentException("不支持的单位：" + 
                    (fromGroup == null ? fromUnit : toUnit));
            }

            if (!fromGroup.equals(toGroup)) {
                throw new IllegalArgumentException("无法在不同类型的单位间转换：" + 
                    fromUnit + " → " + toUnit);
            }

            if ("temperature".equals(fromGroup)) {
                return convertTemperature(value, fromUnit, toUnit);
            }

            double baseValue = value * TO_BASE_FACTOR.get(fromUnit);
            return baseValue / TO_BASE_FACTOR.get(toUnit);
        }

        private double convertTemperature(double value, String fromUnit, String toUnit) {
            double celsius = switch (fromUnit) {
                case "°C" -> value;
                case "°F" -> (value - 32) * 5 / 9;
                case "K" -> value - 273.15;
                default -> throw new IllegalArgumentException("未知的温度单位：" + fromUnit);
            };

            return switch (toUnit) {
                case "°C" -> celsius;
                case "°F" -> celsius * 9 / 5 + 32;
                case "K" -> celsius + 273.15;
                default -> throw new IllegalArgumentException("未知的温度单位：" + toUnit);
            };
        }
    }

    /**
     * AI 翻译
     */
    public String translateText(ToolInvocationContext context) {
        long startTime = System.currentTimeMillis();
        try {
            String text = (String) context.getParameters().get("text");
            String targetLang = (String) context.getParameters().get("targetLang");

            if (StrUtil.isBlank(text)) {
                return "翻译失败：输入文本为空";
            }

            if (StrUtil.isBlank(targetLang)) {
                return "翻译失败：请指定目标语言";
            }

            String translation = translateWithAI(text, targetLang);

            long executionTime = System.currentTimeMillis() - startTime;
            log.info("AI 翻译完成，目标语言：{}, 原文长度：{}, 译文长度：{}, 耗时：{}ms",
                    targetLang, text.length(), translation.length(), executionTime);

            return "翻译结果（" + targetLang + "）：" + translation;

        } catch (Exception e) {
            log.error("翻译失败", e);
            return "翻译失败：" + e.getMessage();
        }
    }

    /**
     * AI 文本摘要
     */
    public String summarizeText(ToolInvocationContext context) {
        try {
            String text = (String) context.getParameters().get("text");
            int maxLength = Integer.parseInt(context.getParameters().getOrDefault("maxLength", "100").toString());

            if (StrUtil.isBlank(text)) {
                return "文本摘要：输入文本为空";
            }

            String summary = generateAISummary(text, maxLength);

            log.info("AI 文本摘要生成成功，原文长度：{}, 摘要长度：{}", text.length(), summary.length());
            return "文本摘要：" + summary;

        } catch (Exception e) {
            log.error("文本摘要失败", e);
            return "文本摘要失败：" + e.getMessage();
        }
    }

    /**
     * 提示词优化
     * @param context 包含待优化的提示词文本
     * @return 优化后的提示词
     */
    public String optimizePrompt(ToolInvocationContext context) {
        try {
            String text = (String) context.getParameters().get("text");
            
            if (StrUtil.isBlank(text)) {
                return "提示词优化失败：输入文本为空";
            }

            // 使用 ID 为 1 的模型
            AiModel aiModel = aiModelDao.getById(1L);
            if (aiModel == null) {
                log.warn("未找到 ID 为 1 的 AI 模型");
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "系统未配置可用的 AI 模型（ID=1）");
            }

            ModelProvider provider = modelProviderDao.getById(aiModel.getProviderId());
            if (provider == null) {
                log.warn("未找到模型提供商配置，providerId: {}", aiModel.getProviderId());
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "模型提供商配置不存在");
            }

            String prompt = buildOptimizationPrompt(text);
            
            ChatModel chatModel = aiModelSupport.createChatModel(aiModel, provider);
            ChatClient chatClient = ChatClient.builder(chatModel).build();

            String optimizedPrompt = chatClient.prompt()
                    .user(prompt)
                    .options(aiModelSupport.buildModelOptions(aiModel))
                    .call()
                    .content();

            if (StrUtil.isBlank(optimizedPrompt)) {
                log.error("AI 提示词优化返回空结果");
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "AI 优化返回空结果");
            }

            log.info("提示词优化成功，原文长度：{}, 优化后长度：{}", text.length(), optimizedPrompt.length());
            
            return "优化后的提示词：\n" + optimizedPrompt.trim();

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("提示词优化失败", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "提示词优化失败：" + e.getMessage());
        }
    }

    /**
     * 天气查询
     */
    public String weatherQuery(ToolInvocationContext context) {
        try {
            String city = (String) context.getParameters().get("city");

            if (StrUtil.isBlank(city)) {
                return "天气查询失败：请提供城市名称";
            }

            // 使用高德地图天气 API（免费，无需 key 的基础版本）
            String location = geocodeCity(city);
            if (location == null) {
                return "天气查询失败：未找到城市 '" + city + "'";
            }

            String weatherInfo = queryWeather(location);

            log.info("天气查询成功，城市：{}, 位置：{}", city, location);

            return weatherInfo;

        } catch (Exception e) {
            log.error("天气查询失败", e);
            return "天气查询失败：" + e.getMessage();
        }
    }

    /**
     * 产品搜索
     */
    public String productSearch(ToolInvocationContext context) {
        try {
            String productName = (String) context.getParameters().get("productName");
            String category = (String) context.getParameters().get("category");
            Integer maxResults = (Integer) context.getParameters().getOrDefault("maxResults", 5);

            if (StrUtil.isBlank(productName)) {
                return "产品搜索失败：请提供产品名称";
            }

            // 模拟产品搜索（实际项目中应该调用数据库或商品 API）
            List<Map<String, Object>> products = searchProductsFromDatabase(productName, category, maxResults);

            if (products.isEmpty()) {
                return "未找到相关产品：" + productName;
            }

            StringBuilder result = new StringBuilder();
            result.append("🔍 产品搜索结果（共 ").append(products.size()).append(" 个）：\n\n");

            for (int i = 0; i < products.size(); i++) {
                Map<String, Object> product = products.get(i);
                result.append((i + 1)).append(". ").append(product.get("name")).append("\n");
                result.append("   价格：¥").append(product.get("price")).append("\n");
                result.append("   分类：").append(product.get("category")).append("\n");
                result.append("   库存：").append(product.get("stock")).append(" 件\n");
                result.append("   描述：").append(product.get("description")).append("\n\n");
            }

            log.info("产品搜索成功，关键词：{}, 分类：{}, 结果数：{}", productName, category, products.size());

            return result.toString();

        } catch (Exception e) {
            log.error("产品搜索失败", e);
            return "产品搜索失败：" + e.getMessage();
        }
    }

    /**
     * 联网搜索
     */
    public String onlineSearch(ToolInvocationContext context) {
        try {
            String query = (String) context.getParameters().get("query");
            Integer maxResults = (Integer) context.getParameters().getOrDefault("maxResults", 5);

            if (StrUtil.isBlank(query)) {
                return "搜索失败：请提供搜索关键词";
            }

            // 使用 DuckDuckGo HTML 搜索（无需 API Key）
            List<Map<String, String>> searchResults = searchWithDuckDuckGo(query, maxResults);

            if (searchResults.isEmpty()) {
                return "未找到相关搜索结果：" + query;
            }

            StringBuilder result = new StringBuilder();
            result.append("🔍 搜索结果（共 ").append(searchResults.size()).append(" 条）：\n\n");

            for (int i = 0; i < searchResults.size(); i++) {
                Map<String, String> item = searchResults.get(i);
                result.append((i + 1)).append(". ").append(item.get("title")).append("\n");
                result.append("   链接：").append(item.get("url")).append("\n");
                result.append("   摘要：").append(item.get("snippet")).append("\n\n");
            }

            log.info("联网搜索成功，关键词：{}, 结果数：{}", query, searchResults.size());

            return result.toString();

        } catch (Exception e) {
            log.error("联网搜索失败", e);
            return "搜索失败：" + e.getMessage();
        }
    }

    /**
     * 使用 DuckDuckGo 搜索（模拟实现）
     */
    private List<Map<String, String>> searchWithDuckDuckGo(String query, int maxResults) {
        List<Map<String, String>> results = new ArrayList<>();

        try {
            // 方案 1：使用 DuckDuckGo HTML 接口（需要解析 HTML）
            // 由于 HTML 解析较复杂，这里使用简化的模拟实现

            // 方案 2：使用 Bing/Google 的免费 API（需要申请 Key）
            // 这里提供模拟数据用于测试

            log.info("执行搜索：{}, 最大结果数：{}", query, maxResults);

            // 模拟搜索结果
            for (int i = 0; i < Math.min(maxResults, 5); i++) {
                Map<String, String> item = new HashMap<>();
                item.put("title", "搜索结果 " + (i + 1) + ": " + query);
                item.put("url", "https://example.com/result-" + (i + 1));
                item.put("snippet", "这是关于\"" + query + "\"的搜索结果摘要..." +
                        "实际项目中会调用真实的搜索引擎 API 获取数据。");
                results.add(item);
            }

        } catch (Exception e) {
            log.warn("搜索异常，返回模拟数据", e);
            // 即使出错也返回模拟数据
            for (int i = 0; i < maxResults; i++) {
                Map<String, String> item = new HashMap<>();
                item.put("title", "搜索结果 " + (i + 1) + ": " + query);
                item.put("url", "https://example.com/result-" + (i + 1));
                item.put("snippet", "搜索服务暂时不可用，这是模拟结果");
                results.add(item);
            }
        }

        return results;
    }


    /**
     * 从数据库搜索产品（模拟实现）
     */
    private List<Map<String, Object>> searchProductsFromDatabase(String productName, String category, int maxResults) {
        List<Map<String, Object>> results = new ArrayList<>();

        // TODO: 实际项目中应该查询数据库
        // 这里提供模拟数据用于测试

        // 示例模拟数据
        Map<String, Object> product1 = new HashMap<>();
        product1.put("name", productName + " - 标准版");
        product1.put("price", "99.00");
        product1.put("category", category != null ? category : "数码配件");
        product1.put("stock", 158);
        product1.put("description", "高性价比版本，适合日常使用");
        results.add(product1);

        if (maxResults > 1) {
            Map<String, Object> product2 = new HashMap<>();
            product2.put("name", productName + " - 专业版");
            product2.put("price", "199.00");
            product2.put("category", category != null ? category : "数码配件");
            product2.put("stock", 87);
            product2.put("description", "功能更全面，适合专业人士");
            results.add(product2);
        }

        if (maxResults > 2) {
            Map<String, Object> product3 = new HashMap<>();
            product3.put("name", productName + " - 旗舰版");
            product3.put("price", "299.00");
            product3.put("category", category != null ? category : "数码配件");
            product3.put("stock", 42);
            product3.put("description", "顶级配置，极致体验");
            results.add(product3);
        }

        return results.stream().limit(maxResults).toList();
    }

    /**
     * 将城市名转换为经纬度坐标
     */
    private String geocodeCity(String city) {
        try {
            // 使用 OpenStreetMap Nominatim 服务（免费，无需 key）
            String url = "https://nominatim.openstreetmap.org/search?format=json&q=" +
                    java.net.URLEncoder.encode(city, StandardCharsets.UTF_8) + "&limit=1";

            var restTemplate = new RestTemplate();
            String response = restTemplate.getForObject(url, String.class);

            if (StrUtil.isNotBlank(response)) {
                JsonNode jsonNode = new ObjectMapper().readTree(response);
                if (jsonNode.isArray() && !jsonNode.isEmpty()) {
                    JsonNode firstResult = jsonNode.get(0);
                    String lat = firstResult.get("lat").asText();
                    String lon = firstResult.get("lon").asText();
                    return lat + "," + lon;
                }
            }
        } catch (Exception e) {
            log.warn("地理编码失败，城市：{}", city, e);
        }
        return null;
    }

    /**
     * 查询天气信息
     */
    private String queryWeather(String location) {
        try {
            // 使用 Open-Meteo 天气 API（免费，无需 key）
            String[] parts = location.split(",");
            String lat = parts[0];
            String lon = parts[1];

            String url = String.format(
                    "https://api.open-meteo.com/v1/forecast?latitude=%s&longitude=%s" +
                            "&current_weather=true&hourly=temperature_2m,relativehumidity_2m,precipitation_probability",
                    lat, lon
            );

            var restTemplate = new RestTemplate();
            String response = restTemplate.getForObject(url, String.class);

            if (StrUtil.isNotBlank(response)) {
                JsonNode jsonNode = new ObjectMapper().readTree(response);

                // 解析当前天气
                JsonNode current = jsonNode.get("current_weather");
                if (current != null) {
                    double temperature = current.get("temperature").asDouble();
                    double windspeed = current.get("windspeed").asDouble();
                    int winddirection = current.get("winddirection").asInt();
                    String weatherCode = current.get("weathercode").asText();

                    String weatherInfo = "🌤️ 天气信息：\n" +
                            "温度：" + temperature + "°C\n" +
                            "风速：" + windspeed + " km/h\n" +
                            "风向：" + winddirection + "°\n" +
                            "天气状况：" + getWeatherDescription(weatherCode);

                    return weatherInfo;
                }
            }

            return "未能获取天气数据";

        } catch (Exception e) {
            log.warn("天气查询失败", e);
            return "天气查询失败：" + e.getMessage();
        }
    }

    /**
     * 根据天气代码获取描述
     */
    private String getWeatherDescription(String code) {
        int codeNum = Integer.parseInt(code);
        return switch (codeNum) {
            case 0 -> "晴朗";
            case 1, 2, 3 -> "多云";
            case 45, 48 -> "有雾";
            case 51, 53, 55 -> "毛毛雨";
            case 61, 63, 65 -> "下雨";
            case 71, 73, 75 -> "下雪";
            case 80, 81, 82 -> "阵雨";
            case 95, 96, 99 -> "雷雨";
            default -> "未知";
        };
    }

    /**
     * 构建提示词优化的 Prompt
     */
    private String buildOptimizationPrompt(String text) {
        String truncatedText = text.length() > 5000 ? text.substring(0, 5000) + "...[内容过长已截断]" : text;

        return String.format("""
你是一个专业的提示词工程师。请帮助用户优化他们的提示词（Prompt），使其更加清晰、有效。

任务要求：
1. 分析原始提示词的结构和内容
2. 优化提示词的表达，使其更加明确和具体
3. 保持原意不变，提升表达质量
4. 直接输出优化后的提示词，不需要解释

原始提示词：
"%s"

优化后的提示词：""", truncatedText);
    }

    /**
     * RE-Reading（重读）- 对文本进行二次深度阅读和理解
     * @param context 包含待重读的文本
     * @return 重读后的结构化内容
     */
    public String reReading(ToolInvocationContext context) {
        String text = (String) context.getParameters().get("text");

        return text +
                "\n\nplease read the prompt again:\n" +
                text;
    }
    /**
     * 获取使用者的位置信息（精确到县级）
     */
    public String getUserLocation() {
        try {
            // 尝试从 HTTP 请求中获取客户端 IP
            HttpServletRequest request = null;
            try {
                request = ((ServletRequestAttributes)
                        Objects.requireNonNull(RequestContextHolder.getRequestAttributes()))
                    .getRequest();
            } catch (Exception e) {
                log.debug("非 HTTP 请求上下文，将无法获取位置信息");
            }
            
            if (request == null) {
                return "位置信息：无法获取（非 HTTP 请求上下文）";
            }
            
            String ip = getClientIp(request);
            
            if (StrUtil.isBlank(ip) || "127.0.0.1".equals(ip) || "0:0:0:0:0:0:0:1".equals(ip)) {
                return "位置信息：本地访问";
            }
            
            // 调用 IP 地址库查询地理位置
            String location = queryIpLocation(ip);
            
            log.info("IP 地址查询结果：IP={}, 位置={}", ip, location);
            
            return location;
            
        } catch (Exception e) {
            log.error("获取位置信息失败", e);
            return "无法获取位置信息";
        }
    }

    /**
     * 查询 IP 地址的地理位置（使用腾讯网 IP 地址库）
     */
    private String queryIpLocation(String ip) {
        try {
            // 使用腾讯网免费的 IP 地址查询接口
            String url = "https://apis.map.qq.com/ws/location/v1/ip";
            
            // 构建请求参数（需要腾讯地图 Key，可以替换为其他免费服务）
            // 这里使用一个不需要 key 的备用方案
            String backupUrl = "http://ip-api.com/json/" + ip + "?lang=zh-CN";
            
            var restTemplate = new RestTemplate();
            
            // 尝试使用备用方案（ip-api.com 免费版）
            String response = restTemplate.getForObject(backupUrl, String.class);
            
            if (StrUtil.isNotBlank(response)) {
                // 解析 JSON 响应
                JsonNode jsonNode =
                    new ObjectMapper().readTree(response);
                
                String status = jsonNode.get("status").asText();
                
                if ("success".equals(status)) {
                    String country = jsonNode.has("country") ? jsonNode.get("country").asText() : "";
                    String region = jsonNode.has("regionName") ? jsonNode.get("regionName").asText() : "";
                    String city = jsonNode.has("city") ? jsonNode.get("city").asText() : "";
                    String district = jsonNode.has("district") ? jsonNode.get("district").asText() : "";
                    String isp = jsonNode.has("isp") ? jsonNode.get("isp").asText() : "";
                    
                    StringBuilder location = new StringBuilder();
                    location.append("📍 位置信息：");
                    
                    if (StrUtil.isNotBlank(country)) {
                        location.append(country);
                    }
                    if (StrUtil.isNotBlank(region)) {
                        location.append(" ").append(region);
                    }
                    if (StrUtil.isNotBlank(city)) {
                        location.append(" ").append(city);
                    }
                    if (StrUtil.isNotBlank(district)) {
                        location.append(" ").append(district);
                    }
                    if (StrUtil.isNotBlank(isp)) {
                        location.append(" (").append(isp).append(")");
                    }
                    
                    return location.toString().trim();
                }
            }
            
            // 如果查询失败，返回 IP 地址
            return "IP 地址：" + ip + "（位置查询失败）";
            
        } catch (Exception e) {
            log.warn("IP 地址查询异常，返回 IP 地址", e);
            return "IP 地址：" + ip;
        }
    }

    /**
     * 从 HttpServletRequest 中获取真实客户端 IP
     */
    private String getClientIp(jakarta.servlet.http.HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (StrUtil.isBlank(ip) || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (StrUtil.isBlank(ip) || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (StrUtil.isBlank(ip) || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (StrUtil.isBlank(ip) || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        
        // 如果是多代理，取第一个 IP
        if (StrUtil.isNotBlank(ip) && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        
        return ip;
    }

    // ==================== 私有辅助方法 ====================

    private String translateWithAI(String text, String targetLang) {
        try {
            AiModel translationModel = getTranslationModel();
            if (translationModel == null) {
                translationModel = aiModelDao.getById(1L);
            }

            if (translationModel == null) {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "未配置可用的 AI 模型");
            }

            ModelProvider provider = modelProviderDao.getById(translationModel.getProviderId());
            if (provider == null) {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "模型提供商配置不存在");
            }

            String prompt = buildTranslationPrompt(text, targetLang);
            ChatModel chatModel = aiModelSupport.createChatModel(translationModel, provider);
            ChatClient chatClient = ChatClient.builder(chatModel).build();

            String translation = chatClient.prompt()
                    .user(prompt)
                    .options(aiModelSupport.buildModelOptions(translationModel))
                    .call()
                    .content();

            if (StrUtil.isBlank(translation)) {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "AI 翻译返回空结果");
            }

            return translation.trim();

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("AI 翻译异常", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "翻译服务异常：" + e.getMessage());
        }
    }

    private AiModel getTranslationModel() {
        try {
            List<AiModel> allModels = aiModelDao.list();
            if (allModels != null && !allModels.isEmpty()) {
                Optional<AiModel> translateModel = allModels.stream()
                        .filter(m -> m.getModelName().toLowerCase().contains("translate") ||
                                (m.getModelDisplayName() != null &&
                                        m.getModelDisplayName().toLowerCase().contains("translate")))
                        .findFirst();

                if (translateModel.isPresent()) {
                    log.debug("找到翻译专用模型：{}", translateModel.get().getModelName());
                    return translateModel.get();
                }

                Optional<AiModel> lightModel = allModels.stream()
                        .filter(m -> m.getModelName().toLowerCase().contains("turbo") ||
                                m.getModelName().toLowerCase().contains("mini") ||
                                m.getModelName().toLowerCase().contains("flash"))
                        .findFirst();

                if (lightModel.isPresent()) {
                    log.debug("使用轻量级模型进行翻译：{}", lightModel.get().getModelName());
                    return lightModel.get();
                }
            }

            return aiModelDao.getById(1L);

        } catch (Exception e) {
            log.error("获取翻译模型失败", e);
            return null;
        }
    }

    private String buildTranslationPrompt(String text, String targetLang) {
        String targetLangName = getTargetLangName(targetLang);
        String truncatedText = text.length() > 5000 ? text.substring(0, 5000) + "...[内容过长已截断]" : text;

        return String.format("""
                你是一位专业的翻译人员。请将以下文本翻译成%s。
                
                翻译要求：
                1. 准确传达原文的含义和语气
                2. 符合目标语言的表达习惯，不要逐字翻译
                3. 保持专业术语的准确性
                4. 如果是技术文档，保持术语的专业性
                5. 只输出翻译结果，不要添加任何解释
                
                待翻译文本：
                "%s"
                
                %s翻译：""",
                targetLangName,
                truncatedText,
                targetLangName
        );
    }

    private static String getTargetLangName(String targetLang) {
        Map<String, String> langMap = new HashMap<>();
        langMap.put("en", "英语");
        langMap.put("zh", "中文");
        langMap.put("ja", "日语");
        langMap.put("ko", "韩语");
        langMap.put("fr", "法语");
        langMap.put("de", "德语");
        langMap.put("es", "西班牙语");
        langMap.put("ru", "俄语");
        langMap.put("pt", "葡萄牙语");
        langMap.put("it", "意大利语");
        langMap.put("ar", "阿拉伯语");
        langMap.put("hi", "印地语");
        langMap.put("th", "泰语");
        langMap.put("vi", "越南语");

        return langMap.getOrDefault(targetLang.toLowerCase(), targetLang);
    }

    private String generateAISummary(String text, int maxLength) {
        try {
            AiModel aiModel = aiModelDao.getById(1L);
            if (aiModel == null) {
                log.warn("未找到 AI 模型，使用规则-based 摘要");
                return summarizeTextByRules(text, maxLength);
            }

            ModelProvider provider = modelProviderDao.getById(aiModel.getProviderId());
            if (provider == null) {
                log.warn("未找到模型提供商，使用规则-based 摘要");
                return summarizeTextByRules(text, maxLength);
            }

            String prompt = String.format("""
                    请将以下文本浓缩成简洁的摘要，保持在%d字以内。要求：
                    1. 保留核心信息和关键事实
                    2. 语言通顺连贯
                    3. 删除冗余和次要细节
                    
                    原文：
                    %s
                    
                    摘要：""",
                    maxLength,
                    text.length() > 3000 ? text.substring(0, 3000) + "..." : text
            );

            ChatModel chatModel = aiModelSupport.createChatModel(aiModel, provider);
            ChatClient chatClient = ChatClient.builder(chatModel).build();

            String summary = chatClient.prompt()
                    .user(prompt)
                    .options(aiModelSupport.buildModelOptions(aiModel))
                    .call()
                    .content();

            return StrUtil.isNotBlank(summary) ? summary : summarizeTextByRules(text, maxLength);

        } catch (Exception e) {
            log.error("AI 摘要生成失败，降级到规则-based 方法", e);
            return summarizeTextByRules(text, maxLength);
        }
    }

    private String summarizeTextByRules(String text, int maxLength) {
        text = text.trim().replaceAll("\\s+", " ");

        if (text.length() <= maxLength) {
            return text;
        }

        List<String> sentences = splitSentences(text);
        List<String> keySentences = extractKeySentences(sentences, maxLength);
        String summary = String.join(" ", keySentences);

        if (summary.length() > maxLength) {
            summary = smartTruncate(summary, maxLength);
        }

        return summary;
    }

    private List<String> splitSentences(String text) {
        List<String> sentences = new ArrayList<>();
        String[] parts = text.split("(?<=[。！？.!?])\\s*");
        for (String part : parts) {
            String trimmed = part.trim();
            if (StrUtil.isNotBlank(trimmed)) {
                sentences.add(trimmed);
            }
        }
        return sentences;
    }

    private List<String> extractKeySentences(List<String> sentences, int maxLength) {
        List<String> result = new ArrayList<>();
        AtomicInteger currentLength = new AtomicInteger();

        if (!sentences.isEmpty()) {
            String firstSentence = sentences.getFirst();
            if (firstSentence.length() <= maxLength * 0.7) {
                result.add(firstSentence);
                currentLength.addAndGet(firstSentence.length());
            }
        }

        List<String> keywordSentences = sentences.stream()
                .filter(this::containsKeywords)
                .filter(s -> currentLength.get() + s.length() <= maxLength * 0.9)
                .limit(2)
                .toList();

        for (String sentence : keywordSentences) {
            if (!result.contains(sentence)) {
                result.add(sentence);
                currentLength.addAndGet(sentence.length());
            }
        }

        sentences.stream()
                .sorted((a, b) -> Integer.compare(b.length(), a.length()))
                .filter(s -> !result.contains(s))
                .filter(s -> currentLength.get() + s.length() <= maxLength)
                .limit(2)
                .forEach(s -> {
                    result.add(s);
                    currentLength.addAndGet(s.length());
                });

        return result;
    }

    private boolean containsKeywords(String sentence) {
        String lower = sentence.toLowerCase();
        List<String> keywords = Arrays.asList(
                "重要", "关键", "首先", "其次", "最后", "因此", "所以", "然而", "但是",
                "总之", "综上所述", "结果表明", "发现", "显示", "表明",
                "important", "key", "significant", "therefore", "however",
                "conclusion", "result", "show", "demonstrate", "find"
        );

        return keywords.stream().anyMatch(lower::contains);
    }

    private String smartTruncate(String text, int maxLength) {
        if (text.length() <= maxLength) {
            return text;
        }

        int lastSpace = text.lastIndexOf(" ", maxLength);
        int lastPunctuation = Math.max(
                text.lastIndexOf(".", maxLength),
                text.lastIndexOf(",", maxLength)
        );

        int cutPoint = Math.max(lastSpace, lastPunctuation);

        if (cutPoint > maxLength * 0.8) {
            return text.substring(0, cutPoint) + "...";
        } else {
            return text.substring(0, maxLength) + "...";
        }
    }
}
