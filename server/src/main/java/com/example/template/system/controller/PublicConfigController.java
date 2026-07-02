package com.example.template.system.controller;

import com.example.template.common.Result;
import com.example.template.system.dto.AppPublicConfig;
import com.example.template.system.dto.CaptchaChallenge;
import com.example.template.system.service.LoginCaptchaService;
import com.example.template.system.service.SystemConfigService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 公开配置接口（无需登录）。
 */
@Tag(name = "公开配置", description = "登录页与前端壳读取的应用配置")
@RestController
@RequestMapping("/api/v1/public")
public class PublicConfigController {

    private final SystemConfigService systemConfigService;
    private final LoginCaptchaService loginCaptchaService;

    public PublicConfigController(SystemConfigService systemConfigService,
                                  LoginCaptchaService loginCaptchaService) {
        this.systemConfigService = systemConfigService;
        this.loginCaptchaService = loginCaptchaService;
    }

    @Operation(summary = "获取公开应用配置", description = "站点信息、维护模式、密码策略、空闲超时等")
    @ApiResponse(responseCode = "200", description = "成功")
    @GetMapping("/app-config")
    public Result<AppPublicConfig> appConfig() {
        return Result.success(systemConfigService.getAppPublicConfig());
    }

    @Operation(summary = "获取登录验证码", description = "仅在配置开启验证码时由前端调用")
    @ApiResponse(responseCode = "200", description = "成功")
    @GetMapping("/captcha")
    public Result<CaptchaChallenge> captcha() {
        if (!systemConfigService.isCaptchaEnabled()) {
            return Result.success(null);
        }
        return Result.success(loginCaptchaService.issue());
    }
}
