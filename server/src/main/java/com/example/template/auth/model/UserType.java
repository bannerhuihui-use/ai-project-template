package com.example.template.auth.model;

/**
 * 用户类型。决定接口保护粒度（映射为 Spring 权限 ROLE_ADMIN / ROLE_MEMBER）。
 */
public enum UserType {
    /** 后台管理员。 */
    ADMIN,
    /** C 端会员（微信小程序等）。 */
    MEMBER
}
