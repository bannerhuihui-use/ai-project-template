package com.example.template.auth.security;

import com.example.template.auth.config.AuthProperties;
import com.example.template.common.ResultCode;
import com.example.template.system.service.SystemConfigService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * 维护模式全链路拦截：开启后仅 {@code SUPER_ADMIN} 可访问受保护接口。
 *
 * <p>白名单（登录、公开配置、健康检查等）仍放行；登录入口在 {@code AuthServiceImpl} 已单独拦截。</p>
 */
@Component
public class MaintenanceModeFilter extends OncePerRequestFilter {

    private static final String SUPER_ADMIN_ROLE = "SUPER_ADMIN";

    private final AntPathMatcher pathMatcher = new AntPathMatcher();
    private final SystemConfigService systemConfigService;
    private final SecurityResponseWriter responseWriter;
    private final List<String> whitelist;

    public MaintenanceModeFilter(SystemConfigService systemConfigService,
                                 SecurityResponseWriter responseWriter,
                                 AuthProperties authProperties) {
        this.systemConfigService = systemConfigService;
        this.responseWriter = responseWriter;
        this.whitelist = authProperties.getWhitelist().getPaths();
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        if (!systemConfigService.isMaintenanceMode()) {
            return true;
        }
        String uri = request.getRequestURI();
        return whitelist.stream().anyMatch(pattern -> pathMatcher.match(pattern, uri));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        CurrentUser user = SecurityUtils.getCurrentUser();
        if (user == null || isSuperAdmin(user)) {
            filterChain.doFilter(request, response);
            return;
        }
        responseWriter.writeError(response, HttpServletResponse.SC_SERVICE_UNAVAILABLE, ResultCode.SYSTEM_MAINTENANCE);
    }

    private boolean isSuperAdmin(CurrentUser user) {
        List<String> roles = user.getRoles();
        return roles != null && roles.stream().anyMatch(SUPER_ADMIN_ROLE::equals);
    }
}
