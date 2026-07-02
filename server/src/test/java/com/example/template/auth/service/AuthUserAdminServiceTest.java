package com.example.template.auth.service;

import com.example.template.auth.dto.RoleItem;
import com.example.template.auth.dto.UserAdminItem;
import com.example.template.auth.dto.UserCreateRequest;
import com.example.template.auth.dto.UserUpdateRequest;
import com.example.template.auth.model.IdentityType;
import com.example.template.auth.model.SysUser;
import com.example.template.auth.model.UserIdentity;
import com.example.template.auth.repository.LoginDeviceRepository;
import com.example.template.auth.repository.SysRoleRepository;
import com.example.template.auth.repository.SysUserRepository;
import com.example.template.auth.repository.UserIdentityRepository;
import com.example.template.auth.security.CurrentUser;
import com.example.template.auth.service.impl.AuthUserAdminServiceImpl;
import com.example.template.auth.support.SuperAdminGuard;
import com.example.template.common.BusinessException;
import com.example.template.common.ResultCode;
import com.example.template.testsupport.AuthTestSupport;
import com.example.template.testsupport.RbacTestSupport;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * AuthUserAdminServiceImpl 单元测试（手写 fake，不连 PostgreSQL）。
 */
class AuthUserAdminServiceTest {

    private static final Long OPERATOR_ID = 99L;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    private void loginAsOperator() {
        CurrentUser principal = new CurrentUser(OPERATOR_ID, "ADMIN", "USERNAME", List.of("ADMIN"));
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, List.of()));
    }

    private SysUser user(Long id, String status) {
        SysUser u = new SysUser();
        u.setId(id);
        u.setUserType("ADMIN");
        u.setStatus(status);
        u.setDeleted(0);
        return u;
    }

    @Test
    void createUser_success() {
        List<String> roleReplaceCalls = new ArrayList<>();
        AuthUserAdminServiceImpl service = new AuthUserAdminServiceImpl(
                fakeUserRepo(null, 100L),
                fakeIdentityRepo(false),
                fakeRoleRepo(),
                fakeLoginDeviceRepo(),
                (userId, codes) -> roleReplaceCalls.add(userId + ":" + codes),
                passwordEncoder,
                AuthTestSupport.defaultSystemConfig(),
                new SuperAdminGuard(RbacTestSupport.noopRoleRepository()));

        UserCreateRequest req = new UserCreateRequest();
        req.setUsername("newop");
        req.setPassword("Pass@2026");
        req.setNickname("新运营");
        req.setRoleCodes(List.of("ADMIN"));

        Long id = service.createUser(req);
        assertEquals(100L, id);
        assertEquals(1, roleReplaceCalls.size());
    }

    @Test
    void createUser_usernameExists() {
        AuthUserAdminServiceImpl service = new AuthUserAdminServiceImpl(
                fakeUserRepo(null, null),
                fakeIdentityRepo(true),
                fakeRoleRepo(),
                fakeLoginDeviceRepo(),
                (userId, codes) -> { },
                passwordEncoder,
                AuthTestSupport.defaultSystemConfig(),
                new SuperAdminGuard(RbacTestSupport.noopRoleRepository()));

        UserCreateRequest req = new UserCreateRequest();
        req.setUsername("admin");
        req.setPassword("Pass@2026");

        BusinessException ex = assertThrows(BusinessException.class, () -> service.createUser(req));
        assertEquals(ResultCode.AUTH_USERNAME_EXISTS.getCode(), ex.getCode());
    }

    @Test
    void resetPassword_cannotResetSelf() {
        loginAsOperator();
        AuthUserAdminServiceImpl service = new AuthUserAdminServiceImpl(
                fakeUserRepo(user(OPERATOR_ID, "NORMAL"), null),
                fakeIdentityRepoWithUser(OPERATOR_ID),
                fakeRoleRepo(),
                fakeLoginDeviceRepo(),
                (userId, codes) -> { },
                passwordEncoder,
                AuthTestSupport.defaultSystemConfig(),
                new SuperAdminGuard(RbacTestSupport.noopRoleRepository()));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.resetPassword(OPERATOR_ID, "NewPass@2026"));
        assertEquals(ResultCode.AUTH_CANNOT_OPERATE_SELF.getCode(), ex.getCode());
    }

    @Test
    void updateUser_disableSelfBlocked() {
        loginAsOperator();
        AuthUserAdminServiceImpl service = new AuthUserAdminServiceImpl(
                fakeUserRepo(user(OPERATOR_ID, "NORMAL"), null),
                fakeIdentityRepo(false),
                fakeRoleRepo(),
                fakeLoginDeviceRepo(),
                (userId, codes) -> { },
                passwordEncoder,
                AuthTestSupport.defaultSystemConfig(),
                new SuperAdminGuard(RbacTestSupport.noopRoleRepository()));

        UserUpdateRequest req = new UserUpdateRequest();
        req.setStatus("DISABLED");

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.updateUser(OPERATOR_ID, req));
        assertEquals(ResultCode.AUTH_CANNOT_OPERATE_SELF.getCode(), ex.getCode());
    }

    @Test
    void getUser_notFound() {
        AuthUserAdminServiceImpl service = new AuthUserAdminServiceImpl(
                fakeUserRepo(null, null),
                fakeIdentityRepo(false),
                fakeRoleRepo(),
                fakeLoginDeviceRepo(),
                (userId, codes) -> { },
                passwordEncoder,
                AuthTestSupport.defaultSystemConfig(),
                new SuperAdminGuard(RbacTestSupport.noopRoleRepository()));

        BusinessException ex = assertThrows(BusinessException.class, () -> service.getUser(404L));
        assertEquals(ResultCode.NOT_FOUND.getCode(), ex.getCode());
    }

    private SysUserRepository fakeUserRepo(SysUser target, Long insertId) {
        return new SysUserRepository(null) {
            @Override
            public Optional<SysUser> findActiveById(Long id) {
                return target != null && target.getId().equals(id) ? Optional.of(target) : Optional.empty();
            }

            @Override
            public Optional<UserAdminItem> findAdminItemById(Long userId) {
                return Optional.empty();
            }

            @Override
            public Long insertAdminUser(String nickname, String email, String phone) {
                return insertId;
            }

            @Override
            public int updateProfile(Long id, String nickname, String email, String phone) {
                return 1;
            }

            @Override
            public int updateStatus(Long id, String status) {
                return 1;
            }

            @Override
            public int incrementTokenVersion(Long id) {
                return 1;
            }
        };
    }

    private UserIdentityRepository fakeIdentityRepo(boolean exists) {
        return new UserIdentityRepository(null) {
            @Override
            public boolean existsActiveByTypeAndIdentifier(String identityType, String identifier) {
                return exists;
            }

            @Override
            public Long insert(Long userId, String identityType, String identifier, String credential) {
                return 1L;
            }
        };
    }

    private UserIdentityRepository fakeIdentityRepoWithUser(Long userId) {
        return new UserIdentityRepository(null) {
            @Override
            public Optional<UserIdentity> findActiveByUserIdAndType(Long uid, String type) {
                if (uid.equals(userId) && IdentityType.USERNAME.name().equals(type)) {
                    UserIdentity identity = new UserIdentity();
                    identity.setUserId(userId);
                    return Optional.of(identity);
                }
                return Optional.empty();
            }

            @Override
            public int updateCredentialByUserIdAndType(Long uid, String type, String credential) {
                return 1;
            }
        };
    }

    private SysRoleRepository fakeRoleRepo() {
        return new SysRoleRepository(null) {
            @Override
            public List<RoleItem> findAllActiveRoles() {
                return List.of(new RoleItem(1L, "ADMIN", "管理员"));
            }
        };
    }

    private LoginDeviceRepository fakeLoginDeviceRepo() {
        return new LoginDeviceRepository(null) {
            @Override
            public List<LoginDeviceRepository.LoginDeviceRow> findRecentByUserId(Long userId, int limit) {
                return List.of();
            }
        };
    }
}
