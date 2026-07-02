package com.example.template.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.ArrayList;
import java.util.List;

/**
 * 菜单树节点（返回给前端用于动态路由、侧边栏、面包屑）。
 * 字段契约见 docs/API/auth-v2.0-rbac.md。
 */
@Schema(name = "MenuNode", description = "菜单树节点")
public class MenuNode {

    @Schema(description = "权限编码", example = "menu:auth:user")
    private String permissionCode;

    @Schema(description = "权限类型", example = "MENU")
    private String permissionType;

    @Schema(description = "菜单标题", example = "用户管理")
    private String title;

    @Schema(description = "前端路由 path", example = "/system/user")
    private String path;

    @Schema(description = "前端路由 name", example = "AuthUser")
    private String name;

    @Schema(description = "前端组件路径", example = "/pages/auth/user/index")
    private String component;

    @Schema(description = "重定向路径", example = "/system/user/list")
    private String redirect;

    @Schema(description = "图标", example = "icon-user")
    private String icon;

    @Schema(description = "是否显示面包屑", example = "true")
    private boolean breadcrumb;

    @Schema(description = "目录是否可点击（false 仅作分组）", example = "true")
    private boolean clickable;

    @Schema(description = "仅一个子路由时是否仍显示父级", example = "false")
    private boolean alwaysShow;

    @Schema(description = "菜单是否可见", example = "true")
    private boolean visible;

    @Schema(description = "页面是否缓存", example = "false")
    private boolean keepAlive;

    @Schema(description = "是否外链", example = "false")
    private boolean externalLink;

    @Schema(description = "外链地址（externalLink=true 时有效）")
    private String externalLinkUrl;

    @Schema(description = "同级排序", example = "10")
    private Integer sortNo;

    @Schema(description = "子菜单")
    private List<MenuNode> children = new ArrayList<>();

    public String getPermissionCode() {
        return permissionCode;
    }

    public void setPermissionCode(String permissionCode) {
        this.permissionCode = permissionCode;
    }

    public String getPermissionType() {
        return permissionType;
    }

    public void setPermissionType(String permissionType) {
        this.permissionType = permissionType;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getComponent() {
        return component;
    }

    public void setComponent(String component) {
        this.component = component;
    }

    public String getRedirect() {
        return redirect;
    }

    public void setRedirect(String redirect) {
        this.redirect = redirect;
    }

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public boolean isBreadcrumb() {
        return breadcrumb;
    }

    public void setBreadcrumb(boolean breadcrumb) {
        this.breadcrumb = breadcrumb;
    }

    public boolean isClickable() {
        return clickable;
    }

    public void setClickable(boolean clickable) {
        this.clickable = clickable;
    }

    public boolean isAlwaysShow() {
        return alwaysShow;
    }

    public void setAlwaysShow(boolean alwaysShow) {
        this.alwaysShow = alwaysShow;
    }

    public boolean isVisible() {
        return visible;
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    public boolean isKeepAlive() {
        return keepAlive;
    }

    public void setKeepAlive(boolean keepAlive) {
        this.keepAlive = keepAlive;
    }

    public boolean isExternalLink() {
        return externalLink;
    }

    public void setExternalLink(boolean externalLink) {
        this.externalLink = externalLink;
    }

    public String getExternalLinkUrl() {
        return externalLinkUrl;
    }

    public void setExternalLinkUrl(String externalLinkUrl) {
        this.externalLinkUrl = externalLinkUrl;
    }

    public Integer getSortNo() {
        return sortNo;
    }

    public void setSortNo(Integer sortNo) {
        this.sortNo = sortNo;
    }

    public List<MenuNode> getChildren() {
        return children;
    }

    public void setChildren(List<MenuNode> children) {
        this.children = children;
    }
}
