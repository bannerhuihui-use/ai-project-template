package com.example.template.auth.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * 方法级权限判定（供 {@code @PreAuthorize("@perm.has('...')")} 使用）。
 *
 * <p>规则：拥有 {@code ROLE_SUPER_ADMIN} 直接短路放行（视为具备全部权限码）；
 * 否则要求 authorities 中包含目标权限码。权限码 authorities 由 {@link JwtAuthenticationFilter}
 * 在鉴权时从 DB 加载注入（决策 C2，不放进 token）。</p>
 */
@Component("perm")
public class PermissionChecker {

    private static final String SUPER_ADMIN_AUTHORITY = "ROLE_SUPER_ADMIN";

    /**
     * 判断当前登录用户是否具备指定权限码。
     *
     * @param permissionCode 权限编码，如 {@code auth:role:grant_permission}
     * @return 具备或为超级管理员返回 true
     */
    public boolean has(String permissionCode) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }
        boolean matched = false;
        for (GrantedAuthority authority : authentication.getAuthorities()) {
            String value = authority.getAuthority();
            if (SUPER_ADMIN_AUTHORITY.equals(value)) {
                return true;
            }
            if (permissionCode != null && permissionCode.equals(value)) {
                matched = true;
            }
        }
        return matched;
    }
}
