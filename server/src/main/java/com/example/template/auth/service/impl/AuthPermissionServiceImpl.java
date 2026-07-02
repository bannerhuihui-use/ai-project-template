package com.example.template.auth.service.impl;

import com.example.template.auth.dto.MenuNode;
import com.example.template.auth.model.SysPermission;
import com.example.template.auth.repository.PermissionRepository;
import com.example.template.auth.security.CurrentUser;
import com.example.template.auth.security.SecurityUtils;
import com.example.template.auth.service.AuthPermissionService;
import com.example.template.common.BusinessException;
import com.example.template.common.ResultCode;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 当前用户权限读取实现。
 */
@Service
public class AuthPermissionServiceImpl implements AuthPermissionService {

    private static final int FLAG_TRUE = 1;

    private final PermissionRepository permissionRepository;

    public AuthPermissionServiceImpl(PermissionRepository permissionRepository) {
        this.permissionRepository = permissionRepository;
    }

    @Override
    public List<MenuNode> listCurrentMenus() {
        Long userId = currentUserId();
        List<SysPermission> menus = permissionRepository.findMenusByUserId(userId);
        return buildTree(menus);
    }

    @Override
    public List<String> listCurrentButtonPermissions() {
        Long userId = currentUserId();
        return permissionRepository.findButtonApiCodesByUserId(userId);
    }

    private Long currentUserId() {
        CurrentUser currentUser = SecurityUtils.getCurrentUser();
        if (currentUser == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED.getCode(), ResultCode.UNAUTHORIZED.getMessage());
        }
        return currentUser.getUserId();
    }

    /**
     * 将扁平菜单列表构建为树。入参已按 sort_no 排序，保持同级顺序。
     * 父节点缺失（被裁剪/未授权）的节点提升为根，避免菜单丢失。
     */
    private List<MenuNode> buildTree(List<SysPermission> permissions) {
        Map<Long, MenuNode> nodeById = new LinkedHashMap<>();
        for (SysPermission p : permissions) {
            nodeById.put(p.getId(), toNode(p));
        }
        List<MenuNode> roots = new ArrayList<>();
        for (SysPermission p : permissions) {
            MenuNode node = nodeById.get(p.getId());
            Long parentId = p.getParentId();
            if (parentId != null && nodeById.containsKey(parentId)) {
                nodeById.get(parentId).getChildren().add(node);
            } else {
                roots.add(node);
            }
        }
        return roots;
    }

    private MenuNode toNode(SysPermission p) {
        MenuNode node = new MenuNode();
        node.setPermissionCode(p.getPermissionCode());
        node.setPermissionType(p.getPermissionType());
        node.setTitle(p.getPermissionName());
        node.setPath(p.getRoutePath());
        node.setName(p.getRouteName());
        node.setComponent(p.getComponentPath());
        node.setRedirect(p.getRedirect());
        node.setIcon(p.getIcon());
        node.setBreadcrumb(isTrue(p.getBreadcrumb()));
        node.setClickable(isTrue(p.getClickable()));
        node.setAlwaysShow(isTrue(p.getAlwaysShow()));
        node.setVisible(isTrue(p.getVisible()));
        node.setKeepAlive(isTrue(p.getKeepAlive()));
        node.setExternalLink(isTrue(p.getIsExternalLink()));
        node.setExternalLinkUrl(p.getExternalLinkUrl());
        node.setSortNo(p.getSortNo());
        return node;
    }

    private boolean isTrue(Integer flag) {
        return flag != null && flag == FLAG_TRUE;
    }
}
