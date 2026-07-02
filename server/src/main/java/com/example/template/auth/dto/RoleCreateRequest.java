package com.example.template.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 新建角色请求。
 */
@Schema(name = "RoleCreateRequest", description = "新建角色请求")
public class RoleCreateRequest {

    @Schema(description = "角色编码（大写字母开头，仅大写字母/数字/下划线）", example = "OPERATOR",
            requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "角色编码不能为空")
    @Size(min = 2, max = 64, message = "角色编码长度为 2-64")
    @Pattern(regexp = "^[A-Z][A-Z0-9_]{1,63}$", message = "角色编码格式非法")
    private String roleCode;

    @Schema(description = "角色名称", example = "运营人员", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "角色名称不能为空")
    @Size(min = 1, max = 64, message = "角色名称长度为 1-64")
    private String roleName;

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
