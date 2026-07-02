package com.example.template.system.support;

import com.example.template.common.BusinessException;
import com.example.template.common.ResultCode;
import com.example.template.system.SystemConfigKeys;
import org.springframework.util.StringUtils;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Locale;

/**
 * 内置配置项的业务边界校验（保存时触发）。
 */
public final class BuiltinConfigValidator {

    static final int PASSWORD_MIN_LENGTH_LOW = 6;
    static final int PASSWORD_MIN_LENGTH_HIGH = 64;
    /** 密码最大长度（与 BCrypt 安全输入上限对齐）。 */
    public static final int PASSWORD_MAX_LENGTH = 64;
    static final int SESSION_IDLE_MINUTES_LOW = 5;
    static final int SESSION_IDLE_MINUTES_HIGH = 1440;
    static final int AUDIT_RETENTION_DAYS_LOW = 1;
    static final int AUDIT_RETENTION_DAYS_HIGH = 365;
    static final int SITE_NAME_MAX_LENGTH = 128;
    static final int SITE_COPYRIGHT_MAX_LENGTH = 256;
    static final int SITE_LOGO_MAX_LENGTH = 2048;

    private BuiltinConfigValidator() {
    }

    /**
     * 校验内置配置项取值；非内置键直接跳过。
     */
    public static void validate(String configKey, String value) {
        if (!SystemConfigKeys.isBuiltin(configKey)) {
            return;
        }
        String normalized = value == null ? "" : value.trim();
        switch (configKey) {
            case SystemConfigKeys.PASSWORD_MIN_LENGTH ->
                    validateIntRange(configKey, normalized, PASSWORD_MIN_LENGTH_LOW, PASSWORD_MIN_LENGTH_HIGH);
            case SystemConfigKeys.SESSION_IDLE_MINUTES ->
                    validateIntRange(configKey, normalized, SESSION_IDLE_MINUTES_LOW, SESSION_IDLE_MINUTES_HIGH);
            case SystemConfigKeys.AUDIT_RETENTION_DAYS ->
                    validateIntRange(configKey, normalized, AUDIT_RETENTION_DAYS_LOW, AUDIT_RETENTION_DAYS_HIGH);
            case SystemConfigKeys.SITE_NAME ->
                    validateMaxLength(configKey, normalized, SITE_NAME_MAX_LENGTH);
            case SystemConfigKeys.SITE_COPYRIGHT ->
                    validateMaxLength(configKey, normalized, SITE_COPYRIGHT_MAX_LENGTH);
            case SystemConfigKeys.SITE_LOGO ->
                    validateSiteLogo(normalized);
            default -> {
                // BOOLEAN 等由通用类型校验覆盖
            }
        }
    }

    /** 读取侧兜底：将整数配置钳制在合法区间内。 */
    public static int clampPasswordMinLength(int value) {
        return clamp(value, PASSWORD_MIN_LENGTH_LOW, PASSWORD_MIN_LENGTH_HIGH);
    }

    public static int clampSessionIdleMinutes(int value) {
        return clamp(value, SESSION_IDLE_MINUTES_LOW, SESSION_IDLE_MINUTES_HIGH);
    }

    public static int clampAuditRetentionDays(int value) {
        return clamp(value, AUDIT_RETENTION_DAYS_LOW, AUDIT_RETENTION_DAYS_HIGH);
    }

    private static void validateIntRange(String configKey, String value, int low, int high) {
        if (!StringUtils.hasText(value)) {
            throw invalid(configKey, "不能为空");
        }
        int parsed;
        try {
            parsed = Integer.parseInt(value);
        } catch (NumberFormatException e) {
            throw invalid(configKey, "须为整数");
        }
        if (parsed < low || parsed > high) {
            throw invalid(configKey, "须在 " + low + "~" + high + " 之间");
        }
    }

    private static void validateMaxLength(String configKey, String value, int maxLength) {
        if (value.length() > maxLength) {
            throw invalid(configKey, "长度不能超过 " + maxLength + " 字符");
        }
    }

    private static void validateSiteLogo(String value) {
        if (!StringUtils.hasText(value)) {
            return;
        }
        if (value.length() > SITE_LOGO_MAX_LENGTH) {
            throw invalid(SystemConfigKeys.SITE_LOGO, "URL 长度不能超过 " + SITE_LOGO_MAX_LENGTH + " 字符");
        }
        String lower = value.toLowerCase(Locale.ROOT);
        if (!lower.startsWith("http://") && !lower.startsWith("https://")) {
            throw invalid(SystemConfigKeys.SITE_LOGO, "仅支持 http:// 或 https:// 开头的图片地址");
        }
        try {
            URI uri = new URI(value);
            String scheme = uri.getScheme();
            if (scheme == null || (!"http".equalsIgnoreCase(scheme) && !"https".equalsIgnoreCase(scheme))) {
                throw invalid(SystemConfigKeys.SITE_LOGO, "仅支持 http:// 或 https:// 协议");
            }
        } catch (URISyntaxException e) {
            throw invalid(SystemConfigKeys.SITE_LOGO, "URL 格式非法");
        }
    }

    private static int clamp(int value, int low, int high) {
        return Math.max(low, Math.min(high, value));
    }

    private static BusinessException invalid(String configKey, String reason) {
        return new BusinessException(ResultCode.CONFIG_VALUE_INVALID.getCode(),
                "配置项 " + configKey + " 取值非法：" + reason);
    }
}
