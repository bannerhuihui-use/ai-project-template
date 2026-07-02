package com.example.template.auth.config;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.Arrays;

/**
 * 生产环境 JWT secret 安全校验。
 *
 * <p>当 active profile 含 {@code prod} 时，若 secret 为空、等于模板默认值或长度不足，
 * 直接抛异常中断启动，避免带着弱密钥上生产。</p>
 *
 * <p>安全约束：任何分支都<b>不打印 secret 明文</b>，仅给出修复指引。</p>
 */
@Component
public class AuthSecretValidator {

    private static final Logger log = LoggerFactory.getLogger(AuthSecretValidator.class);

    /** 生产 profile 标识。 */
    private static final String PROD_PROFILE = "prod";
    /** 模板内置默认值（application.yml 中 JWT_SECRET 的兜底值），生产严禁沿用。 */
    private static final String DEFAULT_SECRET = "please-change-this-secret-please-change-this-secret";
    /** HS256 安全下限：派生前的原始密钥建议至少 32 字符。 */
    private static final int MIN_SECRET_LENGTH = 32;

    private final Environment environment;
    private final AuthProperties authProperties;

    public AuthSecretValidator(Environment environment, AuthProperties authProperties) {
        this.environment = environment;
        this.authProperties = authProperties;
    }

    @PostConstruct
    public void validate() {
        boolean prod = Arrays.asList(environment.getActiveProfiles()).contains(PROD_PROFILE);
        if (!prod) {
            return;
        }

        String secret = authProperties.getJwt().getSecret();
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException(
                    "生产环境必须通过环境变量 JWT_SECRET 配置非空 JWT 签名密钥，当前为空。");
        }
        if (DEFAULT_SECRET.equals(secret.trim())) {
            throw new IllegalStateException(
                    "生产环境检测到 JWT secret 仍为模板默认值，请改用安全随机密钥（环境变量 JWT_SECRET）。");
        }
        if (secret.trim().length() < MIN_SECRET_LENGTH) {
            throw new IllegalStateException(
                    "生产环境 JWT secret 长度过短，至少需要 " + MIN_SECRET_LENGTH + " 个字符。");
        }
        log.info("JWT secret 生产校验通过（prod profile）。");
    }
}
