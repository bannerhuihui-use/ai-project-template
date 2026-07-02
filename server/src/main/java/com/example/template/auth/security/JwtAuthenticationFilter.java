package com.example.template.auth.security;

import com.example.template.auth.config.AuthProperties;
import com.example.template.auth.model.AccountStatus;
import com.example.template.auth.model.SysUser;
import com.example.template.auth.model.TokenType;
import com.example.template.auth.repository.SysUserRepository;
import com.example.template.common.ResultCode;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * JWT 鉴权过滤器。
 *
 * <p>流程：白名单放行 → 读取 Bearer Token → 校验解析 → <b>仅接受 ACCESS 令牌</b>
 * → 黑名单校验（启用时；Redis 异常 fail-closed 返回 503）→ <b>每请求查库校验用户状态 + token 版本</b>
 * → 写入 SecurityContext。无 Token 则匿名放行，由授权规则决定是否 401。</p>
 *
 * <p>不打印 token / 密码等敏感信息。</p>
 */
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);
    private static final String BEARER_PREFIX = "Bearer ";
    private static final String ROLE_PREFIX = "ROLE_";
    private static final String SUPER_ADMIN_ROLE = "SUPER_ADMIN";

    private final AntPathMatcher pathMatcher = new AntPathMatcher();
    private final JwtTokenProvider jwtTokenProvider;
    private final SysUserRepository sysUserRepository;
    private final UserPermissionCacheService userPermissionCacheService;
    private final TokenBlacklistService tokenBlacklistService;
    private final SecurityResponseWriter responseWriter;
    private final List<String> whitelist;

    public JwtAuthenticationFilter(JwtTokenProvider jwtTokenProvider,
                                   SysUserRepository sysUserRepository,
                                   UserPermissionCacheService userPermissionCacheService,
                                   TokenBlacklistService tokenBlacklistService,
                                   SecurityResponseWriter responseWriter,
                                   AuthProperties authProperties) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.sysUserRepository = sysUserRepository;
        this.userPermissionCacheService = userPermissionCacheService;
        this.tokenBlacklistService = tokenBlacklistService;
        this.responseWriter = responseWriter;
        this.whitelist = authProperties.getWhitelist().getPaths();
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String uri = request.getRequestURI();
        return whitelist.stream().anyMatch(pattern -> pathMatcher.match(pattern, uri));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (!StringUtils.hasText(header) || !header.startsWith(BEARER_PREFIX)) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = header.substring(BEARER_PREFIX.length()).trim();
        ParsedToken parsed;
        try {
            parsed = jwtTokenProvider.parse(token);
        } catch (JwtException | IllegalArgumentException e) {
            log.warn("JWT 校验失败：{}", e.getMessage());
            responseWriter.writeError(response, HttpServletResponse.SC_UNAUTHORIZED, ResultCode.AUTH_TOKEN_INVALID);
            return;
        }

        // 仅接受 ACCESS 令牌；refresh 令牌只能用于刷新接口
        if (parsed.tokenType() != TokenType.ACCESS) {
            responseWriter.writeError(response, HttpServletResponse.SC_UNAUTHORIZED, ResultCode.AUTH_TOKEN_TYPE_INVALID);
            return;
        }

        // 黑名单校验：Redis 异常时 fail-closed（拒绝访问，返回 503）
        try {
            if (tokenBlacklistService.isBlacklisted(parsed.jti())) {
                responseWriter.writeError(response, HttpServletResponse.SC_UNAUTHORIZED, ResultCode.AUTH_TOKEN_BLACKLISTED);
                return;
            }
        } catch (RedisUnavailableException e) {
            responseWriter.writeError(response, HttpServletResponse.SC_SERVICE_UNAVAILABLE,
                    ResultCode.AUTH_SERVICE_UNAVAILABLE);
            return;
        }

        ResultCode statusError = checkUser(parsed);
        if (statusError != null) {
            responseWriter.writeError(response, HttpServletResponse.SC_UNAUTHORIZED, statusError);
            return;
        }

        authenticate(request, parsed.toCurrentUser());
        filterChain.doFilter(request, response);
    }

    /**
     * 每请求查库校验用户状态与 token 版本，保证禁用/改密/强制下线后旧令牌即时失效。
     *
     * @param parsed 解析后的令牌
     * @return 错误码；通过则返回 null
     */
    private ResultCode checkUser(ParsedToken parsed) {
        Optional<SysUser> userOpt = sysUserRepository.findActiveById(parsed.userId());
        if (userOpt.isEmpty()) {
            return ResultCode.AUTH_TOKEN_INVALID;
        }
        SysUser user = userOpt.get();
        String status = user.getStatus();
        if (AccountStatus.DISABLED.name().equals(status)) {
            return ResultCode.AUTH_ACCOUNT_DISABLED;
        }
        if (!AccountStatus.NORMAL.name().equals(status)) {
            return ResultCode.AUTH_TOKEN_INVALID;
        }
        // token 版本比对：不一致说明令牌已被 token_version 自增（改密/禁用/强制下线）作废
        if (!Objects.equals(user.getTokenVersion(), parsed.tokenVersion())) {
            return ResultCode.AUTH_TOKEN_VERSION_EXPIRED;
        }
        return null;
    }

    private void authenticate(HttpServletRequest request, CurrentUser currentUser) {
        List<SimpleGrantedAuthority> authorities =
                new ArrayList<>(buildAuthorities(currentUser.getUserType(), currentUser.getRoles()));
        // 权限码 authorities 来自 DB（决策 C2，不放进 token）。
        // SUPER_ADMIN 不加载权限码：其拥有 ROLE_SUPER_ADMIN，由 PermissionChecker 短路放行。
        if (!isSuperAdmin(currentUser.getRoles())) {
            for (String code : userPermissionCacheService.getButtonApiCodes(currentUser.getUserId())) {
                if (StringUtils.hasText(code)) {
                    authorities.add(new SimpleGrantedAuthority(code));
                }
            }
        }
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(currentUser, null, authorities);
        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    private boolean isSuperAdmin(List<String> roles) {
        return roles != null && roles.stream().anyMatch(SUPER_ADMIN_ROLE::equals);
    }

    /**
     * 组装权限：同时包含 ROLE_{userType} 与每个 ROLE_{roleCode}。
     *
     * <p>用 {@link LinkedHashSet} 去重（userType 与某个 roleCode 可能同名，如均为 ADMIN），
     * 并跳过空白项，保证既覆盖 v1 的 user_type 鉴权，又让 token 中的 roles 真正参与授权。</p>
     *
     * @param userType 用户类型，可空
     * @param roles    角色编码列表，可空
     * @return 去重后的权限列表
     */
    static List<SimpleGrantedAuthority> buildAuthorities(String userType, List<String> roles) {
        Set<String> roleNames = new LinkedHashSet<>();
        if (StringUtils.hasText(userType)) {
            roleNames.add(ROLE_PREFIX + userType.trim());
        }
        if (roles != null) {
            for (String role : roles) {
                if (StringUtils.hasText(role)) {
                    roleNames.add(ROLE_PREFIX + role.trim());
                }
            }
        }
        return roleNames.stream().map(SimpleGrantedAuthority::new).toList();
    }
}
