package com.example.template.auth.service;

import com.example.template.auth.repository.PermissionRepository;
import com.example.template.auth.repository.SysRoleRepository;
import com.example.template.auth.repository.SysUserRepository;
import com.example.template.auth.security.UserPermissionCacheService;
import com.example.template.auth.service.impl.AuthRolePermissionServiceImpl;
import com.example.template.common.BusinessException;
import com.example.template.common.ResultCode;
import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * AuthRolePermissionServiceImpl 单元测试（手写 fake，不连库）。
 *
 * <p>覆盖：覆盖式更新成功（清空+写入+整角色失效）、角色不存在 40401、权限码非法 40402、清空场景。</p>
 */
class AuthRolePermissionServiceTest {

    private static UserPermissionCacheService noopCache() {
        return new UserPermissionCacheService(new PermissionRepository(null));
    }

    private SysRoleRepository roleRepo(boolean roleExists) {
        return new SysRoleRepository(null) {
            @Override
            public boolean existsActiveById(Long roleId) {
                return roleExists;
            }
        };
    }

    @Test
    void replace_success_shouldClearInsertAndBumpVersion() {
        int[] softDeleted = {0};
        int[] inserted = {0};
        int[] bumped = {0};

        PermissionRepository permRepo = new PermissionRepository(null) {
            @Override
            public List<Long> findActiveIdsByCodes(Collection<String> codes) {
                return List.of(10L, 11L);
            }

            @Override
            public int softDeleteByRoleId(Long roleId) {
                softDeleted[0]++;
                return 1;
            }

            @Override
            public void insertRolePermissions(Long roleId, List<Long> permissionIds) {
                inserted[0] = permissionIds.size();
            }
        };
        SysUserRepository userRepo = new SysUserRepository(null) {
            @Override
            public int incrementTokenVersionByRoleId(Long roleId) {
                bumped[0]++;
                return 3;
            }
        };
        AuthRolePermissionServiceImpl service =
                new AuthRolePermissionServiceImpl(permRepo, roleRepo(true), userRepo, noopCache());

        service.replaceRolePermissions(1L, List.of("auth:user:disable", "auth:role:read"));

        assertEquals(1, softDeleted[0], "应先清空旧关联");
        assertEquals(2, inserted[0], "应写入 2 条新关联");
        assertEquals(1, bumped[0], "应触发整角色 token 失效");
    }

    @Test
    void replace_roleNotFound_shouldFail() {
        AuthRolePermissionServiceImpl service = new AuthRolePermissionServiceImpl(
                new PermissionRepository(null), roleRepo(false), new SysUserRepository(null), noopCache());

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.replaceRolePermissions(99L, List.of("auth:user:disable")));
        assertEquals(ResultCode.AUTH_ROLE_NOT_FOUND.getCode(), ex.getCode());
    }

    @Test
    void replace_invalidCode_shouldFail() {
        PermissionRepository permRepo = new PermissionRepository(null) {
            @Override
            public List<Long> findActiveIdsByCodes(Collection<String> codes) {
                // 提交 2 个但只解析出 1 个有效 -> 非法
                return List.of(10L);
            }
        };
        AuthRolePermissionServiceImpl service =
                new AuthRolePermissionServiceImpl(permRepo, roleRepo(true), new SysUserRepository(null), noopCache());

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.replaceRolePermissions(1L, List.of("auth:user:disable", "bad:code")));
        assertEquals(ResultCode.AUTH_PERMISSION_NOT_FOUND.getCode(), ex.getCode());
    }

    @Test
    void replace_emptyCodes_shouldClearOnly() {
        int[] softDeleted = {0};
        int[] inserted = {-1};
        PermissionRepository permRepo = new PermissionRepository(null) {
            @Override
            public int softDeleteByRoleId(Long roleId) {
                softDeleted[0]++;
                return 1;
            }

            @Override
            public void insertRolePermissions(Long roleId, List<Long> permissionIds) {
                inserted[0] = permissionIds.size();
            }
        };
        SysUserRepository userRepo = new SysUserRepository(null) {
            @Override
            public int incrementTokenVersionByRoleId(Long roleId) {
                return 0;
            }
        };
        AuthRolePermissionServiceImpl service =
                new AuthRolePermissionServiceImpl(permRepo, roleRepo(true), userRepo, noopCache());

        service.replaceRolePermissions(1L, List.of());

        assertEquals(1, softDeleted[0], "清空场景仍应执行软删");
        assertEquals(0, inserted[0], "清空场景写入 0 条");
    }
}
