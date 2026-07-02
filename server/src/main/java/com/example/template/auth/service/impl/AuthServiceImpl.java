package com.example.template.auth.service.impl;

import com.example.template.auth.dto.AdminLoginRequest;
import com.example.template.auth.dto.AuthUserInfo;
import com.example.template.auth.dto.ChangePasswordRequest;
import com.example.template.auth.dto.LoginContext;
import com.example.template.auth.dto.LoginResponse;
import com.example.template.auth.dto.LogoutRequest;
import com.example.template.auth.dto.RefreshTokenRequest;
import com.example.template.auth.dto.WechatLoginRequest;
import com.example.template.auth.model.AccountStatus;
import com.example.template.auth.model.IdentityType;
import com.example.template.auth.model.LoginDevice;
import com.example.template.auth.model.SysUser;
import com.example.template.auth.model.TokenType;
import com.example.template.auth.model.UserIdentity;
import com.example.template.auth.model.UserType;
import com.example.template.auth.repository.LoginDeviceRepository;
import com.example.template.auth.repository.SysRoleRepository;
import com.example.template.auth.repository.SysUserRepository;
import com.example.template.auth.repository.UserIdentityRepository;
import com.example.template.auth.security.CurrentUser;
import com.example.template.auth.security.JwtTokenProvider;
import com.example.template.auth.security.ParsedToken;
import com.example.template.auth.security.RedisUnavailableException;
import com.example.template.auth.security.SecurityUtils;
import com.example.template.auth.security.TokenBlacklistService;
import com.example.template.auth.service.AuthService;
import com.example.template.common.BusinessException;
import com.example.template.common.ResultCode;
import com.example.template.system.service.LoginCaptchaService;
import com.example.template.system.service.SystemConfigService;
import io.jsonwebtoken.JwtException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

/**
 * 鉴权业务实现。
 */
@Service
public class AuthServiceImpl implements AuthService {

    private static final String TOKEN_TYPE_BEARER = "Bearer";
    private static final String ROLE_SUPER_ADMIN = "SUPER_ADMIN";

    private static final Logger log = LoggerFactory.getLogger(AuthServiceImpl.class);

    private final UserIdentityRepository userIdentityRepository;
    private final SysUserRepository sysUserRepository;
    private final SysRoleRepository sysRoleRepository;
    private final LoginDeviceRepository loginDeviceRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final TokenBlacklistService tokenBlacklistService;
    private final PasswordEncoder passwordEncoder;
    private final SystemConfigService systemConfigService;
    private final LoginCaptchaService loginCaptchaService;

    public AuthServiceImpl(UserIdentityRepository userIdentityRepository,
                           SysUserRepository sysUserRepository,
                           SysRoleRepository sysRoleRepository,
                           LoginDeviceRepository loginDeviceRepository,
                           JwtTokenProvider jwtTokenProvider,
                           TokenBlacklistService tokenBlacklistService,
                           PasswordEncoder passwordEncoder,
                           SystemConfigService systemConfigService,
                           LoginCaptchaService loginCaptchaService) {
        this.userIdentityRepository = userIdentityRepository;
        this.sysUserRepository = sysUserRepository;
        this.sysRoleRepository = sysRoleRepository;
        this.loginDeviceRepository = loginDeviceRepository;
        this.jwtTokenProvider = jwtTokenProvider;
        this.tokenBlacklistService = tokenBlacklistService;
        this.passwordEncoder = passwordEncoder;
        this.systemConfigService = systemConfigService;
        this.loginCaptchaService = loginCaptchaService;
    }

