package com.example.template.common;

import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serializable;

/**
 * 统一返回结构。
 *
 * @param <T> 数据类型
 */
@Schema(name = "Result", description = "统一返回结构")
public class Result<T> implements Serializable {

    @Schema(description = "响应码，0 表示成功，非 0 为错误码", example = "0", requiredMode = Schema.RequiredMode.REQUIRED)
    private int code;

    @Schema(description = "响应描述信息，成功为 success，失败为错误说明", example = "success", requiredMode = Schema.RequiredMode.REQUIRED)
    private String message;

    @Schema(description = "业务数据，成功时返回，失败时通常为 null")
    private T data;

    public Result() {
    }

    public Result(int code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    public static <T> Result<T> success() {
        return new Result<>(ResultCode.SUCCESS.getCode(), ResultCode.SUCCESS.getMessage(), null);
    }

    public static <T> Result<T> success(T data) {
        return new Result<>(ResultCode.SUCCESS.getCode(), ResultCode.SUCCESS.getMessage(), data);
    }

    public static <T> Result<T> error(ResultCode resultCode) {
        return new Result<>(resultCode.getCode(), resultCode.getMessage(), null);
    }

    public static <T> Result<T> error(int code, String message) {
        return new Result<>(code, message, null);
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }
}
