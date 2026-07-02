package com.example.template.auth.dto;

import com.example.template.system.support.BuiltinConfigValidator;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 修改密码请求。userId 取自登录上下文，请求体不含 userId（防越权改他人密码）。
 */
@Schema(name = "ChangePasswordRequest", description = "修改密码请求")
public class ChangePasswordRequest {

    @Schema(description = "原密码", example = "123456", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "原密码不能为空")
    @Size(max = BuiltinConfigValidator.PASSWORD_MAX_LENGTH, message = "原密码长度不能超过 64 位")
    private String oldPassword;

    @Schema(description = "新密码", example = "NewPass@2026", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "新密码不能为空")
    @Size(max = BuiltinConfigValidator.PASSWORD_MAX_LENGTH, message = "新密码长度不能超过 64 位")
    private String newPassword;

    public String getOldPassword() {
        return oldPassword;
    }

    public void setOldPassword(String oldPassword) {
        this.oldPassword = oldPassword;
    }

    public String getNewPassword() {
        return newPassword;
    }

    public void setNewPassword(String newPassword) {
        this.newPassword = newPassword;
    }
}
