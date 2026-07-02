package com.example.template.system.service;

import com.example.template.system.dto.CaptchaChallenge;

/**
 * 登录算术验证码服务。
 */
public interface LoginCaptchaService {

    /**
     * 签发一道新验证码。
     */
    CaptchaChallenge issue();

    /**
     * 校验验证码；失败抛出业务异常。
     */
    void verifyOrThrow(String captchaId, String answer);
}
