package com.example.template.auth.repository;

import com.example.template.auth.model.LoginDevice;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 登录设备数据访问。
 *
 * <p>upsert 口径：有 deviceId 时更新同 (user_id, identity_type, device_id) 的现有记录，
 * 不存在则插入；无 deviceId 时直接插入一条登录记录（不做去重）。</p>
 */
@Repository
public class LoginDeviceRepository {

    private static final int DEFAULT_LIST_LIMIT = 50;

    private static final RowMapper<LoginDeviceRow> ROW_MAPPER = (rs, rowNum) -> {
        LoginDeviceRow row = new LoginDeviceRow();
        row.setId(rs.getLong("id"));
        row.setIdentityType(rs.getString("identity_type"));
        row.setDeviceId(rs.getString("device_id"));
        row.setDeviceName(rs.getString("device_name"));
        row.setPlatform(rs.getString("platform"));
        row.setIp(rs.getString("ip"));
        if (rs.getTimestamp("last_login_time") != null) {
            row.setLastLoginTime(rs.getTimestamp("last_login_time").toLocalDateTime());
        }
        return row;
    };

    private final JdbcTemplate jdbcTemplate;

    public LoginDeviceRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * 记录一次登录设备信息。
     *
     * @param device 设备信息
     */
    public void recordLogin(LoginDevice device) {
        if (StringUtils.hasText(device.getDeviceId())) {
            int updated = jdbcTemplate.update(
                    "UPDATE auth_login_device SET device_name = ?, platform = ?, ip = ?, user_agent = ?, "
                            + "last_login_time = now(), updated_at = now() "
                            + "WHERE user_id = ? AND identity_type = ? AND device_id = ? AND deleted = 0",
                    device.getDeviceName(), device.getPlatform(), device.getIp(), device.getUserAgent(),
                    device.getUserId(), device.getIdentityType(), device.getDeviceId());
            if (updated > 0) {
                return;
            }
        }
        jdbcTemplate.update(
                "INSERT INTO auth_login_device "
                        + "(user_id, identity_type, device_id, device_name, platform, ip, user_agent, last_login_time) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?, now())",
                device.getUserId(), device.getIdentityType(), device.getDeviceId(),
                device.getDeviceName(), device.getPlatform(), device.getIp(), device.getUserAgent());
    }

    /**
     * 查询用户最近登录设备（按最近登录时间倒序）。
     *
     * @param userId 用户 ID
     * @param limit  最大条数，超过 100 时截断为 100
     */
    public List<LoginDeviceRow> findRecentByUserId(Long userId, int limit) {
        int safeLimit = limit < 1 ? DEFAULT_LIST_LIMIT : Math.min(limit, 100);
        String sql = "SELECT id, identity_type, device_id, device_name, platform, ip, last_login_time "
                + "FROM auth_login_device WHERE user_id = ? AND deleted = 0 "
                + "ORDER BY last_login_time DESC NULLS LAST, id DESC LIMIT ?";
        return jdbcTemplate.query(sql, ROW_MAPPER, userId, safeLimit);
    }

    /**
     * 登录设备查询行（Repository 内部使用）。
     */
    public static class LoginDeviceRow {

        private Long id;
        private String identityType;
        private String deviceId;
        private String deviceName;
        private String platform;
        private String ip;
        private java.time.LocalDateTime lastLoginTime;

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getIdentityType() {
            return identityType;
        }

        public void setIdentityType(String identityType) {
            this.identityType = identityType;
        }

        public String getDeviceId() {
            return deviceId;
        }

        public void setDeviceId(String deviceId) {
            this.deviceId = deviceId;
        }

        public String getDeviceName() {
            return deviceName;
        }

        public void setDeviceName(String deviceName) {
            this.deviceName = deviceName;
        }

        public String getPlatform() {
            return platform;
        }

        public void setPlatform(String platform) {
            this.platform = platform;
        }

        public String getIp() {
            return ip;
        }

        public void setIp(String ip) {
            this.ip = ip;
        }

        public java.time.LocalDateTime getLastLoginTime() {
            return lastLoginTime;
        }

        public void setLastLoginTime(java.time.LocalDateTime lastLoginTime) {
            this.lastLoginTime = lastLoginTime;
        }
    }
}
