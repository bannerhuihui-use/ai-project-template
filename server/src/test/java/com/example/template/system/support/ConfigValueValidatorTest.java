package com.example.template.system.support;

import com.example.template.common.BusinessException;
import com.example.template.common.ResultCode;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * ConfigValueValidator 单元测试。
 */
class ConfigValueValidatorTest {

    @Test
    void json_emptyAllowed() {
        ConfigValueValidator.validate("JSON", "");
        assertEquals("", ConfigValueValidator.normalizeJson(""));
    }

    @Test
    void json_objectValid() {
        String json = "  {\"a\": 1, \"b\": \"x\"}  ";
        ConfigValueValidator.validate("JSON", json);
        assertEquals("{\"a\": 1, \"b\": \"x\"}", ConfigValueValidator.normalizeJson(json));
    }

    @Test
    void json_arrayValid() {
        String json = "[1, 2, 3]";
        ConfigValueValidator.validate("JSON", json);
        assertEquals(json, ConfigValueValidator.normalizeJson(json));
    }

    @Test
    void json_invalidSyntax() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> ConfigValueValidator.validate("JSON", "{not-json}"));
        assertEquals(ResultCode.CONFIG_VALUE_INVALID.getCode(), ex.getCode());
    }

    @Test
    void json_looksLikeJsonButInvalid() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> ConfigValueValidator.validate("JSON", "{\"a\":}"));
        assertEquals(ResultCode.CONFIG_VALUE_INVALID.getCode(), ex.getCode());
    }

    @Test
    void json_primitiveRejected() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> ConfigValueValidator.validate("JSON", "123"));
        assertEquals(ResultCode.CONFIG_VALUE_INVALID.getCode(), ex.getCode());
    }

    @Test
    void json_tooLong() {
        String tooLong = "{\"k\":\"" + "x".repeat(ConfigValueValidator.JSON_MAX_LENGTH) + "\"}";
        BusinessException ex = assertThrows(BusinessException.class,
                () -> ConfigValueValidator.validate("JSON", tooLong));
        assertEquals(ResultCode.CONFIG_VALUE_INVALID.getCode(), ex.getCode());
    }

    @Test
    void boolean_onlyTrueFalse() {
        ConfigValueValidator.validate("BOOLEAN", "true");
        ConfigValueValidator.validate("BOOLEAN", "false");
        assertThrows(BusinessException.class, () -> ConfigValueValidator.validate("BOOLEAN", "yes"));
    }

    @Test
    void number_validAndInvalid() {
        ConfigValueValidator.validate("NUMBER", "3.14");
        assertThrows(BusinessException.class, () -> ConfigValueValidator.validate("NUMBER", "abc"));
    }

    @Test
    void unsupportedType() {
        assertThrows(BusinessException.class, () -> ConfigValueValidator.validate("XML", "{}"));
    }
}
