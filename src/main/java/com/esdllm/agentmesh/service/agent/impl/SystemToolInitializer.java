package com.esdllm.agentmesh.service.agent.impl;

import cn.hutool.core.util.StrUtil;
import com.esdllm.agentmesh.model.domain.Tools;
import com.esdllm.agentmesh.repository.dao.ToolsDao;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * 系统工具初始化器
 * 在应用启动时自动将预定义的系统工具注册到数据库
 */
@Component
@Slf4j
public class SystemToolInitializer {

    @Resource
    private ToolsDao toolsDao;

    /**
     * 定义所有系统工具的配置
     */
    private static final List<Map<String, Object>> SYSTEM_TOOLS_CONFIG = Arrays.asList(
        // 1. 日期计算器
        Map.of(
            "toolCodeName", "date_calculator",
            "displayName", "日期计算器",
            "description", "计算指定日期的前后日期，支持加减天数",
            "inputSchema", Map.of(
                "type", "object",
                "properties", Map.of(
                    "operation", Map.of("type", "string", "enum", Arrays.asList("add", "subtract"), "description", "操作类型：add-增加天数，subtract-减少天数"),
                    "date", Map.of("type", "string", "description", "基准日期，格式：yyyy-MM-dd，不传则默认为今天"),
                    "days", Map.of("type", "integer", "description", "要加减的天数")
                ),
                "required", Arrays.asList("operation", "days")
            )
        ),
        
        // 2. 单位转换器
        Map.of(
            "toolCodeName", "unit_converter",
            "displayName", "单位转换器",
            "description", "支持常见物理单位的转换，如长度、重量等",
            "inputSchema", Map.of(
                "type", "object",
                "properties", Map.of(
                    "value", Map.of("type", "number", "description", "要转换的数值"),
                    "fromUnit", Map.of("type", "string", "description", "原始单位，如：km, m, kg, g"),
                    "toUnit", Map.of("type", "string", "description", "目标单位，如：km, m, kg, g")
                ),
                "required", Arrays.asList("value", "fromUnit", "toUnit")
            )
        ),
        
        // 3. 文本翻译
        Map.of(
            "toolCodeName", "translator",
            "displayName", "文本翻译",
            "description", "将文本翻译成指定语言，支持多种语言互译",
            "inputSchema", Map.of(
                "type", "object",
                "properties", Map.of(
                    "text", Map.of("type", "string", "description", "要翻译的文本"),
                    "targetLang", Map.of("type", "string", "description", "目标语言，如：en-英语，zh-中文，ja-日语")
                ),
                "required", Arrays.asList("text", "targetLang")
            )
        ),
        
        // 4. 文本摘要
        Map.of(
            "toolCodeName", "text_summarizer",
            "displayName", "文本摘要",
            "description", "对长文本生成摘要，提取关键信息",
            "inputSchema", Map.of(
                "type", "object",
                "properties", Map.of(
                    "text", Map.of("type", "string", "description", "要摘要的原文本"),
                    "maxLength", Map.of("type", "integer", "description", "摘要的最大长度，默认 100")
                ),
                "required", List.of("text")
            )
        ),
        
        // 5. 提示词优化
        Map.of(
            "toolCodeName", "optimize_prompt",
            "displayName", "提示词优化",
            "description", "使用 AI 优化用户的提示词，使其更加清晰有效",
            "inputSchema", Map.of(
                "type", "object",
                "properties", Map.of(
                    "text", Map.of("type", "string", "description", "待优化的提示词文本")
                ),
                "required", List.of("text")
            )
        ),
        
        // 6. RE-Reading（重读）
        Map.of(
            "toolCodeName", "re_reading",
            "displayName", "重读",
            "description", "对文本进行二次深度阅读，帮助理解和记忆",
            "inputSchema", Map.of(
                "type", "object",
                "properties", Map.of(
                    "text", Map.of("type", "string", "description", "需要重读的文本")
                ),
                "required", List.of("text")
            )
        ),
        
        // 7. 获取用户位置
        Map.of(
            "toolCodeName", "get_user_location",
            "displayName", "获取位置",
            "description", "根据 IP 地址获取用户的地理位置信息（精确到县级）",
            "inputSchema", Map.of(
                "type", "object",
                "properties", Map.of()
            )
        ),
        
        // 8. 天气查询
        Map.of(
            "toolCodeName", "weather_query",
            "displayName", "天气查询",
            "description", "查询全球任意城市的实时天气信息",
            "inputSchema", Map.of(
                "type", "object",
                "properties", Map.of(
                    "city", Map.of("type", "string", "description", "城市名称，如：北京、上海、New York")
                ),
                "required", List.of("city")
            )
        ),
        
        // 9. 产品搜索
        Map.of(
            "toolCodeName", "product_search",
            "displayName", "产品搜索",
            "description", "搜索产品信息，包括价格、库存、描述等",
            "inputSchema", Map.of(
                "type", "object",
                "properties", Map.of(
                    "productName", Map.of("type", "string", "description", "产品名称或关键词"),
                    "category", Map.of("type", "string", "description", "产品分类，可选"),
                    "maxResults", Map.of("type", "integer", "description", "最大返回数量，默认 5")
                ),
                "required", List.of("productName")
            )
        ),
        
        // 10. 联网搜索
        Map.of(
            "toolCodeName", "online_search",
            "displayName", "联网搜索",
            "description", "在互联网上搜索相关信息和资讯",
            "inputSchema", Map.of(
                "type", "object",
                "properties", Map.of(
                    "query", Map.of("type", "string", "description", "搜索关键词"),
                    "maxResults", Map.of("type", "integer", "description", "最大返回数量，默认 5")
                ),
                "required", List.of("query")
            )
        )
    );

