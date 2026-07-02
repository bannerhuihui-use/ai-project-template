package com.example.template.system.support;

import com.example.template.common.BusinessException;
import com.example.template.common.ResultCode;
import com.example.template.system.SystemConfigKeys;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class BuiltinConfigValidatorTest {

    @Test
    void passwordMinLength_inRange_ok() {
        assertDoesNotThrow(() -> BuiltinConfigValidator.validate(
                SystemConfigKeys.PASSWORD_MIN_LENGTH, "8"));
    }

    @Test
    void passwordMinLength_tooLow_rejected() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> BuiltinConfigValidator.validate(SystemConfigKeys.PASSWORD_MIN_LENGTH, "3"));
        assertEquals(ResultCode.CONFIG_VALUE_INVALID.getCode(), ex.getCode());
    }

    @Test
    void siteLogo_http_ok() {
        assertDoesNotThrow(() -> BuiltinConfigValidator.validate(
                SystemConfigKeys.SITE_LOGO, "https://cdn.example.com/logo.png"));
    }

    @Test
    void siteLogo_empty_ok() {
        assertDoesNotThrow(() -> BuiltinConfigValidator.validate(SystemConfigKeys.SITE_LOGO, ""));
    }

    @Test
    void siteLogo_javascript_rejected() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> BuiltinConfigValidator.validate(
                        SystemConfigKeys.SITE_LOGO, "javascript:alert(1)"));
        assertEquals(ResultCode.CONFIG_VALUE_INVALID.getCode(), ex.getCode());
    }

    @Test
    void customKey_skipped() {
        assertDoesNotThrow(() -> BuiltinConfigValidator.validate("custom.foo", "anything"));
    }

    @Test
    void clampPasswordMinLength() {
        assertEquals(6, BuiltinConfigValidator.clampPasswordMinLength(1));
        assertEquals(64, BuiltinConfigValidator.clampPasswordMinLength(100));
        assertEquals(10, BuiltinConfigValidator.clampPasswordMinLength(10));
    }
}