    @Override
    @Transactional
    public LoginResponse adminLogin(AdminLoginRequest request, LoginContext context) {
        if (systemConfigService.isCaptchaEnabled()) {
            loginCaptchaService.verifyOrThrow(request.getCaptchaId(), request.getCaptchaAnswer());
        }

        UserIdentity identity = userIdentityRepository
                .findActiveByTypeAndIdentifier(IdentityType.USERNAME.name(), request.getUsername())
                // 账号不存在与密码错误返回同一错误，避免账号枚举
                .orElseThrow(() -> new BusinessException(ResultCode.AUTH_BAD_CREDENTIALS.getCode(),
                        ResultCode.AUTH_BAD_CREDENTIALS.getMessage()));

        if (!AccountStatus.NORMAL.name().equals(identity.getStatus())) {
            throw new BusinessException(ResultCode.AUTH_ACCOUNT_DISABLED.getCode(),
                    ResultCode.AUTH_ACCOUNT_DISABLED.getMessage());
        }

        if (!StringUtils.hasText(identity.getCredential())
                || !passwordEncoder.matches(request.getPassword(), identity.getCredential())) {
            throw new BusinessException(ResultCode.AUTH_BAD_CREDENTIALS.getCode(),
                    ResultCode.AUTH_BAD_CREDENTIALS.getMessage());
        }

        SysUser user = sysUserRepository.findActiveById(identity.getUserId())
                .orElseThrow(() -> new BusinessException(ResultCode.AUTH_BAD_CREDENTIALS.getCode(),
                        ResultCode.AUTH_BAD_CREDENTIALS.getMessage()));

        if (!UserType.ADMIN.name().equals(user.getUserType())) {
            throw new BusinessException(ResultCode.AUTH_NOT_ADMIN.getCode(),
                    ResultCode.AUTH_NOT_ADMIN.getMessage());
        }
        if (!AccountStatus.NORMAL.name().equals(user.getStatus())) {
            throw new BusinessException(ResultCode.AUTH_ACCOUNT_DISABLED.getCode(),
                    ResultCode.AUTH_ACCOUNT_DISABLED.getMessage());
        }

        List<String> roles = sysRoleRepository.findRoleCodesByUserId(user.getId());

        if (systemConfigService.isMaintenanceMode() && !roles.contains(ROLE_SUPER_ADMIN)) {
            throw new BusinessException(ResultCode.SYSTEM_MAINTENANCE);
        }

        userIdentityRepository.updateLastLoginTime(identity.getId());
        sysUserRepository.updateLastLoginTime(user.getId());
        recordLoginDevice(user.getId(), IdentityType.USERNAME.name(), request, context);

        log.info("管理员登录成功: userId={}", user.getId());
        return issueTokens(user, IdentityType.USERNAME.name(), roles);
    }

    /**
     * 记录登录设备。防御式：任何异常都不影响登录主流程。
     */
    private void recordLoginDevice(Long userId, String identityType, AdminLoginRequest request, LoginContext context) {
        try {
            String ip = context == null ? null : context.ip();
            String userAgent = context == null ? null : context.userAgent();
            LoginDevice device = new LoginDevice(userId, identityType, request.getDeviceId(),
                    request.getDeviceName(), request.getPlatform(), ip, userAgent);
            loginDeviceRepository.recordLogin(device);
        } catch (RuntimeException e) {
            log.warn("记录登录设备失败（不影响登录）: userId={}, error={}", userId, e.getMessage());
        }
    }

    @Override
    public LoginResponse wechatLogin(WechatLoginRequest request) {
        log.info("收到微信登录请求（v1 占位，未接入 wechat-miniapp）");
        throw new BusinessException(ResultCode.WECHAT_LOGIN_NOT_SUPPORTED.getCode(),
                ResultCode.WECHAT_LOGIN_NOT_SUPPORTED.getMessage());
    }

