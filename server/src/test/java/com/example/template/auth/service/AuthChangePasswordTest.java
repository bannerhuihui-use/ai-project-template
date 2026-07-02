package com.example.template.auth.service;

import com.example.template.auth.config.AuthProperties;
import com.example.template.auth.dto.ChangePasswordRequest;
import com.example.template.auth.dto.LoginResponse;
import com.example.template.auth.model.SysUser;
import com.example.template.auth.model.UserIdentity;
import com.example.template.auth.repository.LoginDeviceRepository;
import com.example.template.auth.repository.SysRoleRepository;
import com.example.template.auth.repository.SysUserRepository;
import com.example.template.auth.repository.UserIdentityRepository;
import com.example.template.auth.security.CurrentUser;
import com.example.template.auth.security.JwtTokenProvider;
import com.example.template.auth.security.TokenBlacklistService;
import com.example.template.auth.service.impl.AuthServiceImpl;
import com.example.template.common.BusinessException;
import com.example.template.common.ResultCode;
import com.example.template.testsupport.AuthTestSupport;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * AuthServiceImpl.changePassword 单元测试（手写 fake，不连 Redis/PostgreSQL）。
 *
 * <p>覆盖：改密成功（token_version 自增 + 续签）、原密码错误、新密码与原密码相同。</p>
 */
class AuthChangePasswordTest {

    private static final String SECRET = "a-reasonably-long-secret-for-test";
    private static final String OLD_PASSWORD = "oldPass123";

    private final PasswordEncoder encoder = new BCryptPasswordEncoder();

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    private void login(Long userId) {
        CurrentUser principal = new CurrentUser(userId, "ADMIN", "USERNAME", List.of("ADMIN"),
                "jti-1", 0, null);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, List.of()));
    }

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

    private UserIdentity identityWithPassword(String rawPassword) {
        UserIdentity identity = new UserIdentity();
        identity.setId(10L);
        identity.setUserId(1L);
        identity.setIdentityType("USERNAME");
        identity.setIdentifier("admin");
        identity.setCredential(encoder.encode(rawPassword));
        identity.setStatus("NORMAL");
        identity.setDeleted(0);
        return identity;
    }

    private AuthServiceImpl service(SysUser user, UserIdentity identity, int[] increments, String[] newCredential) {
        UserIdentityRepository identityRepo = new UserIdentityRepository(null) {
            @Override
            public Optional<UserIdentity> findActiveByUserIdAndType(Long userId, String identityType) {
                return Optional.of(identity);
            }

            @Override
            public int updateCredentialByUserIdAndType(Long userId, String identityType, String credential) {
                newCredential[0] = credential;
                identity.setCredential(credential);
                return 1;
            }
        };
        SysUserRepository userRepo = new SysUserRepository(null) {
            @Override
            public Optional<SysUser> findActiveById(Long id) {
                return Optional.of(user);
            }

            @Override
            public int updatePasswordUpdatedAt(Long id) {
                return 1;
            }

            @Override
            public int incrementTokenVersion(Long id) {
                increments[0]++;
                user.setTokenVersion(user.getTokenVersion() + 1);
                return 1;
            }
        };
        SysRoleRepository roleRepo = new SysRoleRepository(null) {
            @Override
            public List<String> findRoleCodesByUserId(Long userId) {
                return List.of("ADMIN");
            }
        };
        return new AuthServiceImpl(identityRepo, userRepo, roleRepo,
                new LoginDeviceRepository(null), tokenProvider(), disabledBlacklist(), encoder,
                AuthTestSupport.defaultSystemConfig(), AuthTestSupport.noopCaptcha());
    }

    private TokenBlacklistService disabledBlacklist() {
        return new TokenBlacklistService() {
            @Override
            public void blacklist(String jti, long ttlSeconds) {
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
    }

    @Test
    void changePassword_success_shouldBumpVersionAndReissueTokens() {
        login(1L);
        int[] increments = {0};
        String[] newCredential = {null};
        AuthServiceImpl service = service(normalAdmin(), identityWithPassword(OLD_PASSWORD), increments, newCredential);

        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setOldPassword(OLD_PASSWORD);
        request.setNewPassword("brandNewPass456");

        LoginResponse resp = service.changePassword(request);

        assertEquals(1, increments[0], "改密应触发 token_version 自增一次");
        assertTrue(encoder.matches("brandNewPass456", newCredential[0]), "新凭证应为新密码的 BCrypt 密文");
        assertNotNull(resp.getAccessToken(), "改密成功应续签新 access");
        assertNotNull(resp.getRefreshToken(), "改密成功应续签新 refresh");
    }

    @Test
    void changePassword_wrongOldPassword_shouldFail() {
        login(1L);
        AuthServiceImpl service = service(normalAdmin(), identityWithPassword(OLD_PASSWORD), new int[]{0}, new String[]{null});

        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setOldPassword("wrongOld");
        request.setNewPassword("brandNewPass456");

        BusinessException ex = assertThrows(BusinessException.class, () -> service.changePassword(request));
        assertEquals(ResultCode.AUTH_OLD_PASSWORD_INCORRECT.getCode(), ex.getCode());
    }

    @Test
    void changePassword_sameAsOld_shouldFail() {
        login(1L);
        AuthServiceImpl service = service(normalAdmin(), identityWithPassword(OLD_PASSWORD), new int[]{0}, new String[]{null});

        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setOldPassword(OLD_PASSWORD);
        request.setNewPassword(OLD_PASSWORD);

        BusinessException ex = assertThrows(BusinessException.class, () -> service.changePassword(request));
        assertEquals(ResultCode.AUTH_NEW_PASSWORD_SAME.getCode(), ex.getCode());
    }
}
