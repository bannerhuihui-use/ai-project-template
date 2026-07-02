package com.example.template.auth.model;

/**
 * 登录身份类型。新增登录方式必须在此扩展，并落到 user_identity 表。
 */
public enum IdentityType {
    /** 用户名 + 密码。 */
    USERNAME,
    /** 手机号（+ 密码或验证码）。 */
    PHONE,
    /** 邮箱（+ 密码）。 */
    EMAIL,
    /** 微信小程序（openid）。 */
    WECHAT_MINIAPP
}
