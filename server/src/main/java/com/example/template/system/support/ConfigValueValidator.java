package com.example.template.system.support;

import com.example.template.common.BusinessException;
import com.example.template.common.ResultCode;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.util.StringUtils;

import java.util.Locale;
import java.util.Set;

/**
 * 系统配置值类型校验（STRING / NUMBER / BOOLEAN / JSON）。
 *
 * <p>JSON 使用 Jackson 解析，仅允许对象或数组根节点；空串视为合法空值。</p>
 */
public final class ConfigValueValidator {

    /** JSON 配置值最大字符数（与前端 configConstraints 对齐）。 */
    public static final int JSON_MAX_LENGTH = 65_536;

    private static final Set<String> SUPPORTED_VALUE_TYPES = Set.of("STRING", "NUMBER", "BOOLEAN", "JSON");

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private ConfigValueValidator() {
    }

    /**
     * 按值类型校验配置取值。
     *
     * @param valueType 值类型
     * @param value     配置值（可为空串）
     */
    public static void validate(String valueType, String value) {
        if (!StringUtils.hasText(valueType)) {
            throw new BusinessException(ResultCode.CONFIG_VALUE_INVALID.getCode(), "值类型不能为空");
        }
        String type = valueType.trim().toUpperCase(Locale.ROOT);
        if (!SUPPORTED_VALUE_TYPES.contains(type)) {
            throw new BusinessException(ResultCode.CONFIG_VALUE_INVALID.getCode(),
                    "不支持的值类型: " + valueType);
        }
        String normalized = value == null ? "" : value;
        switch (type) {
            case "NUMBER" -> validateNumber(normalized);
            case "BOOLEAN" -> validateBoolean(normalized);
            case "JSON" -> validateJson(normalized);
            default -> {
                // STRING: no extra validation
            }
        }
    }

    /**
     * 规范化并校验 JSON 配置值；通过则返回 trim 后的字符串（空串保持不变）。
     */
    public static String normalizeJson(String value) {
        if (!StringUtils.hasText(value)) {
            return "";
        }
        String trimmed = value.trim();
        validateJson(trimmed);
        return trimmed;
    }

    private static void validateNumber(String value) {
        if (!StringUtils.hasText(value)) {
            return;
        }
        try {
            Double.parseDouble(value.trim());
        } catch (NumberFormatException e) {
            throw new BusinessException(ResultCode.CONFIG_VALUE_INVALID.getCode(), "数值格式非法");
        }
    }

    private static void validateBoolean(String value) {
        if (!StringUtils.hasText(value)) {
            return;
        }
        String trimmed = value.trim();
        if (!"true".equalsIgnoreCase(trimmed) && !"false".equalsIgnoreCase(trimmed)) {
            throw new BusinessException(ResultCode.CONFIG_VALUE_INVALID.getCode(),
                    "布尔值仅支持 true 或 false");
        }
    }

    private static void validateJson(String value) {
        if (!StringUtils.hasText(value)) {
            return;
        }
        String trimmed = value.trim();
        if (trimmed.length() > JSON_MAX_LENGTH) {
            throw new BusinessException(ResultCode.CONFIG_VALUE_INVALID.getCode(),
                    "JSON 长度不能超过 " + JSON_MAX_LENGTH + " 字符");
        }
        try {
            JsonNode node = OBJECT_MAPPER.readTree(trimmed);
            if (!node.isObject() && !node.isArray()) {
                throw new BusinessException(ResultCode.CONFIG_VALUE_INVALID.getCode(),
                        "JSON 根节点须为对象或数组");
            }
        } catch (JsonProcessingException e) {
            throw new BusinessException(ResultCode.CONFIG_VALUE_INVALID.getCode(), "JSON 格式非法");
        }
    }
}