    /**
     * 应用启动后自动执行初始化工具
     */
    @PostConstruct
    @Transactional(rollbackFor = Exception.class)
    public void init() {
        log.info("=== 开始初始化系统工具 ===");
        
        int createdCount = 0;
        int updatedCount = 0;
        int skippedCount = 0;
        
        try {
            for (Map<String, Object> config : SYSTEM_TOOLS_CONFIG) {
                String toolCodeName = (String) config.get("toolCodeName");
                
                // 查询数据库中是否已存在该工具
                Tools existingTool = findExistingTool(toolCodeName);

                // 工具已存在，检查是否需要更新
                if (shouldUpdate(existingTool, config)) {
                    updateTool(existingTool, config);
                    updatedCount++;
                } else {
                    log.debug("系统工具已存在且无需更新：{}", toolCodeName);
                    skippedCount++;
                }
            }
            
            log.info("=== 系统工具初始化完成 ===");
            log.info("新建工具：{} 个，更新工具：{} 个，跳过工具：{} 个", 
                    createdCount, updatedCount, skippedCount);
                    
        } catch (Exception e) {
            log.error("系统工具初始化失败", e);
            throw new RuntimeException("系统工具初始化失败：" + e.getMessage(), e);
        }
    }

    /**
     * 查找已存在的工具
     */
    private Tools findExistingTool(String toolCodeName) {
        List<Tools> systemTools = toolsDao.getSystemTools();
        return systemTools.stream()
                .filter(tool -> toolCodeName.equals(tool.getToolCodeName()))
                .findFirst()
                .orElse(null);
    }

    /**
     * 判断是否需要更新工具
     */
    private boolean shouldUpdate(Tools existingTool, Map<String, Object> config) {
        String displayName = (String) config.get("displayName");
        String description = (String) config.get("description");
        
        // 如果显示名或描述发生变化，则需要更新
        return !StrUtil.equals(existingTool.getDisplayName(), displayName) ||
               !StrUtil.equals(existingTool.getDescription(), description);
    }

    /**
     * 创建新工具
     */
    private void createTool(Map<String, Object> config) {
        log.info("创建系统工具：{}", config.get("toolCodeName"));
        
        Tools tool = buildToolFromConfig(config);
        tool.setCreatedAt(new Date());
        tool.setUpdatedAt(new Date());
        
        boolean saved = toolsDao.save(tool);
        if (!saved) {
            throw new RuntimeException("保存系统工具失败：" + config.get("toolCodeName"));
        }
        
        log.info("系统工具创建成功：{}, id: {}", 
                config.get("toolCodeName"), tool.getId());
    }

    /**
     * 更新已有工具
     */
    private void updateTool(Tools existingTool, Map<String, Object> config) {
        log.info("更新系统工具：{}", config.get("toolCodeName"));
        
        existingTool.setDisplayName((String) config.get("displayName"));
        existingTool.setDescription((String) config.get("description"));
        existingTool.setInputSchema(config.get("inputSchema"));
        existingTool.setIsEnabled(true);
        existingTool.setUpdatedAt(new Date());
        
        boolean updated = toolsDao.updateById(existingTool);
        if (!updated) {
            throw new RuntimeException("更新系统工具失败：" + config.get("toolCodeName"));
        }
        
        log.info("系统工具更新成功：{}", config.get("toolCodeName"));
    }

    /**
     * 从配置构建工具对象
     */
    private Tools buildToolFromConfig(Map<String, Object> config) {
        Tools tool = new Tools();
        tool.setOwnerId(null); // null 表示系统工具
        tool.setSourceType("SYSTEM");
        tool.setToolCodeName((String) config.get("toolCodeName"));
        tool.setDisplayName((String) config.get("displayName"));
        tool.setDescription((String) config.get("description"));
        tool.setInputSchema(config.get("inputSchema"));
        tool.setOutputSchema(null); // 系统工具通常不需要输出 schema
        tool.setCustomEndpointUrl(null); // 系统工具不需要 URL
        tool.setMcpServerId(null);
        tool.setIsEnabled(true);
        tool.setIsDelete(0);
        return tool;
    }
}
