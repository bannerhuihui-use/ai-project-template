package com.example.template.auth.service;

import com.example.template.auth.dto.MenuNode;
import com.example.template.auth.model.SysPermission;
import com.example.template.auth.repository.PermissionRepository;
import com.example.template.auth.security.CurrentUser;
import com.example.template.auth.service.impl.AuthPermissionServiceImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * AuthPermissionServiceImpl 单元测试（手写 fake，不连库）。
 *
 * <p>覆盖：菜单树父子构建、排序、不可点击目录、父节点缺失提升为根、按钮权限透传。</p>
 */
class AuthPermissionServiceTest {

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    private void login(Long userId) {
        CurrentUser principal = new CurrentUser(userId, "ADMIN", "USERNAME", List.of("ADMIN"));
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, List.of()));
    }

    private SysPermission menu(long id, Long parentId, String code, int sortNo, int clickable) {
        SysPermission p = new SysPermission();
        p.setId(id);
        p.setParentId(parentId);
        p.setPermissionCode(code);
        p.setPermissionName(code);
        p.setPermissionType("MENU");
        p.setSortNo(sortNo);
        p.setClickable(clickable);
        p.setVisible(1);
        p.setBreadcrumb(1);
        return p;
    }

    private AuthPermissionServiceImpl service(List<SysPermission> menus, List<String> buttons) {
        PermissionRepository repo = new PermissionRepository(null) {
            @Override
            public List<SysPermission> findMenusByUserId(Long userId) {
                return menus;
            }

            @Override
            public List<String> findButtonApiCodesByUserId(Long userId) {
                return buttons;
            }
        };
        return new AuthPermissionServiceImpl(repo);
    }

    @Test
    void listCurrentMenus_buildsTreeWithParentChild() {
        login(1L);
        // root(id=1, clickable=0) -> child(id=2)
        List<SysPermission> menus = List.of(
                menu(1L, null, "menu:system:root", 100, 0),
                menu(2L, 1L, "menu:auth:user", 10, 1));
        AuthPermissionServiceImpl service = service(menus, List.of());

        List<MenuNode> tree = service.listCurrentMenus();

        assertEquals(1, tree.size(), "应只有一个根节点");
        MenuNode root = tree.get(0);
        assertEquals("menu:system:root", root.getPermissionCode());
        assertFalse(root.isClickable(), "目录不可点击");
        assertEquals(1, root.getChildren().size(), "根下应有一个子菜单");
        assertEquals("menu:auth:user", root.getChildren().get(0).getPermissionCode());
        assertTrue(root.getChildren().get(0).isClickable());
    }

    @Test
    void listCurrentMenus_promotesOrphanToRoot() {
        login(1L);
        // 子节点的父在结果集中缺失（未授权/被裁剪），应提升为根，避免菜单丢失
        List<SysPermission> menus = List.of(menu(2L, 999L, "menu:auth:user", 10, 1));
        AuthPermissionServiceImpl service = service(menus, List.of());

        List<MenuNode> tree = service.listCurrentMenus();

        assertEquals(1, tree.size());
        assertEquals("menu:auth:user", tree.get(0).getPermissionCode());
    }

    @Test
    void listCurrentButtonPermissions_passthrough() {
        login(1L);
        AuthPermissionServiceImpl service = service(List.of(),
                List.of("auth:user:create", "auth:user:disable"));

        List<String> codes = service.listCurrentButtonPermissions();

        assertEquals(2, codes.size());
        assertTrue(codes.contains("auth:user:disable"));
    }
}
