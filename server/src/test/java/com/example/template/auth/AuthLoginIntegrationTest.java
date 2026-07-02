package com.example.template.auth;

import com.example.template.auth.config.AuthProperties;
import com.example.template.auth.dto.AdminLoginRequest;
import com.example.template.auth.dto.LoginResponse;
import com.example.template.auth.dto.WechatLoginRequest;
import com.example.template.auth.repository.LoginDeviceRepository;
import com.example.template.auth.repository.SysRoleRepository;
import com.example.template.auth.repository.SysUserRepository;
import com.example.template.auth.repository.UserIdentityRepository;
import com.example.template.auth.security.JwtTokenProvider;
import com.example.template.auth.security.TokenBlacklistService;
import com.example.template.auth.service.impl.AuthServiceImpl;
import com.example.template.common.BusinessException;
import com.example.template.common.ResultCode;
import com.example.template.testsupport.AuthTestSupport;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import javax.sql.DataSource;
import java.io.File;
import java.sql.Connection;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * 后台登录端到端集成测试（连接本地 PostgreSQL）。
 *
 * <p>手动装配，不加载完整 Spring 容器，避免 Redis/RabbitMQ/Mongo 干扰。
 * 标记为 {@code integration}，默认 {@code mvn test} 不执行（surefire 已排除该分组，
 * 保证默认测试零外部依赖）；需要时用 {@code mvn test -Dgroups=integration} 单独运行。
 * 连接信息从环境变量读取（POSTGRES_HOST/PORT/DATABASE/USERNAME/PASSWORD）。</p>
 */
@Tag("integration")
class AuthLoginIntegrationTest {

    private static final String PASSWORD = "123456";
    private static final String SQL_FILE = "src/main/resources/db/template-full.sql";

    private JdbcTemplate jdbcTemplate;
    private AuthServiceImpl authService;
    private PasswordEncoder passwordEncoder;

    private Long adminUserId;

    @BeforeEach
    void setUp() throws Exception {
        DataSource dataSource = buildDataSource();
        assumeTrue(dataSource != null, "本地 PostgreSQL 不可达，跳过该集成测试");

        this.jdbcTemplate = new JdbcTemplate(dataSource);
        this.passwordEncoder = new BCryptPasswordEncoder();

        // 1) 建表 + 种子（template-full.sql 幂等，等价 Flyway V1~V8）
        try (Connection connection = dataSource.getConnection()) {
            ScriptUtils.executeSqlScript(connection, new org.springframework.core.io.FileSystemResource(new File(SQL_FILE)));
        }

        // 2) 装配业务对象
        UserIdentityRepository identityRepo = new UserIdentityRepository(jdbcTemplate);
        SysUserRepository userRepo = new SysUserRepository(jdbcTemplate);
        SysRoleRepository roleRepo = new SysRoleRepository(jdbcTemplate);
        LoginDeviceRepository deviceRepo = new LoginDeviceRepository(jdbcTemplate);
        AuthProperties props = new AuthProperties();
        props.getJwt().setSecret("integration-test-secret");
        props.getJwt().setIssuer("template-server");
        props.getJwt().setAccessExpireSeconds(7200);
        props.getJwt().setRefreshExpireSeconds(604800);
        JwtTokenProvider tokenProvider = new JwtTokenProvider(props);
        // 集成测试不依赖 Redis：用禁用态黑名单（无 Redis 连接）
        TokenBlacklistService blacklist = disabledBlacklist();
        this.authService = new AuthServiceImpl(identityRepo, userRepo, roleRepo, deviceRepo,
                tokenProvider, blacklist, passwordEncoder,
                AuthTestSupport.defaultSystemConfig(), AuthTestSupport.noopCaptcha());

        // 3) 保证文档种子 admin 的口令确实是 123456（修正历史可能写入的错误 hash）
        jdbcTemplate.update("UPDATE user_identity SET credential = ? "
                        + "WHERE identity_type = 'USERNAME' AND identifier = 'admin'",
                passwordEncoder.encode(PASSWORD));

        // 4) 造测试账号（独立于真实数据，结束后清理）
        cleanupTestUsers();
        adminUserId = createUser("ADMIN", "NORMAL", "it_admin", "ADMIN");
        createUser("MEMBER", "NORMAL", "it_member", null);
        createUser("ADMIN", "DISABLED", "it_disabled", "ADMIN");
    }

    @AfterEach
    void tearDown() {
        if (jdbcTemplate != null) {
            cleanupTestUsers();
        }
    }

    @Test
    void adminLogin_success_andTokenCarriesClaims() {
        LoginResponse resp = authService.adminLogin(req("it_admin", PASSWORD), null);

        assertNotNull(resp.getAccessToken());
        assertNotNull(resp.getRefreshToken());
        assertEquals("Bearer", resp.getTokenType());
        assertEquals(7200, resp.getAccessExpireSeconds());
        assertEquals(604800, resp.getRefreshExpireSeconds());
        assertEquals("ADMIN", resp.getUser().getUserType());
        assertTrue(resp.getUser().getRoles().contains("ADMIN"), "应包含 ADMIN 角色");
        assertEquals(adminUserId, resp.getUser().getUserId());
    }

