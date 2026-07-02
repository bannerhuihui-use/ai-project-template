package com.example.template.system.service.impl;

import com.example.template.common.BusinessException;
import com.example.template.common.ResultCode;
import com.example.template.system.dto.CaptchaChallenge;
import com.example.template.system.service.LoginCaptchaService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.security.SecureRandom;
import java.time.Duration;
import java.util.UUID;

/**
 * 基于 Redis 的算术验证码实现。
 */
@Service
public class LoginCaptchaServiceImpl implements LoginCaptchaService {

    private static final Logger log = LoggerFactory.getLogger(LoginCaptchaServiceImpl.class);

    private static final String REDIS_KEY_PREFIX = "auth:captcha:";
    private static final Duration TTL = Duration.ofMinutes(5);
    private static final SecureRandom RANDOM = new SecureRandom();

    private final StringRedisTemplate redisTemplate;

    public LoginCaptchaServiceImpl(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public CaptchaChallenge issue() {
        int left = RANDOM.nextInt(9) + 1;
        int right = RANDOM.nextInt(9) + 1;
        String captchaId = UUID.randomUUID().toString();
        String answer = String.valueOf(left + right);
        String key = REDIS_KEY_PREFIX + captchaId;
        try {
            redisTemplate.opsForValue().set(key, answer, TTL);
        } catch (Exception e) {
            log.warn("验证码写入 Redis 失败: captchaId={}", captchaId);
            throw new BusinessException(ResultCode.AUTH_SERVICE_UNAVAILABLE);
        }
        return new CaptchaChallenge(captchaId, left + " + " + right + " = ?");
    }

    @Override
    public void verifyOrThrow(String captchaId, String answer) {
        if (!StringUtils.hasText(captchaId) || !StringUtils.hasText(answer)) {
            throw new BusinessException(ResultCode.AUTH_CAPTCHA_INVALID);
        }
        String key = REDIS_KEY_PREFIX + captchaId.trim();
        String expected;
        try {
            expected = redisTemplate.opsForValue().get(key);
        } catch (Exception e) {
            log.warn("验证码读取 Redis 失败: captchaId={}", captchaId.trim());
            throw new BusinessException(ResultCode.AUTH_SERVICE_UNAVAILABLE);
        }
        if (!StringUtils.hasText(expected)) {
            throw new BusinessException(ResultCode.AUTH_CAPTCHA_INVALID);
        }
        String normalized = answer.trim();
        if (!expected.equals(normalized)) {
            throw new BusinessException(ResultCode.AUTH_CAPTCHA_INVALID);
        }
        try {
            redisTemplate.delete(key);
        } catch (Exception e) {
            log.warn("验证码删除 Redis 失败: captchaId={}", captchaId.trim());
        }
    }
}
