package com.example.template.auth.security;

import com.example.template.auth.config.AuthProperties;
import com.example.template.auth.model.TokenType;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * JwtTokenProvider 单元测试（不依赖数据库/Redis/Spring 容器）。
 */
class JwtTokenProviderTest {

    private JwtTokenProvider newProvider(String secret, long accessExpire, long refreshExpire) {
        AuthProperties properties = new AuthProperties();
        properties.getJwt().setSecret(secret);
        properties.getJwt().setIssuer("template-server");
        properties.getJwt().setAccessExpireSeconds(accessExpire);
        properties.getJwt().setRefreshExpireSeconds(refreshExpire);
        return new JwtTokenProvider(properties);
    }

    @Test
    void shouldCreateAndParseAccessToken() {
        JwtTokenProvider provider = newProvider("a-reasonably-long-secret-for-test", 7200, 604800);

        String token = provider.createAccessToken(1L, "ADMIN", "USERNAME", List.of("ADMIN", "OPERATOR"), 3);
        ParsedToken parsed = provider.parse(token);

        assertEquals(1L, parsed.userId());
        assertEquals("ADMIN", parsed.userType());
        assertEquals("USERNAME", parsed.identityType());
        assertEquals(List.of("ADMIN", "OPERATOR"), parsed.roles());
        assertEquals(TokenType.ACCESS, parsed.tokenType());
        assertEquals(3, parsed.tokenVersion());
        assertNotNull(parsed.jti(), "access token 应包含 jti");
        assertNotNull(parsed.expiresAt());
    }

    @Test
    void shouldCreateAndParseRefreshToken() {
        JwtTokenProvider provider = newProvider("a-reasonably-long-secret-for-test", 7200, 604800);

        String token = provider.createRefreshToken(2L, "ADMIN", "USERNAME", 0);
        ParsedToken parsed = provider.parse(token);

        assertEquals(2L, parsed.userId());
        assertEquals(TokenType.REFRESH, parsed.tokenType());
        assertTrue(parsed.roles().isEmpty(), "refresh token 不应携带 roles");
        assertNotNull(parsed.jti(), "refresh token 应包含 jti");
    }

    @Test
    void accessAndRefreshShouldHaveDifferentJti() {
        JwtTokenProvider provider = newProvider("a-reasonably-long-secret-for-test", 7200, 604800);

        String access = provider.createAccessToken(1L, "ADMIN", "USERNAME", List.of("ADMIN"), 0);
        String refresh = provider.createRefreshToken(1L, "ADMIN", "USERNAME", 0);

        assertTrue(!provider.parse(access).jti().equals(provider.parse(refresh).jti()),
                "不同令牌的 jti 应不同");
    }

    @Test
    void shouldWorkEvenWithShortSecret() {
        JwtTokenProvider provider = newProvider("short", 7200, 604800);
        String token = provider.createAccessToken(2L, "MEMBER", "WECHAT_MINIAPP", List.of(), 0);
        assertEquals(2L, provider.parse(token).userId());
    }

    @Test
    void shouldRejectTamperedToken() {
        JwtTokenProvider provider = newProvider("a-reasonably-long-secret-for-test", 7200, 604800);
        String token = provider.createAccessToken(1L, "ADMIN", "USERNAME", List.of("ADMIN"), 0);
        String tampered = token.substring(0, token.length() - 2) + "xx";

        assertThrows(JwtException.class, () -> provider.parse(tampered));
    }

    @Test
    void shouldRejectExpiredToken() throws InterruptedException {
        JwtTokenProvider provider = newProvider("a-reasonably-long-secret-for-test", 1, 1);
        String token = provider.createAccessToken(1L, "ADMIN", "USERNAME", List.of("ADMIN"), 0);

        Thread.sleep(1500L);

        assertThrows(JwtException.class, () -> provider.parse(token));
    }

    @Test
    void shouldRejectTokenSignedByDifferentSecret() {
        JwtTokenProvider issuer = newProvider("secret-one-aaaaaaaaaaaaaaaaaaaa", 7200, 604800);
        JwtTokenProvider verifier = newProvider("secret-two-bbbbbbbbbbbbbbbbbbbb", 7200, 604800);
        String token = issuer.createAccessToken(1L, "ADMIN", "USERNAME", List.of("ADMIN"), 0);

        assertThrows(JwtException.class, () -> verifier.parse(token));
    }
}