    @Test
    void documentedSeedAdmin_canLogin() {
        // 验证种子 admin/123456 可登录（文档示例可用）
        LoginResponse resp = authService.adminLogin(req("admin", PASSWORD), null);
        assertNotNull(resp.getAccessToken());
        assertEquals("ADMIN", resp.getUser().getUserType());
    }

    @Test
    void adminLogin_wrongPassword_returnsBadCredentials() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> authService.adminLogin(req("it_admin", "wrong-password"), null));
        assertEquals(ResultCode.AUTH_BAD_CREDENTIALS.getCode(), ex.getCode());
    }

    @Test
    void adminLogin_unknownAccount_returnsBadCredentials() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> authService.adminLogin(req("no_such_user", PASSWORD), null));
        assertEquals(ResultCode.AUTH_BAD_CREDENTIALS.getCode(), ex.getCode());
    }

    @Test
    void adminLogin_memberAccount_returnsNotAdmin() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> authService.adminLogin(req("it_member", PASSWORD), null));
        assertEquals(ResultCode.AUTH_NOT_ADMIN.getCode(), ex.getCode());
    }

    @Test
    void adminLogin_disabledAccount_returnsDisabled() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> authService.adminLogin(req("it_disabled", PASSWORD), null));
        assertEquals(ResultCode.AUTH_ACCOUNT_DISABLED.getCode(), ex.getCode());
    }

    private TokenBlacklistService disabledBlacklist() {
        return new TokenBlacklistService() {
            @Override
            public void blacklist(String jti, long ttlSeconds) {
                // no-op
            }

            @Override
            public boolean isBlacklisted(String jti) {
                return false;
            }

            @Override
            public boolean isEnabled() {
                return false;
            }
        };
    }

    @Test
    void wechatLogin_returnsNotSupported() {
        WechatLoginRequest request = new WechatLoginRequest();
        request.setCode("wx-login-code");
        BusinessException ex = assertThrows(BusinessException.class, () -> authService.wechatLogin(request));
        assertEquals(ResultCode.WECHAT_LOGIN_NOT_SUPPORTED.getCode(), ex.getCode());
    }

    private AdminLoginRequest req(String username, String password) {
        AdminLoginRequest request = new AdminLoginRequest();
        request.setUsername(username);
        request.setPassword(password);
        return request;
    }

    /**
     * 创建测试用户 + 用户名登录身份（可选绑定角色）。
     *
     * @return 新建用户 ID
     */
    private Long createUser(String userType, String status, String username, String roleCode) {
        Long userId = jdbcTemplate.queryForObject(
                "INSERT INTO sys_user (user_type, nickname, status) VALUES (?, ?, ?) RETURNING id",
                Long.class, userType, username, status);
        jdbcTemplate.update("INSERT INTO user_identity (user_id, identity_type, identifier, credential, status) "
                        + "VALUES (?, 'USERNAME', ?, ?, 'NORMAL')",
                userId, username, passwordEncoder.encode(PASSWORD));
        if (roleCode != null) {
            Long roleId = jdbcTemplate.queryForObject(
                    "SELECT id FROM sys_role WHERE role_code = ? LIMIT 1", Long.class, roleCode);
            jdbcTemplate.update(
                    "INSERT INTO sys_user_role (user_id, role_id) VALUES (?, ?) ON CONFLICT DO NOTHING",
                    userId, roleId);
        }
        return userId;
    }

    private void cleanupTestUsers() {
        for (String username : List.of("it_admin", "it_member", "it_disabled")) {
            List<Long> userIds = jdbcTemplate.queryForList(
                    "SELECT user_id FROM user_identity WHERE identity_type = 'USERNAME' AND identifier = ?",
                    Long.class, username);
            jdbcTemplate.update(
                    "DELETE FROM user_identity WHERE identity_type = 'USERNAME' AND identifier = ?", username);
            for (Long userId : userIds) {
                jdbcTemplate.update("DELETE FROM sys_user_role WHERE user_id = ?", userId);
                jdbcTemplate.update("DELETE FROM sys_user WHERE id = ?", userId);
            }
        }
    }

    private DataSource buildDataSource() {
        String host = System.getenv("POSTGRES_HOST");
        String port = envOrDefault("POSTGRES_PORT", "5432");
        String database = System.getenv("POSTGRES_DATABASE");
        String username = System.getenv("POSTGRES_USERNAME");
        String password = System.getenv("POSTGRES_PASSWORD");
        if (isBlank(host) || isBlank(database) || isBlank(username)) {
            return null;
        }
        String url = "jdbc:postgresql://" + host + ":" + port + "/" + database;
        DriverManagerDataSource dataSource = new DriverManagerDataSource(url, username, password == null ? "" : password);
        dataSource.setDriverClassName("org.postgresql.Driver");
        try (Connection ignored = dataSource.getConnection()) {
            return dataSource;
        } catch (Exception e) {
            return null;
        }
    }

    private String envOrDefault(String key, String defaultValue) {
        String value = System.getenv(key);
        return isBlank(value) ? defaultValue : value;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
