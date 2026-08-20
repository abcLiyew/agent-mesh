package com.esdllm.agentmesh.model.dto.request;

import com.esdllm.agentmesh.model.dto.tool.ToolSchemaConfig;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;

/**
 * 添加智能体请求
 */
@Data
public class AgentAddRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 智能体名称
     */
    private String name;

    /**
     * 智能体简介
     */
    private String description;

    /**
     * 头像 URL
     */
    private String avatarUrl;

    /**
     * 系统提示词：定义智能体核心行为和角色的 Prompt
     */
    private String systemPrompt;

    /**
     * 角色定义补充：额外的角色设定描述
     */
    private String roleDefinition;

    /**
     * 决策模型 ID：负责思考、规划、调用工具的模型 (高智力模型)
     */
    private Long decisionModelId;

    /**
     * 回复模型 ID：负责最终生成文本的模型 (可是低成本模型)
     */
    private Long responseModelId;

    /**
     * 是否启用工具：false 表示该智能体不使用任何工具
     */
    private Boolean isToolEnabled;

    /**
     * 工具配置覆写：智能体级别的特定工具参数配置
     *
     * 前端传递 JSON 字符串格式示例：
     * <pre>
     * {
     *   "tools": [
     *     {
     *       "name": "web_search",
     *       "displayName": "网络搜索",
     *       "description": "实时搜索互联网信息",
     *       "enabled": true,
     *       "priority": 10,
     *       "parameters": {
     *         "type": "object",
     *         "required": ["query"],
     *         "properties": {
     *           "query": {
     *             "type": "string",
     *             "description": "搜索关键词",
     *             "minLength": 1,
     *             "maxLength": 500
     *           },
     *           "limit": {
     *             "type": "integer",
     *             "description": "返回结果数量",
     *             "default": 10,
     *             "minimum": 1,
     *             "maximum": 50
     *           }
     *         }
     *       },
     *       "invocationConfig": {
     *         "method": "HTTP",
     *         "url": "/api/tools/search",
     *         "httpMethod": "POST",
     *         "timeoutMs": 10000,
     *         "retryTimes": 2
     *       }
     *     }
     *   ],
     *   "globalConfig": {
     *     "defaultTimeoutMs": 30000,
     *     "allowParallelExecution": true,
     *     "maxConcurrentCalls": 5
     *   }
     * }
     * </pre>
     */
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private ToolSchemaConfig toolSchemaJson;

    /**
     * 配置版本号：用于版本管理或回滚
     */
    private String version = "1.0";

    /**
     * 智能体状态：1=发布，0=草稿/停用
     */
    private Integer status = 1;
}
