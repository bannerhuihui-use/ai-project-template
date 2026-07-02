package com.example.template.system;

import java.util.Map;
import java.util.Set;

/**
 * 系统配置键常量，避免魔法字符串散落。
 */
public final class SystemConfigKeys {

    public static final String SITE_NAME = "site.name";
    public static final String SITE_LOGO = "site.logo";
    public static final String SITE_COPYRIGHT = "site.copyright";
    public static final String CAPTCHA_ENABLED = "auth.login.captcha_enabled";
    public static final String PASSWORD_MIN_LENGTH = "auth.password.min_length";
    public static final String SESSION_IDLE_MINUTES = "auth.session.idle_minutes";
    public static final String MAINTENANCE_MODE = "system.maintenance_mode";
    public static final String AUDIT_RETENTION_DAYS = "system.audit.retention_days";

    private static final Set<String> BUILTIN_KEYS = Set.of(
            SITE_NAME, SITE_LOGO, SITE_COPYRIGHT,
            CAPTCHA_ENABLED, PASSWORD_MIN_LENGTH, SESSION_IDLE_MINUTES,
            MAINTENANCE_MODE, AUDIT_RETENTION_DAYS
    );

    /** 已接入业务逻辑的配置键（改值后即时或刷新后生效）。 */
    private static final Set<String> WIRED_KEYS = BUILTIN_KEYS;

    private static final Map<String, String> WIRED_SCOPES = Map.ofEntries(
            Map.entry(SITE_NAME, "登录页标题、侧边栏品牌"),
            Map.entry(SITE_LOGO, "登录页与侧边栏 Logo"),
            Map.entry(SITE_COPYRIGHT, "登录页页脚版权"),
            Map.entry(CAPTCHA_ENABLED, "后台登录验证码"),
            Map.entry(PASSWORD_MIN_LENGTH, "新建用户、改密、重置密码"),
            Map.entry(SESSION_IDLE_MINUTES, "后台空闲自动登出"),
            Map.entry(MAINTENANCE_MODE, "登录拦截（仅超级管理员可进）"),
            Map.entry(AUDIT_RETENTION_DAYS, "审计日志页保留天数展示")
    );

    private SystemConfigKeys() {
    }

    public static boolean isBuiltin(String configKey) {
        return configKey != null && BUILTIN_KEYS.contains(configKey);
    }

    public static boolean isWired(String configKey) {
        return configKey != null && WIRED_KEYS.contains(configKey);
    }

    /**
     * 已接入配置的生效范围说明（供管理端展示）。
     */
    public static String wiredScope(String configKey) {
        if (configKey == null) {
            return null;
        }
        return WIRED_SCOPES.get(configKey);
    }
}
