package com.example.template.testsupport;

import com.example.template.auth.repository.SysRoleRepository;

import java.util.Collections;
import java.util.List;

/**
 * 鉴权/RBAC 单元测试用 Repository stub。
 */
public final class RbacTestSupport {

    private RbacTestSupport() {
    }

    /** 默认无 SUPER_ADMIN 用户、count=2（移除超管时不会触发「最后一名」保护）。 */
    public static SysRoleRepository noopRoleRepository() {
        return new SysRoleRepository(null) {
            @Override
            public List<String> findRoleCodesByUserId(Long userId) {
                return List.of();
            }

            @Override
            public long countUsersWithRoleCode(String roleCode) {
                return 2L;
            }

            @Override
            public List<Long> findActiveIdsByCodes(java.util.Collection<String> roleCodes) {
                if (roleCodes == null || roleCodes.isEmpty()) {
                    return Collections.emptyList();
                }
                return List.copyOf(Collections.nCopies(roleCodes.size(), 1L));
            }
        };
    }

    public static SysRoleRepository roleRepositoryForUser(Long userId, List<String> roleCodes) {
        return new SysRoleRepository(null) {
            @Override
            public List<String> findRoleCodesByUserId(Long uid) {
                return uid != null && uid.equals(userId) ? roleCodes : List.of();
            }

            @Override
            public long countUsersWithRoleCode(String roleCode) {
                return roleCodes.contains(roleCode) ? 1L : 2L;
            }

            @Override
            public List<Long> findActiveIdsByCodes(java.util.Collection<String> codes) {
                if (codes == null || codes.isEmpty()) {
                    return Collections.emptyList();
                }
                return List.copyOf(Collections.nCopies(codes.size(), 1L));
            }
        };
    }
}
