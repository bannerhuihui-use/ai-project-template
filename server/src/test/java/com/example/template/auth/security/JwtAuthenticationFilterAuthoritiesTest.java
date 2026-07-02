package com.example.template.auth.security;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 纯单元测试：验证 {@link JwtAuthenticationFilter#buildAuthorities} 的权限组装逻辑。
 *
 * <p>不连库、不起容器，覆盖 v1.1 核心改动：authorities 同时包含 ROLE_{userType} 与每个
 * ROLE_{roleCode}，且对同名/空白做去重与过滤。</p>
 */
class JwtAuthenticationFilterAuthoritiesTest {

    private static List<String> authorityNames(String userType, List<String> roles) {
        return JwtAuthenticationFilter.buildAuthorities(userType, roles).stream()
                .map(SimpleGrantedAuthority::getAuthority)
                .toList();
    }

    @Test
    void shouldContainBothUserTypeAndRoleAuthorities() {
        List<String> names = authorityNames("ADMIN", List.of("ADMIN", "OPERATOR"));

        assertTrue(names.contains("ROLE_ADMIN"), "应包含 ROLE_ADMIN");
        assertTrue(names.contains("ROLE_OPERATOR"), "应包含 ROLE_OPERATOR");
    }

    @Test
    void shouldDeduplicateUserTypeAndSameRoleCode() {
        List<String> names = authorityNames("ADMIN", List.of("ADMIN"));

        assertEquals(1, names.size(), "userType 与同名 roleCode 应去重为一个权限");
        assertEquals("ROLE_ADMIN", names.get(0));
    }

    @Test
    void shouldSkipBlankAndNullEntries() {
        List<String> names = authorityNames("MEMBER", Arrays.asList(null, "", "  ", "VIP"));

        assertEquals(List.of("ROLE_MEMBER", "ROLE_VIP"), names, "应跳过 null/空白并保留有效角色");
    }

    @Test
    void shouldHandleNullUserTypeAndNullRoles() {
        assertTrue(authorityNames(null, null).isEmpty(), "全空输入应返回空权限列表");
    }

    @Test
    void shouldTrimSurroundingWhitespace() {
        List<String> names = authorityNames("  ADMIN  ", List.of(" OPERATOR "));

        assertEquals(List.of("ROLE_ADMIN", "ROLE_OPERATOR"), names, "应去除前后空白再加前缀");
    }
}
