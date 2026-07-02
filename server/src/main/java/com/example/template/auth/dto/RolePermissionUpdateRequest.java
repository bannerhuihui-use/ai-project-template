package com.example.template.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * 角色权限覆盖设置请求。提交空列表代表清空该角色权限（覆盖式更新）。
 */
@Schema(name = "RolePermissionUpdateRequest", description = "角色权限覆盖设置请求")
public class RolePermissionUpdateRequest {

    @Schema(description = "权限编码列表（覆盖式，空数组=清空）",
            example = "[\"menu:auth:user\", \"auth:user:disable\"]")
    @NotNull(message = "权限列表不能为 null")
    private List<@Size(max = 128, message = "权限编码过长") String> permissionCodes;

    public List<String> getPermissionCodes() {
        return permissionCodes;
    }

    public void setPermissionCodes(List<String> permissionCodes) {
        this.permissionCodes = permissionCodes;
    }
}