    @Override
    @Transactional
    public LoginResponse refreshToken(RefreshTokenRequest request) {
        ParsedToken parsed;
        try {
            parsed = jwtTokenProvider.parse(request.getRefreshToken());
        } catch (JwtException | IllegalArgumentException e) {
            log.warn("refresh token 校验失败：{}", e.getMessage());
            throw new BusinessException(ResultCode.AUTH_REFRESH_TOKEN_INVALID.getCode(),
                    ResultCode.AUTH_REFRESH_TOKEN_INVALID.getMessage());
        }

        if (parsed.tokenType() != TokenType.REFRESH) {
            throw new BusinessException(ResultCode.AUTH_TOKEN_TYPE_INVALID.getCode(),
                    ResultCode.AUTH_TOKEN_TYPE_INVALID.getMessage());
        }

        // refresh token 黑名单校验：被拉黑（含已轮换的旧 refresh）不可再用。
        // 重放处置（B2）：命中黑名单视为疑似重放/盗用，强制整账号失效后再拒绝。
        try {
            if (tokenBlacklistService.isBlacklisted(parsed.jti())) {
                sysUserRepository.incrementTokenVersion(parsed.userId());
                log.warn("检测到已失效 refresh 被重放，已强制整账号失效: userId={}", parsed.userId());
                throw new BusinessException(ResultCode.AUTH_TOKEN_BLACKLISTED.getCode(),
                        ResultCode.AUTH_TOKEN_BLACKLISTED.getMessage());
            }
        } catch (RedisUnavailableException e) {
            throw new BusinessException(ResultCode.AUTH_SERVICE_UNAVAILABLE.getCode(),
                    ResultCode.AUTH_SERVICE_UNAVAILABLE.getMessage());
        }

        SysUser user = sysUserRepository.findActiveById(parsed.userId())
                .orElseThrow(() -> new BusinessException(ResultCode.AUTH_REFRESH_TOKEN_INVALID.getCode(),
                        ResultCode.AUTH_REFRESH_TOKEN_INVALID.getMessage()));
        if (!AccountStatus.NORMAL.name().equals(user.getStatus())) {
            throw new BusinessException(ResultCode.AUTH_ACCOUNT_DISABLED.getCode(),
                    ResultCode.AUTH_ACCOUNT_DISABLED.getMessage());
        }
        if (!Objects.equals(user.getTokenVersion(), parsed.tokenVersion())) {
            throw new BusinessException(ResultCode.AUTH_TOKEN_VERSION_EXPIRED.getCode(),
                    ResultCode.AUTH_TOKEN_VERSION_EXPIRED.getMessage());
        }

        List<String> roles = sysRoleRepository.findRoleCodesByUserId(user.getId());

        if (systemConfigService.isMaintenanceMode() && !roles.contains(ROLE_SUPER_ADMIN)) {
            throw new BusinessException(ResultCode.SYSTEM_MAINTENANCE);
        }

        // 轮换：先拉黑旧 refresh（防重放），成功后再签发新令牌。
        // 黑名单启用时若拉黑失败（Redis 异常），必须中断——否则旧 refresh 仍可重放。
        // 黑名单关闭时 blacklist() 为 no-op，不抛异常；此时防重放退化为依赖较短 TTL + token_version。
        try {
            tokenBlacklistService.blacklist(parsed.jti(), remainingSeconds(parsed.expiresAt()));
        } catch (RedisUnavailableException e) {
            log.warn("刷新令牌时拉黑旧 refresh 失败，已拒绝刷新: userId={}", user.getId());
            throw new BusinessException(ResultCode.AUTH_SERVICE_UNAVAILABLE.getCode(),
                    ResultCode.AUTH_SERVICE_UNAVAILABLE.getMessage());
        }

        log.info("刷新令牌成功: userId={}", user.getId());
        return issueTokens(user, parsed.identityType(), roles);
    }

    @Override
    @Transactional
    public LoginResponse changePassword(ChangePasswordRequest request) {
        CurrentUser currentUser = SecurityUtils.getCurrentUser();
        if (currentUser == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED.getCode(), ResultCode.UNAUTHORIZED.getMessage());
        }

        String identityType = currentUser.getIdentityType();
        // 仅对存在密码凭证的身份（如 USERNAME）有效；无密码身份（如微信）按原密码错误处理，避免信息泄露
        UserIdentity identity = userIdentityRepository
                .findActiveByUserIdAndType(currentUser.getUserId(), identityType)
                .orElseThrow(() -> new BusinessException(ResultCode.AUTH_OLD_PASSWORD_INCORRECT.getCode(),
                        ResultCode.AUTH_OLD_PASSWORD_INCORRECT.getMessage()));

        if (!StringUtils.hasText(identity.getCredential())
                || !passwordEncoder.matches(request.getOldPassword(), identity.getCredential())) {
            throw new BusinessException(ResultCode.AUTH_OLD_PASSWORD_INCORRECT.getCode(),
                    ResultCode.AUTH_OLD_PASSWORD_INCORRECT.getMessage());
        }
        if (passwordEncoder.matches(request.getNewPassword(), identity.getCredential())) {
            throw new BusinessException(ResultCode.AUTH_NEW_PASSWORD_SAME.getCode(),
                    ResultCode.AUTH_NEW_PASSWORD_SAME.getMessage());
        }
        systemConfigService.validatePasswordLength(request.getNewPassword());

        userIdentityRepository.updateCredentialByUserIdAndType(
                currentUser.getUserId(), identityType, passwordEncoder.encode(request.getNewPassword()));
        sysUserRepository.updatePasswordUpdatedAt(currentUser.getUserId());
        // 改密即让旧 access/refresh 全失效
        sysUserRepository.incrementTokenVersion(currentUser.getUserId());

