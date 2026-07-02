package com.example.template.auth.dto;

import com.example.template.system.support.BuiltinConfigValidator;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * 新建后台用户请求。
 */
@Schema(name = "UserCreateRequest", description = "新建后台用户")
public class UserCreateRequest {

    @Schema(description = "登录用户名", example = "operator1", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "用户名不能为空")
    @Size(min = 2, max = 64, message = "用户名长度 2~64")
    @Pattern(regexp = "^[a-zA-Z0-9_]{2,64}$", message = "用户名仅支持字母、数字、下划线")
    private String username;

    @Schema(description = "初始密码", example = "Pass@2026", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "密码不能为空")
    @Size(max = BuiltinConfigValidator.PASSWORD_MAX_LENGTH, message = "密码长度不能超过 64 位")
    private String password;

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

    @Schema(description = "角色编码列表", example = "[\"OPERATOR\"]")
    private List<@Size(max = 64, message = "角色编码过长") String> roleCodes;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

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

    public List<String> getRoleCodes() {
        return roleCodes;
    }

    public void setRoleCodes(List<String> roleCodes) {
        this.roleCodes = roleCodes;
    }
}
