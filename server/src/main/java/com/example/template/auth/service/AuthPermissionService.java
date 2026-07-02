package com.example.template.auth.service;

import com.example.template.auth.dto.MenuNode;

import java.util.List;

/**
 * 当前登录用户的权限读取（菜单树、按钮/接口权限码）。
 */
public interface AuthPermissionService {

    /**
     * 获取当前登录用户的菜单树（已按角色过滤，按 sort_no 排序）。
     *
     * @return 菜单树根节点列表
     */
    List<MenuNode> listCurrentMenus();

    /**
     * 获取当前登录用户的按钮/接口权限码（去重）。
     *
     * @return 权限编码列表
     */
    List<String> listCurrentButtonPermissions();
}
