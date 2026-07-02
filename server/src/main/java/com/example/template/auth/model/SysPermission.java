package com.example.template.auth.model;

import java.time.LocalDateTime;

/**
 * 统一权限定义（对应表 sys_permission）。
 *
 * <p>同一张表承载 MENU/BUTTON/API/DATA 四类权限：菜单类使用路由/图标等元字段，
 * 按钮/接口类主要使用 {@code permissionCode}。</p>
 */
public class SysPermission {

    private Long id;
    private String permissionCode;
    private String permissionName;
    private String permissionType;
    private Long parentId;
    private String module;
    private String action;
    private String routePath;
    private String routeName;
    private String componentPath;
    private String redirect;
    private String icon;
    private Integer clickable;
    private Integer breadcrumb;
    private Integer alwaysShow;
    private Integer isExternalLink;
    private String externalLinkUrl;
    private Integer sortNo;
    private Integer visible;
    private Integer keepAlive;
    private String dataScopeCode;
    private String status;
    private String remark;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Integer deleted;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getPermissionCode() {
        return permissionCode;
    }

    public void setPermissionCode(String permissionCode) {
        this.permissionCode = permissionCode;
    }

    public String getPermissionName() {
        return permissionName;
    }

    public void setPermissionName(String permissionName) {
        this.permissionName = permissionName;
    }

    public String getPermissionType() {
        return permissionType;
    }

    public void setPermissionType(String permissionType) {
        this.permissionType = permissionType;
    }

    public Long getParentId() {
        return parentId;
    }

    public void setParentId(Long parentId) {
        this.parentId = parentId;
    }

    public String getModule() {
        return module;
    }

    public void setModule(String module) {
        this.module = module;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getRoutePath() {
        return routePath;
    }

    public void setRoutePath(String routePath) {
        this.routePath = routePath;
    }

    public String getRouteName() {
        return routeName;
    }

    public void setRouteName(String routeName) {
        this.routeName = routeName;
    }

    public String getComponentPath() {
        return componentPath;
    }

    public void setComponentPath(String componentPath) {
        this.componentPath = componentPath;
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

    public Integer getClickable() {
        return clickable;
    }

    public void setClickable(Integer clickable) {
        this.clickable = clickable;
    }

    public Integer getBreadcrumb() {
        return breadcrumb;
    }

    public void setBreadcrumb(Integer breadcrumb) {
        this.breadcrumb = breadcrumb;
    }

    public Integer getAlwaysShow() {
        return alwaysShow;
    }

    public void setAlwaysShow(Integer alwaysShow) {
        this.alwaysShow = alwaysShow;
    }

    public Integer getIsExternalLink() {
        return isExternalLink;
    }

    public void setIsExternalLink(Integer isExternalLink) {
        this.isExternalLink = isExternalLink;
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

    public Integer getVisible() {
        return visible;
    }

    public void setVisible(Integer visible) {
        this.visible = visible;
    }

    public Integer getKeepAlive() {
        return keepAlive;
    }

    public void setKeepAlive(Integer keepAlive) {
        this.keepAlive = keepAlive;
    }

    public String getDataScopeCode() {
        return dataScopeCode;
    }

    public void setDataScopeCode(String dataScopeCode) {
        this.dataScopeCode = dataScopeCode;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Integer getDeleted() {
        return deleted;
    }

    public void setDeleted(Integer deleted) {
        this.deleted = deleted;
    }
}
