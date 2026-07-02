package com.example.template.auth.security;

/**
 * Redis 不可用信号异常。
 *
 * <p>鉴权链路采用 fail-closed 策略：黑名单校验时若 Redis 异常，抛出本异常，
 * 由过滤器拒绝访问（返回 503 {@code AUTH_SERVICE_UNAVAILABLE}），避免安全风险。</p>
 */
public class RedisUnavailableException extends RuntimeException {

    public RedisUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
