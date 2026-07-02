package com.example.template.auth.security;

import com.example.template.auth.config.AuthProperties;
import com.example.template.auth.model.TokenType;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * JWT 签发与校验（access / refresh 双令牌）。
 *
 * <p>安全要点：</p>
 * <ul>
 *     <li>对配置 secret 做 SHA-256 派生为 32 字节密钥，满足 HS256 对密钥长度的强制要求。</li>
 *     <li>payload 含 jti / userId / userType / identityType / tokenType / tokenVersion / roles(仅 access)
 *     / issuer / 过期时间；<b>不放</b>密码、openid、session_key、手机号明文等敏感信息。</li>
 *     <li>access token 用于访问接口；refresh token 仅用于刷新接口，由调用方校验 {@link TokenType}。</li>
 * </ul>
 */
@Component
public class JwtTokenProvider {

    private static final String CLAIM_USER_TYPE = "userType";
    private static final String CLAIM_IDENTITY_TYPE = "identityType";
    private static final String CLAIM_ROLES = "roles";
    private static final String CLAIM_TOKEN_TYPE = "tokenType";
    private static final String CLAIM_TOKEN_VERSION = "tokenVersion";

    private final SecretKey secretKey;
    private final String issuer;
    private final long accessExpireSeconds;
    private final long refreshExpireSeconds;

    public JwtTokenProvider(AuthProperties authProperties) {
        AuthProperties.Jwt jwt = authProperties.getJwt();
        this.secretKey = buildKey(jwt.getSecret());
        this.issuer = jwt.getIssuer();
        this.accessExpireSeconds = jwt.getAccessExpireSeconds();
        this.refreshExpireSeconds = jwt.getRefreshExpireSeconds();
    }

    /**
     * 签发 access token（含角色，用于访问接口）。
     *
     * @param userId       用户 ID
     * @param userType     用户类型
     * @param identityType 登录身份类型
     * @param roles        角色编码
     * @param tokenVersion 用户当前 token 版本
     * @return JWT 字符串
     */
    public String createAccessToken(Long userId, String userType, String identityType,
                                    List<String> roles, int tokenVersion) {
        return buildToken(userId, userType, identityType, roles, TokenType.ACCESS,
                tokenVersion, accessExpireSeconds);
    }

    /**
     * 签发 refresh token（不含角色，仅用于刷新接口）。
     *
     * @param userId       用户 ID
     * @param userType     用户类型
     * @param identityType 登录身份类型
     * @param tokenVersion 用户当前 token 版本
     * @return JWT 字符串
     */
    public String createRefreshToken(Long userId, String userType, String identityType, int tokenVersion) {
        return buildToken(userId, userType, identityType, null, TokenType.REFRESH,
                tokenVersion, refreshExpireSeconds);
    }

    /**
     * 校验并解析令牌（签名、issuer、过期）。
     *
     * @param token JWT 字符串
     * @return 解析结果
     * @throws JwtException 令牌无效或过期
     */
    @SuppressWarnings("unchecked")
    public ParsedToken parse(String token) {
        Claims claims = Jwts.parser()
                .requireIssuer(issuer)
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();

        Long userId = Long.valueOf(claims.getSubject());
        String userType = claims.get(CLAIM_USER_TYPE, String.class);
        String identityType = claims.get(CLAIM_IDENTITY_TYPE, String.class);
        List<String> roles = claims.get(CLAIM_ROLES, List.class);
        TokenType tokenType = TokenType.valueOf(claims.get(CLAIM_TOKEN_TYPE, String.class));
        Integer tokenVersion = claims.get(CLAIM_TOKEN_VERSION, Integer.class);
        Instant expiresAt = claims.getExpiration() == null ? null : claims.getExpiration().toInstant();

        return new ParsedToken(claims.getId(), userId, userType, identityType,
                roles == null ? List.of() : roles, tokenType, tokenVersion, expiresAt);
    }

    public long getAccessExpireSeconds() {
        return accessExpireSeconds;
    }

    public long getRefreshExpireSeconds() {
        return refreshExpireSeconds;
    }

    private String buildToken(Long userId, String userType, String identityType, List<String> roles,
                              TokenType tokenType, int tokenVersion, long expireSeconds) {
        Date now = new Date();
        Date expiration = new Date(now.getTime() + expireSeconds * 1000L);
        var builder = Jwts.builder()
                .id(UUID.randomUUID().toString())
                .issuer(issuer)
                .subject(String.valueOf(userId))
                .claim(CLAIM_USER_TYPE, userType)
                .claim(CLAIM_IDENTITY_TYPE, identityType)
                .claim(CLAIM_TOKEN_TYPE, tokenType.name())
                .claim(CLAIM_TOKEN_VERSION, tokenVersion)
                .issuedAt(now)
                .expiration(expiration);
        if (roles != null) {
            builder.claim(CLAIM_ROLES, roles);
        }
        return builder.signWith(secretKey).compact();
    }

    private SecretKey buildKey(String secret) {
        String raw = secret == null ? "" : secret;
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(raw.getBytes(StandardCharsets.UTF_8));
            return Keys.hmacShaKeyFor(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("初始化 JWT 密钥失败：缺少 SHA-256 算法", e);
        }
    }
}
