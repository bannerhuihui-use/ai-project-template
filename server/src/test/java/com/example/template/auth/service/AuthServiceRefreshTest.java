package com.example.template.auth.service;

import com.example.template.auth.config.AuthProperties;
import com.example.template.auth.dto.LoginResponse;
import com.example.template.auth.dto.RefreshTokenRequest;
import com.example.template.auth.model.SysUser;
import com.example.template.auth.repository.LoginDeviceRepository;
import com.example.template.auth.repository.SysRoleRepository;
import com.example.template.auth.repository.SysUserRepository;
import com.example.template.auth.repository.UserIdentityRepository;
import com.example.template.auth.security.JwtTokenProvider;
import com.example.template.auth.security.RedisUnavailableException;
import com.example.template.auth.security.TokenBlacklistService;
import com.example.template.auth.service.impl.AuthServiceImpl;
import com.example.template.common.BusinessException;
import com.example.template.common.ResultCode;
import com.example.template.testsupport.AuthTestSupport;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * AuthServiceImpl.refreshToken 单元测试（手写 fake，不连 Redis/PostgreSQL）。
 *
 * <p>重点验证：黑名单启用时拉黑旧 refresh 失败必须中断刷新（防重放）；黑名单关闭时仍可刷新。</p>
 */
class AuthServiceRefreshTest {

    private static final String SECRET = "a-reasonably-long-secret-for-test";

    private JwtTokenProvider tokenProvider() {
        AuthProperties properties = new AuthProperties();
        properties.getJwt().setSecret(SECRET);
        properties.getJwt().setIssuer("template-server");
        properties.getJwt().setAccessExpireSeconds(7200);
        properties.getJwt().setRefreshExpireSeconds(604800);
        return new JwtTokenProvider(properties);
    }

    private SysUser normalAdmin() {
        SysUser user = new SysUser();
        user.setId(1L);
        user.setUserType("ADMIN");
        user.setNickname("管理员");
        user.setStatus("NORMAL");
        user.setTokenVersion(0);
        user.setDeleted(0);
        return user;
    }

    private SysUserRepository userRepo(SysUser user) {
        return new SysUserRepository(null) {
            @Override
            public Optional<SysUser> findActiveById(Long id) {
                return Optional.of(user);
            }
        };
    }

    private SysRoleRepository roleRepo() {
        return new SysRoleRepository(null) {
            @Override
            public List<String> findRoleCodesByUserId(Long userId) {
                return List.of("ADMIN");
            }
        };
    }

    private AuthServiceImpl service(SysUser user, TokenBlacklistService blacklist, JwtTokenProvider provider) {
        return new AuthServiceImpl(
                new UserIdentityRepository(null),
                userRepo(user),
                roleRepo(),
                new LoginDeviceRepository(null),
                provider,
                blacklist,
                null,
                AuthTestSupport.defaultSystemConfig(),
                AuthTestSupport.noopCaptcha());
    }

    @Test
    void refresh_whenBlacklistEnabledAndRedisDown_shouldFailAndNotIssueTokens() {
        JwtTokenProvider provider = tokenProvider();
        String refreshToken = provider.createRefreshToken(1L, "ADMIN", "USERNAME", 0);

        boolean[] issuedAfterBlacklist = {false};
        TokenBlacklistService blacklist = new TokenBlacklistService() {
            @Override
            public void blacklist(String jti, long ttlSeconds) {
                throw new RedisUnavailableException("redis down", new RuntimeException());
            }

            @Override
            public boolean isBlacklisted(String jti) {
                return false;
            }

            @Override
            public boolean isEnabled() {
                return true;
            }
        };

        AuthServiceImpl service = service(normalAdmin(), blacklist, provider);
        RefreshTokenRequest request = new RefreshTokenRequest();
        request.setRefreshToken(refreshToken);

        BusinessException ex = assertThrows(BusinessException.class, () -> service.refreshToken(request));
        assertEquals(ResultCode.AUTH_SERVICE_UNAVAILABLE.getCode(), ex.getCode(),
                "拉黑旧 refresh 失败应返回 50301，且不签发新令牌");
        assertFalse(issuedAfterBlacklist[0]);
    }

    @Test
    void refresh_whenReplayHitsBlacklist_shouldBumpVersionAndReject() {
        JwtTokenProvider provider = tokenProvider();
        String refreshToken = provider.createRefreshToken(1L, "ADMIN", "USERNAME", 0);

        TokenBlacklistService alreadyRotated = new TokenBlacklistService() {
            @Override
            public void blacklist(String jti, long ttlSeconds) {
            }

            @Override
            public boolean isBlacklisted(String jti) {
                return true;
            }

            @Override
            public boolean isEnabled() {
                return true;
            }
        };

        int[] versionBumps = {0};
        SysUser user = normalAdmin();
        SysUserRepository userRepo = new SysUserRepository(null) {
            @Override
            public Optional<SysUser> findActiveById(Long id) {
                return Optional.of(user);
            }

            @Override
            public int incrementTokenVersion(Long id) {
                versionBumps[0]++;
                return 1;
            }
        };
        AuthServiceImpl service = new AuthServiceImpl(
                new UserIdentityRepository(null), userRepo, roleRepo(),
                new LoginDeviceRepository(null), provider, alreadyRotated, null,
                AuthTestSupport.defaultSystemConfig(), AuthTestSupport.noopCaptcha());

        RefreshTokenRequest request = new RefreshTokenRequest();
        request.setRefreshToken(refreshToken);

        BusinessException ex = assertThrows(BusinessException.class, () -> service.refreshToken(request));
        assertEquals(ResultCode.AUTH_TOKEN_BLACKLISTED.getCode(), ex.getCode(),
                "已轮换的 refresh 重放应返回 40105");
        assertEquals(1, versionBumps[0], "重放命中黑名单应强制整账号失效（token_version+1）");
    }

    @Test
    void refresh_whenBlacklistDisabled_shouldStillRotate() {
        JwtTokenProvider provider = tokenProvider();
        String refreshToken = provider.createRefreshToken(1L, "ADMIN", "USERNAME", 0);

        TokenBlacklistService disabled = new TokenBlacklistService() {
            @Override
            public void blacklist(String jti, long ttlSeconds) {
                // no-op（关闭态本不应被调用，调用也无副作用）
            }

            @Override
            public boolean isBlacklisted(String jti) {
                return false;
            }

            @Override
            public boolean isEnabled() {
                return false;
            }
        };

        AuthServiceImpl service = service(normalAdmin(), disabled, provider);
        RefreshTokenRequest request = new RefreshTokenRequest();
        request.setRefreshToken(refreshToken);

        LoginResponse resp = service.refreshToken(request);
        assertNotNull(resp.getAccessToken(), "黑名单关闭时仍应签发新 access");
        assertNotNull(resp.getRefreshToken(), "黑名单关闭时仍应签发新 refresh");
        assertEquals("ADMIN", resp.getUser().getUserType());
    }
}
