package com.example.template.auth.service;

import com.example.template.auth.model.SysUser;
import com.example.template.auth.repository.PermissionRepository;
import com.example.template.auth.repository.SysRoleRepository;
import com.example.template.auth.repository.SysUserRepository;
import com.example.template.auth.repository.SysUserRoleRepository;
import com.example.template.auth.security.UserPermissionCacheService;
import com.example.template.auth.service.impl.AuthUserRoleServiceImpl;
import com.example.template.auth.support.SuperAdminGuard;
import com.example.template.common.BusinessException;
import com.example.template.common.ResultCode;
import com.example.template.testsupport.RbacTestSupport;
import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * AuthUserRoleServiceImpl 单元测试（手写 fake，不连库）。
 *
 * <p>覆盖：覆盖式设置成功（清空+写入+用户 token 失效）、用户不存在 40400、角色编码非法 40401。</p>
 */
class AuthUserRoleServiceTest {

    private static UserPermissionCacheService noopCache() {
        return new UserPermissionCacheService(new PermissionRepository(null));
    }

    private static SuperAdminGuard noopGuard() {
        return new SuperAdminGuard(RbacTestSupport.noopRoleRepository());
    }

    private SysUser normalUser() {
        SysUser user = new SysUser();
        user.setId(1L);
        user.setUserType("ADMIN");
        user.setStatus("NORMAL");
        user.setTokenVersion(0);
        user.setDeleted(0);
        return user;
    }

    private SysUserRepository userRepo(SysUser user) {
        return new SysUserRepository(null) {
            @Override
            public Optional<SysUser> findActiveById(Long id) {
                return Optional.ofNullable(user);
            }
        };
    }

    @Test
    void replace_success_shouldClearInsertAndBumpVersion() {
        int[] softDeleted = {0};
        int[] inserted = {-1};
        int[] bumped = {0};

        SysUserRoleRepository urRepo = new SysUserRoleRepository(null) {
            @Override
            public int softDeleteByUserId(Long userId) {
                softDeleted[0]++;
                return 1;
            }

            @Override
            public void insertUserRoles(Long userId, List<Long> roleIds) {
                inserted[0] = roleIds.size();
            }
        };
        SysRoleRepository roleRepo = new SysRoleRepository(null) {
            @Override
            public List<Long> findActiveIdsByCodes(Collection<String> roleCodes) {
                return List.of(5L, 6L);
            }
        };
        SysUserRepository userRepo = new SysUserRepository(null) {
            @Override
            public Optional<SysUser> findActiveById(Long id) {
                return Optional.of(normalUser());
            }

            @Override
            public int incrementTokenVersion(Long id) {
                bumped[0]++;
                return 1;
            }
        };
        AuthUserRoleServiceImpl service = new AuthUserRoleServiceImpl(urRepo, roleRepo, userRepo, noopCache(), noopGuard());

        service.replaceUserRoles(1L, List.of("ADMIN", "OPERATOR"));

        assertEquals(1, softDeleted[0]);
        assertEquals(2, inserted[0]);
        assertEquals(1, bumped[0]);
    }

    @Test
    void replace_userNotFound_shouldFail() {
        AuthUserRoleServiceImpl service = new AuthUserRoleServiceImpl(
                new SysUserRoleRepository(null), new SysRoleRepository(null), userRepo(null), noopCache(), noopGuard());

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.replaceUserRoles(404L, List.of("ADMIN")));
        assertEquals(ResultCode.NOT_FOUND.getCode(), ex.getCode());
    }

    @Test
    void replace_invalidRoleCode_shouldFail() {
        SysRoleRepository roleRepo = new SysRoleRepository(null) {
            @Override
            public List<Long> findActiveIdsByCodes(Collection<String> roleCodes) {
                return List.of(5L); // 提交 2 个只解析 1 个
            }
        };
        AuthUserRoleServiceImpl service = new AuthUserRoleServiceImpl(
                new SysUserRoleRepository(null), roleRepo, userRepo(normalUser()), noopCache(), noopGuard());

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.replaceUserRoles(1L, List.of("ADMIN", "BAD_ROLE")));
        assertEquals(ResultCode.AUTH_ROLE_NOT_FOUND.getCode(), ex.getCode());
    }
}
