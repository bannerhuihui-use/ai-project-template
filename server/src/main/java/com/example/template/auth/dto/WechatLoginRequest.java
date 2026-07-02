package com.example.template.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/**
 * 微信小程序登录请求（v1 占位，仅定义结构）。
 */
@Schema(name = "WechatLoginRequest", description = "微信小程序登录请求（v1 占位）")
public class WechatLoginRequest {

    @Schema(description = "微信小程序登录 code（wx.login 获取）", example = "wx-login-code",
            requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "code 不能为空")
    private String code;

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }
}
