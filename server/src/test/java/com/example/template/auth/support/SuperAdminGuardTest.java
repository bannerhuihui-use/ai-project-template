package com.example.template.auth.support;

import com.example.template.auth.repository.SysRoleRepository;
import com.example.template.auth.security.CurrentUser;
import com.example.template.auth.security.SecurityUtils;
import com.example.template.common.BusinessException;
import com.example.template.common.ResultCode;
import com.example.template.testsupport.RbacTestSupport;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SuperAdminGuardTest {

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void assertTargetOperable_superAdmin_shouldFail() {
        SysRoleRepository repo = RbacTestSupport.roleRepositoryForUser(1L, List.of(SuperAdminGuard.ROLE_CODE));
        SuperAdminGuard guard = new SuperAdminGuard(repo);

        BusinessException ex = assertThrows(BusinessException.class, () -> guard.assertTargetOperable(1L));
        assertEquals(ResultCode.AUTH_CANNOT_OPERATE_SUPER_ADMIN.getCode(), ex.getCode());
    }

    @Test
    void validateRoleAssignment_nonSuperAdminOperator_shouldFail() {
        loginAs(List.of("ADMIN"));
        SuperAdminGuard guard = new SuperAdminGuard(RbacTestSupport.noopRoleRepository());

        BusinessException ex = assertThrows(BusinessException.class,
                () -> guard.validateRoleAssignment(List.of(SuperAdminGuard.ROLE_CODE)));
        assertEquals(ResultCode.AUTH_SUPER_ADMIN_GRANT_DENIED.getCode(), ex.getCode());
    }

    @Test
    void validateRoleAssignment_superAdminOperator_shouldPass() {
        loginAs(List.of(SuperAdminGuard.ROLE_CODE));
        SuperAdminGuard guard = new SuperAdminGuard(RbacTestSupport.noopRoleRepository());

        guard.validateRoleAssignment(List.of(SuperAdminGuard.ROLE_CODE));
    }

    @Test
    void validateRoleReplacement_removeLastSuperAdmin_shouldFail() {
        loginAs(List.of(SuperAdminGuard.ROLE_CODE));
        SysRoleRepository repo = RbacTestSupport.roleRepositoryForUser(1L, List.of(SuperAdminGuard.ROLE_CODE));
        SuperAdminGuard guard = new SuperAdminGuard(repo);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> guard.validateRoleReplacement(1L, List.of("ADMIN")));
        assertEquals(ResultCode.AUTH_LAST_SUPER_ADMIN.getCode(), ex.getCode());
    }

    private void loginAs(List<String> roles) {
        CurrentUser user = new CurrentUser(99L, "ADMIN", "USERNAME", roles);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user, null, List.of()));
    }
}
