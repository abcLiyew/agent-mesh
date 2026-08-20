package com.esdllm.agentmesh.common;

/**
 * 创建返回结果
 */
public class ResultUtils {
    /**
     * 成功
     *
     * @param data 数据
     * @param <T> 泛型
     * @return {@link BaseResponse}<{@link T}>
     */
    public static <T> BaseResponse<T> success(T data) {
        return new BaseResponse<>(ErrorCode.SUCCESS, data);
    }
    /**
     * 失败
     *
     * @param errorCode 错误代码
     * @param <T> 泛型
     * @return {@link BaseResponse}<{@link T}>
     */
    public static <T> BaseResponse<T> error(ErrorCode errorCode) {
        return new BaseResponse<>(errorCode);
    }

    /**
     * 失败
     * @param errorCode
     * @param message
     * @param description
     * @return
     * @param <T>
     */
    public static <T> BaseResponse<T> error(ErrorCode errorCode,String message,String description) {
        return new BaseResponse<>(errorCode.getCode(),null,message,description);
    }
    /**
     * 失败
     * @param errorCode
     * @param description
     * @return
     * @param <T>
     */
    public static <T> BaseResponse<T> error(ErrorCode errorCode,String description) {
        return new BaseResponse<>(errorCode.getCode(),null,errorCode.getMessage(),description);
    }
    /**
     * 失败
     * @param Code
     * @param message
     * @param description
     * @return
     * @param <T>
     */
    public static <T> BaseResponse<T> error(int Code,String message,String description) {
        return new BaseResponse<>(Code,null,message,description);
    }
}
