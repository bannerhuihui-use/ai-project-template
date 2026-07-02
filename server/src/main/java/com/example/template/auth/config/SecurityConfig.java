package com.example.template.auth.config;

import com.example.template.auth.repository.SysUserRepository;
import com.example.template.auth.security.JwtAuthenticationFilter;
import com.example.template.auth.security.JwtTokenProvider;
import com.example.template.auth.security.MaintenanceModeFilter;
import com.example.template.auth.security.RestAccessDeniedHandler;
import com.example.template.auth.security.RestAuthenticationEntryPoint;
import com.example.template.auth.security.SecurityResponseWriter;
import com.example.template.auth.security.TokenBlacklistService;
import com.example.template.auth.security.UserPermissionCacheService;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Spring Security 配置：无状态 JWT 鉴权。
 *
 * <p>白名单放行；管理员示例接口要求 ROLE_ADMIN；其余接口需认证。
 * 未认证 / 无权限通过自定义处理器返回统一 Result。</p>
 */
@Configuration
@EnableMethodSecurity
@EnableConfigurationProperties(AuthProperties.class)
public class SecurityConfig {

    /** 管理员接口统一前缀：要求 ROLE_ADMIN（admin/login 已在白名单且匹配在前，仍放行）。 */
    private static final String ADMIN_PATH_PATTERN = "/api/v1/auth/admin/**";

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public JwtAuthenticationFilter jwtAuthenticationFilter(JwtTokenProvider jwtTokenProvider,
                                                           SysUserRepository sysUserRepository,
                                                           UserPermissionCacheService userPermissionCacheService,
                                                           TokenBlacklistService tokenBlacklistService,
                                                           SecurityResponseWriter responseWriter,
                                                           AuthProperties authProperties) {
        return new JwtAuthenticationFilter(jwtTokenProvider, sysUserRepository, userPermissionCacheService,
                tokenBlacklistService, responseWriter, authProperties);
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   JwtAuthenticationFilter jwtAuthenticationFilter,
                                                   MaintenanceModeFilter maintenanceModeFilter,
                                                   RestAuthenticationEntryPoint authenticationEntryPoint,
                                                   RestAccessDeniedHandler accessDeniedHandler,
                                                   AuthProperties authProperties) throws Exception {
        String[] whitelist = authProperties.getWhitelist().getPaths().toArray(new String[0]);

        http
                .csrf(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // 白名单在前：admin/login 等公共接口优先 permitAll
                        .requestMatchers(whitelist).permitAll()
                        // 其余 admin 接口统一要求 ROLE_ADMIN
                        .requestMatchers(ADMIN_PATH_PATTERN).hasRole("ADMIN")
                        .anyRequest().authenticated())
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler))
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterAfter(maintenanceModeFilter, JwtAuthenticationFilter.class);

        return http.build();
    }
}
