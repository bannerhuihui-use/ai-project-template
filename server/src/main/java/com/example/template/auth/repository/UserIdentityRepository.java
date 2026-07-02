package com.example.template.auth.repository;

import com.example.template.auth.model.UserIdentity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.util.List;
import java.util.Optional;

/**
 * 登录身份数据访问。仅负责 SQL 执行，不含业务逻辑。
 */
@Repository
public class UserIdentityRepository {

    private static final RowMapper<UserIdentity> ROW_MAPPER = (rs, rowNum) -> {
        UserIdentity identity = new UserIdentity();
        identity.setId(rs.getLong("id"));
        identity.setUserId(rs.getLong("user_id"));
        identity.setIdentityType(rs.getString("identity_type"));
        identity.setIdentifier(rs.getString("identifier"));
        identity.setCredential(rs.getString("credential"));
        identity.setAppId(rs.getString("app_id"));
        identity.setUnionId(rs.getString("union_id"));
        identity.setStatus(rs.getString("status"));
        identity.setDeleted(rs.getInt("deleted"));
        return identity;
    };

    private final JdbcTemplate jdbcTemplate;

    public UserIdentityRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * 按类型与标识查询未删除的登录身份。
     *
     * @param identityType 登录方式
     * @param identifier   登录标识
     * @return 登录身份
     */
    public Optional<UserIdentity> findActiveByTypeAndIdentifier(String identityType, String identifier) {
        String sql = "SELECT id, user_id, identity_type, identifier, credential, app_id, union_id, status, deleted "
                + "FROM user_identity WHERE identity_type = ? AND identifier = ? AND deleted = 0 LIMIT 1";
        List<UserIdentity> list = jdbcTemplate.query(sql, ROW_MAPPER, identityType, identifier);
        return list.stream().findFirst();
    }

    /**
     * 按用户 ID 与登录方式查询未删除的登录身份。
     *
     * @param userId       用户 ID
     * @param identityType 登录方式
     * @return 登录身份
     */
    public Optional<UserIdentity> findActiveByUserIdAndType(Long userId, String identityType) {
        String sql = "SELECT id, user_id, identity_type, identifier, credential, app_id, union_id, status, deleted "
                + "FROM user_identity WHERE user_id = ? AND identity_type = ? AND deleted = 0 LIMIT 1";
        List<UserIdentity> list = jdbcTemplate.query(sql, ROW_MAPPER, userId, identityType);
        return list.stream().findFirst();
    }

    /**
     * 更新指定用户某登录方式的登录凭证（BCrypt 密文）。
     *
     * @param userId       用户 ID
     * @param identityType 登录方式
     * @param credential   新凭证（BCrypt 密文）
     * @return 受影响行数
     */
    public int updateCredentialByUserIdAndType(Long userId, String identityType, String credential) {
        return jdbcTemplate.update(
                "UPDATE user_identity SET credential = ?, updated_at = now() "
                        + "WHERE user_id = ? AND identity_type = ? AND deleted = 0",
                credential, userId, identityType);
    }

    /**
     * 更新最后登录时间。
     *
     * @param id 身份 ID
     */
    public void updateLastLoginTime(Long id) {
        jdbcTemplate.update(
                "UPDATE user_identity SET last_login_time = now(), updated_at = now() WHERE id = ?", id);
    }

    /**
     * 判断指定类型的登录标识是否已存在（未删除）。
     */
    public boolean existsActiveByTypeAndIdentifier(String identityType, String identifier) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM user_identity WHERE identity_type = ? AND identifier = ? AND deleted = 0",
                Integer.class, identityType, identifier);
        return count != null && count > 0;
    }

    /**
     * 新建登录身份。
     *
     * @param userId       用户 ID
     * @param identityType 登录方式
     * @param identifier   登录标识
     * @param credential   BCrypt 密文（可空）
     * @return 新身份 ID
     */
    public Long insert(Long userId, String identityType, String identifier, String credential) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(
                    "INSERT INTO user_identity (user_id, identity_type, identifier, credential, status) "
                            + "VALUES (?, ?, ?, ?, 'NORMAL')",
                    new String[]{"id"});
            ps.setLong(1, userId);
            ps.setString(2, identityType);
            ps.setString(3, identifier);
            ps.setString(4, credential);
            return ps;
        }, keyHolder);
        Number key = keyHolder.getKey();
        if (key == null) {
            throw new IllegalStateException("insert user_identity failed: no generated key");
        }
        return key.longValue();
    }
}
