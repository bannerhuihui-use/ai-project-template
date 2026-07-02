package com.example.template.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 角色下拉项。
 */
@Schema(name = "RoleItem", description = "角色简要信息")
public class RoleItem {

    @Schema(description = "角色 ID", example = "1")
    private Long roleId;

    @Schema(description = "角色编码", example = "ADMIN")
    private String roleCode;

    @Schema(description = "角色名称", example = "管理员")
    private String roleName;

    public RoleItem() {
    }

    public RoleItem(Long roleId, String roleCode, String roleName) {
        this.roleId = roleId;
        this.roleCode = roleCode;
        this.roleName = roleName;
    }

    public Long getRoleId() {
        return roleId;
    }

    public void setRoleId(Long roleId) {
        this.roleId = roleId;
    }

    public String getRoleCode() {
        return roleCode;
    }

    public void setRoleCode(String roleCode) {
        this.roleCode = roleCode;
    }

    public String getRoleName() {
        return roleName;
    }

    public void setRoleName(String roleName) {
        this.roleName = roleName;
    }
}
