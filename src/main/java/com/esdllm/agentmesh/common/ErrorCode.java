package com.esdllm.agentmesh.common;


import lombok.Getter;

/**
 * 错误码枚举
 */
@Getter
public enum ErrorCode {
    SUCCESS(0, "ok", ""),
    MODEL_NOT_FOUND(30002, "模型不存在", ""),
    PROVIDER_NOT_FOUND(30003, "模型服务提供商不存在", ""),
    PARAMS_ERROR(40000, "请求参数错误", ""),
    OPERATION_ERROR(40002, "操作失败", ""),
    NULL_ERROR(40001, "请求数据为空", ""),
    NOT_LOGIN(40100, "未登录", ""),
    NO_AUTH(40101, "无权限", ""),
    NOT_LOGIN_ERROR(40102, "未登录", ""),
    FORBIDDEN_ERROR(40300, "无权限操作", ""),
    NOT_FOUND_ERROR(40400, "请求数据不存在", ""),
    TOOL_NOT_FOUND(50004, "工具不存在", ""),
    TOOL_RUNTIME_ERROR(50005, "工具运行错误", ""),
    TOOL_NOT_FOUND_IN_AGENT(50007, "工具不存在", ""),
    MCP_SERVER_NOT_FOUND(51005, "MCP 服务不存在", ""),
    MCP_NOT_FOUND(51004, "MCP 服务不存在", ""),
    AGENT_NOT_FOUND(52006, "智能体不存在", ""),
    LIGHT_NOT_FOUND(53004, "轻量级模型调用错误", ""),
    MODEL_ERROR(54004, "模型调用错误", ""),
    SYSTEM_ERROR(-1, "系统内部异常", ""),
    ;

    private final int code;
    /**
     * 状态码信息
     */
    private final String message;
    /**
     * 状态码描述
     */
    private final String description;

    ErrorCode(int code, String message, String description) {
        this.code = code;
        this.message = message;
        this.description = description;
    }

}
