package com.example.template.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 退出登录请求（可选体）。
 *
 * <p>当前 access token 通过 Authorization 头携带、必拉黑；若同时传 refreshToken，
 * 则一并拉黑，确保退出后 access + refresh 均不可用。</p>
 */
@Schema(name = "LogoutRequest", description = "退出登录请求（可选）")
public class LogoutRequest {

    @Schema(description = "需要一并失效的刷新令牌（可选）", example = "eyJhbGciOiJIUzI1NiJ9...")
    private String refreshToken;

    public String getRefreshToken() {
        return refreshToken;
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }
}
