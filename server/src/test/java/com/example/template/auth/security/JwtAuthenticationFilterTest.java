package com.example.template.auth.security;

import com.example.template.auth.config.AuthProperties;
import com.example.template.auth.model.SysUser;
import com.example.template.auth.repository.PermissionRepository;
import com.example.template.auth.repository.SysUserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * JwtAuthenticationFilter 单元测试。
 *
 * <p>使用手写 fake/stub（不依赖 Mockito inline agent，也不连真实 Redis/PostgreSQL），
 * 覆盖：ACCESS 通过、REFRESH 被拒、tokenVersion 不一致被拒、黑名单被拒。</p>
 */
class JwtAuthenticationFilterTest {

    private static final String SECRET = "a-reasonably-long-secret-for-test";

    private final JwtTokenProvider tokenProvider = newProvider();

    private JwtTokenProvider newProvider() {
        AuthProperties properties = new AuthProperties();
        properties.getJwt().setSecret(SECRET);
        properties.getJwt().setIssuer("template-server");
        properties.getJwt().setAccessExpireSeconds(7200);
        properties.getJwt().setRefreshExpireSeconds(604800);
        return new JwtTokenProvider(properties);
    }

    /** 手写 SysUserRepository stub：返回预置用户，不连库。 */
    private static SysUserRepository userRepoReturning(SysUser user) {
        return new SysUserRepository(null) {
            @Override
            public Optional<SysUser> findActiveById(Long id) {
                return Optional.ofNullable(user);
            }
        };
    }

    /** 手写 TokenBlacklistService stub：可配置启用与是否命中黑名单。 */
    private static TokenBlacklistService blacklist(boolean enabled, boolean hit) {
        return new TokenBlacklistService() {
            @Override
            public void blacklist(String jti, long ttlSeconds) {
                // no-op
            }

            @Override
            public boolean isBlacklisted(String jti) {
                return hit;
            }

            @Override
            public boolean isEnabled() {
                return enabled;
            }
        };
    }

    /** 手写 PermissionRepository stub：不连库，按钮/接口权限码返回空。 */
    private static PermissionRepository emptyPermissionRepo() {
        return new PermissionRepository(null) {
            @Override
            public List<String> findButtonApiCodesByUserId(Long userId) {
                return List.of();
            }
        };
    }

    private JwtAuthenticationFilter newFilter(SysUserRepository userRepo, TokenBlacklistService blacklist) {
        PermissionRepository permRepo = emptyPermissionRepo();
        // 空白名单：确保过滤器实际执行鉴权逻辑
        return new JwtAuthenticationFilter(tokenProvider, userRepo,
                new UserPermissionCacheService(permRepo), blacklist,
                new SecurityResponseWriter(new ObjectMapper()), new AuthProperties());
    }

    private SysUser userWithVersion(int tokenVersion) {
        SysUser user = new SysUser();
        user.setId(1L);
        user.setUserType("ADMIN");
        user.setNickname("管理员");
        user.setStatus("NORMAL");
        user.setTokenVersion(tokenVersion);
        user.setDeleted(0);
        return user;
    }

    private MockHttpServletResponse doFilter(JwtAuthenticationFilter filter, String token) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/auth/me");
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer " + token);
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request, response, new MockFilterChain());
        return response;
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldAuthenticateValidAccessToken() throws Exception {
        JwtAuthenticationFilter filter = newFilter(userRepoReturning(userWithVersion(0)), blacklist(true, false));
        String access = tokenProvider.createAccessToken(1L, "ADMIN", "USERNAME", List.of("ADMIN"), 0);

        MockHttpServletResponse response = doFilter(filter, access);

        assertEquals(HttpServletResponse.SC_OK, response.getStatus());
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertNotNull(auth, "有效 access token 应写入 SecurityContext");
        assertTrue(auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN")));
    }

    @Test
    void shouldRejectRefreshTokenOnNormalEndpoint() throws Exception {
        JwtAuthenticationFilter filter = newFilter(userRepoReturning(userWithVersion(0)), blacklist(true, false));
        String refresh = tokenProvider.createRefreshToken(1L, "ADMIN", "USERNAME", 0);

        MockHttpServletResponse response = doFilter(filter, refresh);

        assertEquals(HttpServletResponse.SC_UNAUTHORIZED, response.getStatus());
        assertTrue(response.getContentAsString().contains("40106"), "refresh 访问普通接口应返回 40106");
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void shouldRejectWhenTokenVersionMismatch() throws Exception {
        // 库里版本已自增为 1，令牌签发时为 0 -> 不一致
        JwtAuthenticationFilter filter = newFilter(userRepoReturning(userWithVersion(1)), blacklist(true, false));
        String access = tokenProvider.createAccessToken(1L, "ADMIN", "USERNAME", List.of("ADMIN"), 0);

        MockHttpServletResponse response = doFilter(filter, access);

        assertEquals(HttpServletResponse.SC_UNAUTHORIZED, response.getStatus());
        assertTrue(response.getContentAsString().contains("40107"), "版本不一致应返回 40107");
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void shouldRejectBlacklistedToken() throws Exception {
        JwtAuthenticationFilter filter = newFilter(userRepoReturning(userWithVersion(0)), blacklist(true, true));
        String access = tokenProvider.createAccessToken(1L, "ADMIN", "USERNAME", List.of("ADMIN"), 0);

        MockHttpServletResponse response = doFilter(filter, access);

        assertEquals(HttpServletResponse.SC_UNAUTHORIZED, response.getStatus());
        assertTrue(response.getContentAsString().contains("40105"), "黑名单令牌应返回 40105");
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }
}
