package com.example.template.auth.model;

/**
 * 权限类型（统一权限中心，对应 sys_permission.permission_type）。
 *
 * <ul>
 *     <li>{@link #MENU}：菜单/路由，控制页面可见与导航（含 route/icon/breadcrumb 等元数据）。</li>
 *     <li>{@link #BUTTON}：页面按钮动作，控制前端按钮显隐与可用。</li>
 *     <li>{@link #API}：后端接口动作权限，配合 {@code @PreAuthorize("hasAuthority(...)")} 校验。</li>
 *     <li>{@link #DATA}：数据范围权限（v2.1 预留，本版本不落地）。</li>
 * </ul>
 */
public enum PermissionType {
    MENU,
    BUTTON,
    API,
    DATA
}
