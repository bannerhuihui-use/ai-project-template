package com.example.template.common;

/**
 * 统一响应码。业务可在此扩展，避免在代码中散落魔法值。
 */
public enum ResultCode {

    SUCCESS(0, "success"),
    PARAM_ERROR(40000, "参数错误"),
    UNAUTHORIZED(40100, "未认证"),
    FORBIDDEN(40300, "无权限"),
    NOT_FOUND(40400, "资源不存在"),
    BUSINESS_ERROR(50000, "业务异常"),
    SYSTEM_ERROR(50001, "系统异常");

    private final int code;
    private final String message;

    ResultCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}
