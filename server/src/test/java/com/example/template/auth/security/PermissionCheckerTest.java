package com.example.template.auth.security;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * PermissionChecker 单元测试：SUPER_ADMIN 短路、权限码匹配、缺失拒绝、未认证拒绝。
 */
class PermissionCheckerTest {

    private final PermissionChecker checker = new PermissionChecker();

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    private void authorities(String... auths) {
        List<SimpleGrantedAuthority> list = java.util.Arrays.stream(auths)
                .map(SimpleGrantedAuthority::new).toList();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("user", null, list));
    }

    @Test
    void superAdmin_shortCircuits() {
        authorities("ROLE_SUPER_ADMIN");
        assertTrue(checker.has("auth:role:grant_permission"), "超级管理员应短路放行任意权限码");
    }

    @Test
    void hasMatchingCode_allows() {
        authorities("ROLE_ADMIN", "auth:user:disable");
        assertTrue(checker.has("auth:user:disable"));
    }

    @Test
    void missingCode_denies() {
        authorities("ROLE_ADMIN", "auth:user:disable");
        assertFalse(checker.has("auth:role:grant_permission"));
    }

    @Test
    void noAuthentication_denies() {
        assertFalse(checker.has("auth:user:disable"));
    }
}
