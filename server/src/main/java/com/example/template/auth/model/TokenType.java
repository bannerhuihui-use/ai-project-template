package com.example.template.auth.model;

/**
 * JWT 令牌类型。
 *
 * <ul>
 *     <li>{@link #ACCESS}：访问令牌，短有效期，用于访问业务接口。</li>
 *     <li>{@link #REFRESH}：刷新令牌，长有效期，仅用于调用刷新接口换取新令牌。</li>
 * </ul>
 */
public enum TokenType {
    ACCESS,
    REFRESH
}
