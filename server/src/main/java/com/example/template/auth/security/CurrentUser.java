package com.example.template.auth.security;

import java.io.Serializable;
import java.time.Instant;
import java.util.Collections;
import java.util.List;

/**
 * 当前登录用户上下文。作为 Spring Security 的 Authentication principal，
 * 也用于 Controller/Service 获取当前用户。仅含非敏感信息。
 *
 * <p>除身份信息外，还携带当前 access token 的 {@code jti}、{@code tokenVersion}、
 * {@code expiresAt}，供退出登录拉黑当前令牌与 token 版本比对使用。</p>
 */
public class CurrentUser implements Serializable {

    private final Long userId;
    private final String userType;
    private final String identityType;
    private final List<String> roles;
    /** 当前 access token 的唯一 ID（jti）；登录签发前的上下文可为 null。 */
    private final String jti;
    /** 令牌签发时的用户 token 版本。 */
    private final Integer tokenVersion;
    /** 当前 access token 的过期时间；用于计算黑名单 TTL。 */
    private final Instant expiresAt;

    public CurrentUser(Long userId, String userType, String identityType, List<String> roles) {
        this(userId, userType, identityType, roles, null, null, null);
    }

    public CurrentUser(Long userId, String userType, String identityType, List<String> roles,
                       String jti, Integer tokenVersion, Instant expiresAt) {
        this.userId = userId;
        this.userType = userType;
        this.identityType = identityType;
        this.roles = roles == null ? Collections.emptyList() : List.copyOf(roles);
        this.jti = jti;
        this.tokenVersion = tokenVersion;
        this.expiresAt = expiresAt;
    }

    public Long getUserId() {
        return userId;
    }

    public String getUserType() {
        return userType;
    }

    public String getIdentityType() {
        return identityType;
    }

    public List<String> getRoles() {
        return roles;
    }

    public String getJti() {
        return jti;
    }

    public Integer getTokenVersion() {
        return tokenVersion;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }
}
