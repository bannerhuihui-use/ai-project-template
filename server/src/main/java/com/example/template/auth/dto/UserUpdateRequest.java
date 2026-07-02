package com.example.template.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * 更新后台用户请求（不含密码；改密走 reset-password 接口）。
 */
@Schema(name = "UserUpdateRequest", description = "更新后台用户")
public class UserUpdateRequest {

    @Schema(description = "昵称", example = "运营小王")
    @Size(max = 64, message = "昵称最长 64 字符")
    private String nickname;

    @Schema(description = "邮箱", example = "op@example.com")
    @Email(message = "邮箱格式非法")
    @Size(max = 128, message = "邮箱最长 128 字符")
    private String email;

    @Schema(description = "手机号", example = "13800138000")
    @Pattern(regexp = "^$|^1\\d{10}$", message = "手机号须为 11 位有效号码")
    private String phone;

    @Schema(description = "账号状态：NORMAL / DISABLED", example = "NORMAL")
    @Pattern(regexp = "^$|^(NORMAL|DISABLED)$", message = "状态仅支持 NORMAL 或 DISABLED")
    private String status;

    @Schema(description = "角色编码列表（覆盖式；不传则不修改角色）", example = "[\"OPERATOR\"]")
    private List<@Size(max = 64, message = "角色编码过长") String> roleCodes;

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public List<String> getRoleCodes() {
        return roleCodes;
    }

    public void setRoleCodes(List<String> roleCodes) {
        this.roleCodes = roleCodes;
    }
}