        // 续签：用自增后的 token_version 重新签发（重查用户以取最新版本）
        SysUser user = sysUserRepository.findActiveById(currentUser.getUserId())
                .orElseThrow(() -> new BusinessException(ResultCode.AUTH_TOKEN_INVALID.getCode(),
                        ResultCode.AUTH_TOKEN_INVALID.getMessage()));
        List<String> roles = sysRoleRepository.findRoleCodesByUserId(user.getId());

        log.info("用户改密成功，已续签新令牌: userId={}", user.getId());
        return issueTokens(user, identityType, roles);
    }

    @Override
    public AuthUserInfo currentUser() {
        CurrentUser currentUser = SecurityUtils.getCurrentUser();
        if (currentUser == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED.getCode(), ResultCode.UNAUTHORIZED.getMessage());
        }
        SysUser user = sysUserRepository.findActiveById(currentUser.getUserId())
                .orElseThrow(() -> new BusinessException(ResultCode.AUTH_TOKEN_INVALID.getCode(),
                        ResultCode.AUTH_TOKEN_INVALID.getMessage()));
        return new AuthUserInfo(user.getId(), user.getUserType(), user.getNickname(), currentUser.getRoles());
    }

    @Override
    public void logout(LogoutRequest request) {
        CurrentUser currentUser = SecurityUtils.getCurrentUser();
        if (currentUser == null) {
            return;
        }

        if (!tokenBlacklistService.isEnabled()) {
            // 黑名单关闭：回退为整账号失效（该用户所有 access/refresh 立即作废）
            sysUserRepository.incrementTokenVersion(currentUser.getUserId());
            log.info("用户退出登录(黑名单关闭，回退 token_version 自增): userId={}", currentUser.getUserId());
            return;
        }

        try {
            // 拉黑当前 access token
            blacklist(currentUser.getJti(), currentUser.getExpiresAt());
            // 可选：拉黑传入的 refresh token
            blacklistRefreshIfPresent(request);
            log.info("用户退出登录: userId={}", currentUser.getUserId());
        } catch (RedisUnavailableException e) {
            // Redis 异常时回退为整账号失效，确保退出确实生效
            sysUserRepository.incrementTokenVersion(currentUser.getUserId());
            log.warn("退出登录写黑名单失败，已回退 token_version 自增: userId={}", currentUser.getUserId());
        }
    }

    private void blacklistRefreshIfPresent(LogoutRequest request) {
        if (request == null || !StringUtils.hasText(request.getRefreshToken())) {
            return;
        }
        try {
            ParsedToken refresh = jwtTokenProvider.parse(request.getRefreshToken());
            if (refresh.tokenType() == TokenType.REFRESH) {
                blacklist(refresh.jti(), refresh.expiresAt());
            }
        } catch (JwtException | IllegalArgumentException e) {
            // 传入的 refresh token 无效则忽略，不影响 access token 拉黑
            log.warn("退出登录时附带的 refresh token 无效，已忽略");
        }
    }

    private LoginResponse issueTokens(SysUser user, String identityType, List<String> roles) {
        int tokenVersion = user.getTokenVersion() == null ? 0 : user.getTokenVersion();
        String accessToken = jwtTokenProvider.createAccessToken(
                user.getId(), user.getUserType(), identityType, roles, tokenVersion);
        String refreshToken = jwtTokenProvider.createRefreshToken(
                user.getId(), user.getUserType(), identityType, tokenVersion);
        AuthUserInfo userInfo = new AuthUserInfo(user.getId(), user.getUserType(), user.getNickname(), roles);
        return new LoginResponse(accessToken, refreshToken, TOKEN_TYPE_BEARER,
                jwtTokenProvider.getAccessExpireSeconds(), jwtTokenProvider.getRefreshExpireSeconds(), userInfo);
    }

    private void blacklist(String jti, Instant expiresAt) {
        tokenBlacklistService.blacklist(jti, remainingSeconds(expiresAt));
    }

    private long remainingSeconds(Instant expiresAt) {
        if (expiresAt == null) {
            return 0L;
        }
        return expiresAt.getEpochSecond() - Instant.now().getEpochSecond();
    }
}
