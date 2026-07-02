package com.example.template.system.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 面向前端的公开应用配置（登录页、布局、策略提示）。
 */
@Schema(name = "AppPublicConfig", description = "公开应用配置")
public class AppPublicConfig {

    @Schema(description = "站点名称", example = "管理后台")
    private String siteName;

    @Schema(description = "站点 Logo URL", example = "https://cdn.example.com/logo.png")
    private String siteLogo;

    @Schema(description = "页脚版权文案", example = "© 2026 Template")
    private String siteCopyright;

    @Schema(description = "是否开启维护模式（开启后仅超级管理员可登录）", example = "false")
    private boolean maintenanceMode;

    @Schema(description = "登录验证码是否开启", example = "false")
    private boolean captchaEnabled;

    @Schema(description = "密码最小长度", example = "8")
    private int passwordMinLength;

    @Schema(description = "空闲超时（分钟），前端空闲登出参考值", example = "120")
    private int sessionIdleMinutes;

    @Schema(description = "审计日志保留天数（展示说明）", example = "30")
    private int auditRetentionDays;

    public String getSiteName() {
        return siteName;
    }

    public void setSiteName(String siteName) {
        this.siteName = siteName;
    }

    public String getSiteLogo() {
        return siteLogo;
    }

    public void setSiteLogo(String siteLogo) {
        this.siteLogo = siteLogo;
    }

    public String getSiteCopyright() {
        return siteCopyright;
    }

    public void setSiteCopyright(String siteCopyright) {
        this.siteCopyright = siteCopyright;
    }

    public boolean isMaintenanceMode() {
        return maintenanceMode;
    }

    public void setMaintenanceMode(boolean maintenanceMode) {
        this.maintenanceMode = maintenanceMode;
    }

    public boolean isCaptchaEnabled() {
        return captchaEnabled;
    }

    public void setCaptchaEnabled(boolean captchaEnabled) {
        this.captchaEnabled = captchaEnabled;
    }

    public int getPasswordMinLength() {
        return passwordMinLength;
    }

    public void setPasswordMinLength(int passwordMinLength) {
        this.passwordMinLength = passwordMinLength;
    }

    public int getSessionIdleMinutes() {
        return sessionIdleMinutes;
    }

    public void setSessionIdleMinutes(int sessionIdleMinutes) {
        this.sessionIdleMinutes = sessionIdleMinutes;
    }

    public int getAuditRetentionDays() {
        return auditRetentionDays;
    }

    public void setAuditRetentionDays(int auditRetentionDays) {
        this.auditRetentionDays = auditRetentionDays;
    }
}
