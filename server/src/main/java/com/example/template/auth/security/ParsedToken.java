package com.example.template.auth.security;

import com.example.template.auth.model.TokenType;

import java.time.Instant;
import java.util.List;

/**
 * 解析后的 JWT 载荷（签名、issuer、过期已校验通过）。
 *
 * @param jti          令牌唯一 ID
 * @param userId       用户 ID
 * @param userType     用户类型
 * @param identityType 登录身份类型
 * @param roles        角色编码（refresh token 可能为空）
 * @param tokenType    令牌类型 ACCESS / REFRESH
 * @param tokenVersion 签发时的用户 token 版本
 * @param expiresAt    过期时间
 */
public record ParsedToken(
        String jti,
        Long userId,
        String userType,
        String identityType,
        List<String> roles,
        TokenType tokenType,
        Integer tokenVersion,
        Instant expiresAt) {

    /**
     * 转换为安全上下文 principal，携带 jti / tokenVersion / expiresAt。
     *
     * @return 当前用户上下文
     */
    public CurrentUser toCurrentUser() {
        return new CurrentUser(userId, userType, identityType, roles, jti, tokenVersion, expiresAt);
    }
}
