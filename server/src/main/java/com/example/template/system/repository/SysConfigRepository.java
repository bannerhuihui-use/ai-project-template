package com.example.template.system.repository;

import com.example.template.system.dto.ConfigItem;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.sql.PreparedStatement;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 系统配置数据访问。
 */
@Repository
public class SysConfigRepository {

    private static final RowMapper<ConfigItem> ROW_MAPPER = (rs, rowNum) -> {
        ConfigItem item = new ConfigItem();
        item.setId(rs.getLong("id"));
        item.setConfigKey(rs.getString("config_key"));
        item.setConfigName(rs.getString("config_name"));
        item.setConfigValue(rs.getString("config_value"));
        item.setConfigGroup(rs.getString("config_group"));
        item.setValueType(rs.getString("value_type"));
        item.setDescription(rs.getString("description"));
        item.setEditable(rs.getInt("editable"));
        item.setSortNo(rs.getInt("sort_no"));
        return item;
    };

    private final JdbcTemplate jdbcTemplate;

    public SysConfigRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<ConfigItem> findAll(String group) {
        StringBuilder sql = new StringBuilder(
                "SELECT id, config_key, config_name, config_value, config_group, value_type, "
                        + "description, editable, sort_no FROM sys_config WHERE deleted = 0");
        List<Object> args = new ArrayList<>();
        if (StringUtils.hasText(group)) {
            sql.append(" AND config_group = ?");
            args.add(group.trim());
        }
        sql.append(" ORDER BY config_group, sort_no, id");
        return jdbcTemplate.query(sql.toString(), ROW_MAPPER, args.toArray());
    }

    public Optional<ConfigItem> findByKey(String configKey) {
        List<ConfigItem> list = jdbcTemplate.query(
                "SELECT id, config_key, config_name, config_value, config_group, value_type, "
                        + "description, editable, sort_no FROM sys_config "
                        + "WHERE config_key = ? AND deleted = 0 LIMIT 1",
                ROW_MAPPER, configKey);
        return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
    }

    public Optional<ConfigItem> findById(Long id) {
        List<ConfigItem> list = jdbcTemplate.query(
                "SELECT id, config_key, config_name, config_value, config_group, value_type, "
                        + "description, editable, sort_no FROM sys_config "
                        + "WHERE id = ? AND deleted = 0 LIMIT 1",
                ROW_MAPPER, id);
        return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
    }

    public int updateValue(String configKey, String configValue) {
        return jdbcTemplate.update(
                "UPDATE sys_config SET config_value = ?, updated_at = now() "
                        + "WHERE config_key = ? AND deleted = 0 AND editable = 1",
                configValue, configKey);
    }

    /**
     * 新建配置项。
     *
     * @return 新记录 ID
     */
    public Long insert(String configKey, String configName, String configValue, String configGroup,
                       String valueType, String description, int editable, int sortNo) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(
                    "INSERT INTO sys_config (config_key, config_name, config_value, config_group, "
                            + "value_type, description, editable, sort_no) "
                            + "VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                    new String[]{"id"});
            ps.setString(1, configKey);
            ps.setString(2, configName);
            ps.setString(3, configValue);
            ps.setString(4, configGroup);
            ps.setString(5, valueType);
            ps.setString(6, description);
            ps.setInt(7, editable);
            ps.setInt(8, sortNo);
            return ps;
        }, keyHolder);
        Number key = keyHolder.getKey();
        if (key == null) {
            throw new IllegalStateException("insert sys_config failed: no generated key");
        }
        return key.longValue();
    }

    public int updateMetadata(Long id, String configName, String configGroup, String description, int sortNo) {
        return jdbcTemplate.update(
                "UPDATE sys_config SET config_name = ?, config_group = ?, description = ?, "
                        + "sort_no = ?, updated_at = now() WHERE id = ? AND deleted = 0",
                configName, configGroup, description, sortNo, id);
    }

    public int softDeleteById(Long id) {
        return jdbcTemplate.update(
                "UPDATE sys_config SET deleted = 1, updated_at = now() WHERE id = ? AND deleted = 0",
                id);
    }

    /**
     * 读取全部配置键值，供内存缓存使用。
     */
    public Map<String, String> findAllValues() {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT config_key, config_value FROM sys_config WHERE deleted = 0");
        Map<String, String> result = new HashMap<>(rows.size());
        for (Map<String, Object> row : rows) {
            Object key = row.get("config_key");
            if (key == null) {
                continue;
            }
            Object value = row.get("config_value");
            result.put(key.toString(), value == null ? "" : value.toString());
        }
        return result;
    }
}
