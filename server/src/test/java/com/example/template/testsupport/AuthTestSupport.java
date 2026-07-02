package com.example.template.testsupport;

import com.example.template.common.BusinessException;
import com.example.template.common.ResultCode;
import com.example.template.system.dto.AppPublicConfig;
import com.example.template.system.dto.CaptchaChallenge;
import com.example.template.system.dto.ConfigBatchUpdateRequest;
import com.example.template.system.dto.ConfigCreateRequest;
import com.example.template.system.dto.ConfigItem;
import com.example.template.system.dto.ConfigMetaUpdateRequest;
import com.example.template.system.service.LoginCaptchaService;
import com.example.template.system.service.SystemConfigService;

import java.util.List;

/**
 * 鉴权相关单元测试的公共桩实现。
 */
public final class AuthTestSupport {

    private AuthTestSupport() {
    }

    public static SystemConfigService defaultSystemConfig() {
        return systemConfigWithPasswordMinLength(8);
    }

    public static SystemConfigService systemConfigWithPasswordMinLength(int minLength) {
        return new SystemConfigService() {
            @Override
            public List<ConfigItem> listConfigs(String group) {
                return List.of();
            }

            @Override
            public void batchUpdate(ConfigBatchUpdateRequest request) {
            }

            @Override
            public Long create(ConfigCreateRequest request) {
                return 1L;
            }

            @Override
            public void updateMetadata(Long id, ConfigMetaUpdateRequest request) {
            }

            @Override
            public void delete(Long id) {
            }

            @Override
            public AppPublicConfig getAppPublicConfig() {
                AppPublicConfig config = new AppPublicConfig();
                config.setPasswordMinLength(minLength);
                return config;
            }

            @Override
            public void refreshCache() {
            }

            @Override
            public String getString(String configKey, String defaultValue) {
                return defaultValue;
            }

            @Override
            public boolean getBoolean(String configKey, boolean defaultValue) {
                return defaultValue;
            }

            @Override
            public int getInt(String configKey, int defaultValue) {
                return defaultValue;
            }

            @Override
            public boolean isMaintenanceMode() {
                return false;
            }

            @Override
            public boolean isCaptchaEnabled() {
                return false;
            }

            @Override
            public int getPasswordMinLength() {
                return minLength;
            }

            @Override
            public int getSessionIdleMinutes() {
                return 120;
            }

            @Override
            public int getAuditRetentionDays() {
                return 30;
            }

            @Override
            public void validatePasswordLength(String password) {
                if (password == null || password.length() < minLength) {
                    throw new BusinessException(ResultCode.AUTH_PASSWORD_TOO_SHORT.getCode(),
                            "密码至少 " + minLength + " 位");
                }
            }
        };
    }

    public static LoginCaptchaService noopCaptcha() {
        return new LoginCaptchaService() {
            @Override
            public CaptchaChallenge issue() {
                return new CaptchaChallenge("test-id", "1 + 1 = ?");
            }

            @Override
            public void verifyOrThrow(String captchaId, String answer) {
            }
        };
    }
}
