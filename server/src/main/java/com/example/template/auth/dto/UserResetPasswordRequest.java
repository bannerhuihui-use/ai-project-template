package com.example.template.auth.dto;

import com.example.template.system.support.BuiltinConfigValidator;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 管理员重置用户密码请求。
 */
@Schema(name = "UserResetPasswordRequest", description = "管理员重置用户密码")
public class UserResetPasswordRequest {

    @Schema(description = "新密码", example = "NewPass@2026", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "新密码不能为空")
    @Size(max = BuiltinConfigValidator.PASSWORD_MAX_LENGTH, message = "新密码长度不能超过 64 位")
    private String newPassword;

    public String getNewPassword() {
        return newPassword;
    }

    public void setNewPassword(String newPassword) {
        this.newPassword = newPassword;
    }
}
