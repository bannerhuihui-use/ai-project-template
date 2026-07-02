package com.example.template.auth.support;

import com.example.template.common.BusinessException;
import com.example.template.common.ResultCode;
import org.springframework.util.StringUtils;

import java.util.regex.Pattern;

/**
 * 用户资料字段校验（与 sys_user 表字段长度及前端 formRules 对齐）。
 */
public final class UserProfileValidator {

    public static final int NICKNAME_MAX_LENGTH = 64;
    public static final int EMAIL_MAX_LENGTH = 128;
    public static final int USERNAME_MAX_LENGTH = 64;
    public static final int SEARCH_KEYWORD_MAX_LENGTH = 64;

    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[a-zA-Z0-9_]{2,64}$");
    private static final Pattern PHONE_PATTERN = Pattern.compile("^1\\d{10}$");
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[\\w.+-]+@[\\w.-]+\\.[a-zA-Z]{2,}$");

    private UserProfileValidator() {
    }

    public static void validateUsername(String username) {
        if (!StringUtils.hasText(username)) {
            return;
        }
        String trimmed = username.trim();
        if (trimmed.length() > USERNAME_MAX_LENGTH || !USERNAME_PATTERN.matcher(trimmed).matches()) {
            throw new BusinessException(ResultCode.PARAM_ERROR.getCode(),
                    "用户名须为 2~64 位字母、数字或下划线");
        }
    }

    public static void validateOptionalProfile(String nickname, String email, String phone) {
        if (StringUtils.hasText(nickname) && nickname.trim().length() > NICKNAME_MAX_LENGTH) {
            throw new BusinessException(ResultCode.PARAM_ERROR.getCode(),
                    "昵称长度不能超过 " + NICKNAME_MAX_LENGTH + " 字符");
        }
        if (StringUtils.hasText(email)) {
            String trimmed = email.trim();
            if (trimmed.length() > EMAIL_MAX_LENGTH || !EMAIL_PATTERN.matcher(trimmed).matches()) {
                throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), "邮箱格式非法");
            }
        }
        if (StringUtils.hasText(phone)) {
            String trimmed = phone.trim();
            if (!PHONE_PATTERN.matcher(trimmed).matches()) {
                throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), "手机号须为 11 位有效号码");
            }
        }
    }

    public static void validateSearchKeyword(String keyword, String fieldName) {
        if (StringUtils.hasText(keyword) && keyword.trim().length() > SEARCH_KEYWORD_MAX_LENGTH) {
            throw new BusinessException(ResultCode.PARAM_ERROR.getCode(),
                    fieldName + "长度不能超过 " + SEARCH_KEYWORD_MAX_LENGTH + " 字符");
        }
    }
}
