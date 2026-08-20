package com.esdllm.agentmesh.common;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 统一返回类
 * @param <T>
 */
@Data
public class BaseResponse<T> implements Serializable {
    @Serial
    private static final long serialVersionUID = 2148991111972060167L;

    private int code;

    private String message;

    private T data;

    private String description;

    public BaseResponse(int code, T data, String message, String description) {
        this.code = code;
        this.data = data;
        this.message = message;
        this.description = description;
    }
    public BaseResponse(int code, T data ,String message) {
        this.code = code;
        this.data = data;
        this.message = message;
        this.description = "";
    }
    public BaseResponse(int code, T data) {
        this(code, data, "");
    }
    public BaseResponse(ErrorCode errorCode) {
        this(errorCode.getCode(), null, errorCode.getMessage());
    }
    public BaseResponse(ErrorCode errorCode, T data) {
        this(errorCode.getCode(), data, errorCode.getMessage(), errorCode.getDescription());
    }
}
