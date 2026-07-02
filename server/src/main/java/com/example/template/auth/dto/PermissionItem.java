package com.example.template.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 权限点列表项（管理端分页/角色权限查询返回）。
 */
@Schema(name = "PermissionItem", description = "权限点列表项")
public class PermissionItem {

    @Schema(description = "权限 ID", example = "1")
    private Long id;

    @Schema(description = "权限编码", example = "auth:user:disable")
    private String permissionCode;

    @Schema(description = "权限名称", example = "禁用用户")
    private String permissionName;

    @Schema(description = "权限类型", example = "API")
    private String permissionType;

    @Schema(description = "所属模块", example = "auth")
    private String module;

    @Schema(description = "状态", example = "NORMAL")
    private String status;

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

    public String getModule() {
        return module;
    }

    public void setModule(String module) {
        this.module = module;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
