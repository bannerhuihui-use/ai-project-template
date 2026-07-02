package com.example.template.auth.service;

import com.example.template.auth.model.SysUser;
import com.example.template.auth.repository.SysUserRepository;
import com.example.template.auth.security.CurrentUser;
import com.example.template.auth.service.impl.AuthAdminServiceImpl;
import com.example.template.auth.support.SuperAdminGuard;
import com.example.template.common.BusinessException;
import com.example.template.common.ResultCode;
import com.example.template.testsupport.RbacTestSupport;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * AuthAdminServiceImpl 单元测试（手写 fake，不连 PostgreSQL）。
 *
 * <p>覆盖：禁用成功（改状态 + 自增版本）、禁用自己、目标不存在、强制下线成功。</p>
 */
class AuthAdminServiceTest {

    private static final Long OPERATOR_ID = 99L;

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
        SysUser user = new SysUser();
        user.setId(id);
        user.setUserType("ADMIN");
        user.setStatus(status);
        user.setTokenVersion(0);
        user.setDeleted(0);
        return user;
    }

    private SysUserRepository repo(SysUser target, int[] statusUpdates, int[] versionBumps) {
        return new SysUserRepository(null) {
            @Override
            public Optional<SysUser> findActiveById(Long id) {
                return target != null && target.getId().equals(id) ? Optional.of(target) : Optional.empty();
            }

            @Override
            public int updateStatus(Long id, String status) {
                statusUpdates[0]++;
                return 1;
            }

            @Override
            public int incrementTokenVersion(Long id) {
                versionBumps[0]++;
                return 1;
            }
        };
    }

    @Test
    void disableUser_success() {
        loginAsOperator();
        int[] statusUpdates = {0};
        int[] versionBumps = {0};
        AuthAdminServiceImpl service = new AuthAdminServiceImpl(
                repo(user(1L, "NORMAL"), statusUpdates, versionBumps),
                new SuperAdminGuard(RbacTestSupport.noopRoleRepository()));

        service.disableUser(1L);

        assertEquals(1, statusUpdates[0], "禁用应更新状态");
        assertEquals(1, versionBumps[0], "禁用应自增 token_version");
    }

    @Test
    void disableUser_self_shouldFail() {
        loginAsOperator();
        AuthAdminServiceImpl service =
                new AuthAdminServiceImpl(repo(user(OPERATOR_ID, "NORMAL"), new int[]{0}, new int[]{0}),
                        new SuperAdminGuard(RbacTestSupport.noopRoleRepository()));

        BusinessException ex = assertThrows(BusinessException.class, () -> service.disableUser(OPERATOR_ID));
        assertEquals(ResultCode.AUTH_CANNOT_DISABLE_SELF.getCode(), ex.getCode());
    }

    @Test
    void disableUser_notFound_shouldFail() {
        loginAsOperator();
        AuthAdminServiceImpl service = new AuthAdminServiceImpl(repo(null, new int[]{0}, new int[]{0}),
                new SuperAdminGuard(RbacTestSupport.noopRoleRepository()));

        BusinessException ex = assertThrows(BusinessException.class, () -> service.disableUser(404L));
        assertEquals(ResultCode.NOT_FOUND.getCode(), ex.getCode());
    }

    @Test
    void disableUser_alreadyDisabled_isIdempotent() {
        loginAsOperator();
        int[] statusUpdates = {0};
        int[] versionBumps = {0};
        AuthAdminServiceImpl service = new AuthAdminServiceImpl(repo(user(1L, "DISABLED"), statusUpdates, versionBumps),
                new SuperAdminGuard(RbacTestSupport.noopRoleRepository()));

        service.disableUser(1L);

        assertEquals(0, statusUpdates[0], "已禁用应幂等不再更新");
        assertEquals(0, versionBumps[0], "已禁用应幂等不再自增");
    }

    @Test
    void forceLogout_success() {
        loginAsOperator();
        int[] versionBumps = {0};
        AuthAdminServiceImpl service = new AuthAdminServiceImpl(repo(user(1L, "NORMAL"), new int[]{0}, versionBumps),
                new SuperAdminGuard(RbacTestSupport.noopRoleRepository()));

        service.forceLogout(1L);

        assertEquals(1, versionBumps[0], "强制下线应自增 token_version");
    }

    @Test
    void forceLogout_notFound_shouldFail() {
        loginAsOperator();
        AuthAdminServiceImpl service = new AuthAdminServiceImpl(repo(null, new int[]{0}, new int[]{0}),
                new SuperAdminGuard(RbacTestSupport.noopRoleRepository()));

        BusinessException ex = assertThrows(BusinessException.class, () -> service.forceLogout(404L));
        assertEquals(ResultCode.NOT_FOUND.getCode(), ex.getCode());
    }
}
