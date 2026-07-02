package com.example.template.common;

/**
 * 统一响应码。业务可在此扩展，避免在代码中散落魔法值。
 */
public enum ResultCode {

    SUCCESS(0, "success"),
    PARAM_ERROR(40000, "参数错误"),
    AUTH_ROLE_PERMISSION_INVALID(40010, "角色权限参数非法"),
    AUTH_USERNAME_EXISTS(40011, "用户名已存在"),
    AUTH_CANNOT_OPERATE_SELF(40012, "不能对自己执行该操作"),
    AUTH_ROLE_CODE_EXISTS(40013, "角色编码已存在"),
    AUTH_ROLE_CODE_RESERVED(40014, "角色编码为系统保留，不可使用"),
    AUTH_PASSWORD_TOO_SHORT(40015, "密码长度不符合要求"),
    AUTH_PASSWORD_TOO_LONG(40016, "密码长度超过上限"),
    CONFIG_KEY_NOT_EDITABLE(40020, "该配置项不可编辑"),
    CONFIG_VALUE_INVALID(40021, "配置值格式非法"),
    CONFIG_KEY_EXISTS(40022, "配置键已存在"),
    CONFIG_NOT_DELETABLE(40023, "该配置项不可删除"),
    UNAUTHORIZED(40100, "未认证"),
    AUTH_BAD_CREDENTIALS(40101, "用户名或密码错误"),
    AUTH_ACCOUNT_DISABLED(40102, "账号已被禁用"),
    AUTH_TOKEN_INVALID(40103, "登录已失效，请重新登录"),
    AUTH_REFRESH_TOKEN_INVALID(40104, "刷新令牌无效或已过期，请重新登录"),
    AUTH_TOKEN_BLACKLISTED(40105, "登录已失效，请重新登录"),
    AUTH_TOKEN_TYPE_INVALID(40106, "令牌类型不正确"),
    AUTH_TOKEN_VERSION_EXPIRED(40107, "登录已失效，请重新登录"),
    AUTH_OLD_PASSWORD_INCORRECT(40108, "原密码错误"),
    AUTH_NEW_PASSWORD_SAME(40109, "新密码不能与原密码相同"),
    AUTH_CAPTCHA_INVALID(40110, "验证码错误或已过期"),
    FORBIDDEN(40300, "无权限"),
    AUTH_NOT_ADMIN(40301, "需要管理员权限"),
    AUTH_CANNOT_DISABLE_SELF(40302, "不能禁用自己"),
    AUTH_CANNOT_OPERATE_SUPER_ADMIN(40303, "不能对超级管理员执行该操作"),
    AUTH_SUPER_ADMIN_GRANT_DENIED(40304, "仅超级管理员可授予 SUPER_ADMIN 角色"),
    AUTH_LAST_SUPER_ADMIN(40305, "系统至少需保留一名超级管理员"),
    NOT_FOUND(40400, "资源不存在"),
    AUTH_ROLE_NOT_FOUND(40401, "角色不存在"),
    AUTH_PERMISSION_NOT_FOUND(40402, "权限不存在或已禁用"),
    CONFIG_NOT_FOUND(40403, "配置项不存在"),
    FILE_NOT_FOUND(40410, "文件不存在"),
    FILE_TYPE_NOT_ALLOWED(40030, "文件类型不允许"),
    FILE_SIZE_EXCEEDED(40031, "文件大小超出限制"),
    FILE_ACCESS_DENIED(40310, "无权访问该文件"),
    FILE_DELETE_DENIED(40311, "无权删除该文件"),
    FILE_STORAGE_CONFIG_INVALID(50020, "文件存储配置无效"),
    BUSINESS_ERROR(50000, "业务异常"),
    SYSTEM_ERROR(50001, "系统异常"),
    WECHAT_LOGIN_NOT_SUPPORTED(50010, "微信登录能力待接入 wechat-miniapp 模块"),
    AUTH_SERVICE_UNAVAILABLE(50301, "鉴权服务暂不可用，请稍后重试"),
    SYSTEM_MAINTENANCE(50302, "系统维护中，请稍后再试");

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
