package com.example.template.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * 用户角色覆盖设置请求。提交空列表代表清空该用户角色（覆盖式更新）。
 */
@Schema(name = "UserRoleUpdateRequest", description = "用户角色覆盖设置请求")
public class UserRoleUpdateRequest {

    @Schema(description = "角色编码列表（覆盖式，空数组=清空）", example = "[\"ADMIN\", \"OPERATOR\"]")
    @NotNull(message = "角色列表不能为 null")
    private List<@Size(max = 64, message = "角色编码过长") String> roleCodes;

    public List<String> getRoleCodes() {
        return roleCodes;
    }

    public void setRoleCodes(List<String> roleCodes) {
        this.roleCodes = roleCodes;
    }
}
