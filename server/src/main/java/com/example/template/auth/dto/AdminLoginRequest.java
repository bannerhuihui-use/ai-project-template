package com.example.template.auth.dto;

import com.example.template.system.support.BuiltinConfigValidator;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 后台账号密码登录请求。
 */
@Schema(name = "AdminLoginRequest", description = "后台账号密码登录请求")
public class AdminLoginRequest {

    @Schema(description = "用户名", example = "admin", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "用户名不能为空")
    private String username;

    @Schema(description = "密码（明文，仅用于登录校验，不会被记录）", example = "123456",
            requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "密码不能为空")
    @Size(max = BuiltinConfigValidator.PASSWORD_MAX_LENGTH, message = "密码长度不能超过 64 位")
    private String password;

    @Schema(description = "设备 ID（可选，用于登录设备记录；为空则仅记录一条登录）", example = "web-chrome-001")
    @Size(max = 128, message = "设备 ID 过长")
    private String deviceId;

    @Schema(description = "设备名称（可选）", example = "Chrome on macOS")
    @Size(max = 128, message = "设备名称过长")
    private String deviceName;

    @Schema(description = "平台（可选）", example = "WEB")
    @Size(max = 64, message = "平台标识过长")
    private String platform;

    @Schema(description = "验证码 ID（开启验证码时必填）", example = "a1b2c3d4-e5f6-7890-abcd-ef1234567890")
    @Size(max = 64, message = "验证码 ID 过长")
    private String captchaId;

    @Schema(description = "验证码答案（开启验证码时必填）", example = "10")
    @Size(max = 16, message = "验证码答案过长")
    private String captchaAnswer;

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

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public String getDeviceName() {
        return deviceName;
    }

    public void setDeviceName(String deviceName) {
        this.deviceName = deviceName;
    }

    public String getPlatform() {
        return platform;
    }

    public void setPlatform(String platform) {
        this.platform = platform;
    }

    public String getCaptchaId() {
        return captchaId;
    }

    public void setCaptchaId(String captchaId) {
        this.captchaId = captchaId;
    }

    public String getCaptchaAnswer() {
        return captchaAnswer;
    }

    public void setCaptchaAnswer(String captchaAnswer) {
        this.captchaAnswer = captchaAnswer;
    }
}
