package com.example.template.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 登录 / 刷新响应：access + refresh 双令牌 + 用户基本信息。
 */
@Schema(name = "LoginResponse", description = "登录/刷新响应")
public class LoginResponse {

    @Schema(description = "访问令牌（JWT，短有效期，用于访问接口）", example = "eyJhbGciOiJIUzI1NiJ9...")
    private String accessToken;

    @Schema(description = "刷新令牌（JWT，长有效期，仅用于刷新接口）", example = "eyJhbGciOiJIUzI1NiJ9...")
    private String refreshToken;

    @Schema(description = "令牌类型", example = "Bearer")
    private String tokenType;

    @Schema(description = "access token 有效期（秒）", example = "7200")
    private long accessExpireSeconds;

    @Schema(description = "refresh token 有效期（秒）", example = "604800")
    private long refreshExpireSeconds;

    @Schema(description = "用户基本信息")
    private AuthUserInfo user;

    public LoginResponse() {
    }

    public LoginResponse(String accessToken, String refreshToken, String tokenType,
                         long accessExpireSeconds, long refreshExpireSeconds, AuthUserInfo user) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.tokenType = tokenType;
        this.accessExpireSeconds = accessExpireSeconds;
        this.refreshExpireSeconds = refreshExpireSeconds;
        this.user = user;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }

    public String getTokenType() {
        return tokenType;
    }

    public void setTokenType(String tokenType) {
        this.tokenType = tokenType;
    }

    public long getAccessExpireSeconds() {
        return accessExpireSeconds;
    }

    public void setAccessExpireSeconds(long accessExpireSeconds) {
        this.accessExpireSeconds = accessExpireSeconds;
    }

    public long getRefreshExpireSeconds() {
        return refreshExpireSeconds;
    }

    public void setRefreshExpireSeconds(long refreshExpireSeconds) {
        this.refreshExpireSeconds = refreshExpireSeconds;
    }

    public AuthUserInfo getUser() {
        return user;
    }

    public void setUser(AuthUserInfo user) {
        this.user = user;
    }
}
