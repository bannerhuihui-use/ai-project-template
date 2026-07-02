package com.example.template.auth.security;

import com.example.template.auth.config.AuthProperties;
import com.example.template.common.ResultCode;
import com.example.template.system.dto.AppPublicConfig;
import com.example.template.system.dto.ConfigBatchUpdateRequest;
import com.example.template.system.dto.ConfigCreateRequest;
import com.example.template.system.dto.ConfigItem;
import com.example.template.system.dto.ConfigMetaUpdateRequest;
import com.example.template.system.service.SystemConfigService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * MaintenanceModeFilter 单元测试。
 */
class MaintenanceModeFilterTest {

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private SystemConfigService configService(boolean maintenanceMode) {
        return new SystemConfigService() {
            @Override
            public boolean isMaintenanceMode() {
                return maintenanceMode;
            }

            @Override
            public List<ConfigItem> listConfigs(String group) {
                return List.of();
            }

            @Override
            public void batchUpdate(ConfigBatchUpdateRequest request) {
            }

            @Override
            public Long create(ConfigCreateRequest request) {
                return 1L;
            }

            @Override
            public void updateMetadata(Long id, ConfigMetaUpdateRequest request) {
            }

            @Override
            public void delete(Long id) {
            }

            @Override
            public AppPublicConfig getAppPublicConfig() {
                return new AppPublicConfig();
            }

            @Override
            public void refreshCache() {
            }

            @Override
            public String getString(String configKey, String defaultValue) {
                return defaultValue;
            }

            @Override
            public boolean getBoolean(String configKey, boolean defaultValue) {
                return defaultValue;
            }

            @Override
            public int getInt(String configKey, int defaultValue) {
                return defaultValue;
            }

            @Override
            public boolean isCaptchaEnabled() {
                return false;
            }

            @Override
            public int getPasswordMinLength() {
                return 8;
            }

            @Override
            public int getSessionIdleMinutes() {
                return 120;
            }

            @Override
            public int getAuditRetentionDays() {
                return 30;
            }

            @Override
            public void validatePasswordLength(String password) {
            }
        };
    }

    private MaintenanceModeFilter newFilter(boolean maintenanceMode) {
        AuthProperties properties = new AuthProperties();
        properties.getWhitelist().setPaths(List.of("/api/v1/public/**"));
        return new MaintenanceModeFilter(configService(maintenanceMode),
                new SecurityResponseWriter(new ObjectMapper()), properties);
    }

    private void setCurrentUser(List<String> roles) {
        CurrentUser user = new CurrentUser(1L, "ADMIN", "USERNAME", roles);
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(user, null, List.of());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @Test
    void shouldPassWhenMaintenanceOff() throws Exception {
        MaintenanceModeFilter filter = newFilter(false);
        setCurrentUser(List.of("ADMIN"));

        MockHttpServletResponse response = doFilter(filter, "/api/v1/auth/me");

        assertEquals(HttpServletResponse.SC_OK, response.getStatus());
    }

    @Test
    void shouldBlockNonSuperAdminWhenMaintenanceOn() throws Exception {
        MaintenanceModeFilter filter = newFilter(true);
        setCurrentUser(List.of("ADMIN"));

        MockHttpServletResponse response = doFilter(filter, "/api/v1/auth/me");

        assertEquals(HttpServletResponse.SC_SERVICE_UNAVAILABLE, response.getStatus());
        assertTrue(response.getContentAsString().contains(String.valueOf(ResultCode.SYSTEM_MAINTENANCE.getCode())));
    }

    @Test
    void shouldAllowSuperAdminWhenMaintenanceOn() throws Exception {
        MaintenanceModeFilter filter = newFilter(true);
        setCurrentUser(List.of("SUPER_ADMIN"));

        MockHttpServletResponse response = doFilter(filter, "/api/v1/auth/me");

        assertEquals(HttpServletResponse.SC_OK, response.getStatus());
    }

    @Test
    void shouldSkipWhitelistWhenMaintenanceOn() throws Exception {
        MaintenanceModeFilter filter = newFilter(true);
        setCurrentUser(List.of("ADMIN"));

        MockHttpServletResponse response = doFilter(filter, "/api/v1/public/app-config");

        assertEquals(HttpServletResponse.SC_OK, response.getStatus());
    }

    private MockHttpServletResponse doFilter(MaintenanceModeFilter filter, String uri) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", uri);
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request, response, new MockFilterChain());
        return response;
    }
}
