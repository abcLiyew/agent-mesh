package com.esdllm.agentmesh.model.dto.tool;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * 工具 Schema 配置对象
 * 用于智能体级别的工具参数自定义配置
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true) // 忽略未知字段，向前兼容
public class ToolSchemaConfig implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 工具列表
     */
    private List<ToolDefinition> tools;

    /**
     * 全局工具配置
     */
    private GlobalToolConfig globalConfig;

    /**
     * 工具定义
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ToolDefinition implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        /**
         * 工具唯一标识（与工具注册表中的 key 对应）
         */
        private String name;

        /**
         * 工具显示名称
         */
        private String displayName;

        /**
         * 工具描述（可覆盖默认描述）
         */
        private String description;

        /**
         * 是否启用该工具
         */
        private Boolean enabled = true;

        /**
         * 工具优先级（数值越小优先级越高）
         */
        private Integer priority = 100;

        /**
         * 工具图标 URL（可覆盖默认图标）
         */
        private String iconUrl;

        /**
         * 参数配置
         */
        private ToolParameters parameters;

        /**
         * 调用配置
         */
        private InvocationConfig invocationConfig;

        /**
         * 高级配置
         */
        private AdvancedConfig advancedConfig;
    }

    /**
     * 工具参数定义
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ToolParameters implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        /**
         * 参数类型：object, array 等
         */
        private String type = "object";

        /**
         * 必需参数列表
         */
        private List<String> required;

        /**
         * 参数属性定义
         */
        private Map<String, ParameterProperty> properties;
    }

    /**
     * 参数属性
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ParameterProperty implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        /**
         * 参数类型：string, integer, number, boolean, array, object
         */
        private String type;

        /**
         * 参数描述
         */
        private String description;

        /**
         * 默认值
         */
        private Object defaultValue;

        /**
         * 枚举值（当 type 为 string 时有效）
         */
        private List<Object> enumValues;

        /**
         * 最小值（数字类型有效）
         */
        private Number minimum;

        /**
         * 最大值（数字类型有效）
         */
        private Number maximum;

        /**
         * 最小长度（字符串/数组有效）
         */
        private Integer minLength;

        /**
         * 最大长度（字符串/数组有效）
         */
        private Integer maxLength;

        /**
         * 正则表达式模式（字符串有效）
         */
        private String pattern;

        /**
         * 子项属性（当 type 为 object 或 array 时有效）
         */
        private Object items;
    }

    /**
     * 调用配置
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class InvocationConfig implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        /**
         * 调用方式：HTTP, RPC, FUNCTION, MCP
         */
        private String method = "HTTP";

        /**
         * 调用地址或函数名
         */
        private String url;

        /**
         * HTTP 方法：GET, POST, PUT, DELETE 等
         */
        private String httpMethod = "POST";

        /**
         * 请求头配置
         */
        private Map<String, String> headers;

        /**
         * 超时时间（毫秒）
         */
        private Long timeoutMs = 30000L;

        /**
         * 重试次数
         */
        private Integer retryTimes = 3;
    }

    /**
     * 高级配置
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AdvancedConfig implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        /**
         * 是否需要用户确认
         */
        private Boolean requireConfirmation = false;

        /**
         * 确认提示语
         */
        private String confirmationMessage;

        /**
         * 是否记录详细日志
         */
        private Boolean enableDetailedLog = true;

        /**
         * 缓存配置
         */
        private CacheConfig cacheConfig;

        /**
         * 限流配置
         */
        private RateLimitConfig rateLimitConfig;
    }

    /**
     * 缓存配置
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CacheConfig implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        /**
         * 是否启用缓存
         */
        private Boolean enabled = false;

        /**
         * 缓存过期时间（秒）
         */
        private Long ttlSeconds = 3600L;

        /**
         * 缓存键生成策略
         */
        private String keyStrategy;
    }

    /**
     * 限流配置
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RateLimitConfig implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        /**
         * 是否启用限流
         */
        private Boolean enabled = false;

        /**
         * 每分钟最大调用次数
         */
        private Integer maxCallsPerMinute = 60;

        /**
         * 每天最大调用次数
         */
        private Integer maxCallsPerDay = 10000;
    }

    /**
     * 全局工具配置
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GlobalToolConfig implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        /**
         * 默认超时时间（毫秒）
         */
        private Long defaultTimeoutMs = 30000L;

        /**
         * 默认重试次数
         */
        private Integer defaultRetryTimes = 3;

        /**
         * 是否允许并行调用工具
         */
        private Boolean allowParallelExecution = true;

        /**
         * 最大并发工具调用数
         */
        private Integer maxConcurrentCalls = 5;

        /**
         * 工具调用失败时的处理策略：FAIL_FAST, CONTINUE, RETRY
         */
        private String failureStrategy = "CONTINUE";
    }
}
