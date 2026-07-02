package com.example.template.system.service;

import com.example.template.system.dto.AppPublicConfig;
import com.example.template.system.dto.ConfigBatchUpdateRequest;
import com.example.template.system.dto.ConfigCreateRequest;
import com.example.template.system.dto.ConfigItem;
import com.example.template.system.dto.ConfigMetaUpdateRequest;

import java.util.List;

/**
 * 系统配置服务。
 */
public interface SystemConfigService {

    /**
     * 查询配置列表，可按分组过滤。
     */
    List<ConfigItem> listConfigs(String group);

    /**
     * 批量更新可编辑配置项的值。
     */
    void batchUpdate(ConfigBatchUpdateRequest request);

    /**
     * 新建配置项（默认可编辑）。
     *
     * @return 新配置项 ID
     */
    Long create(ConfigCreateRequest request);

    /**
     * 更新配置元数据（名称、分组、说明、排序；不含键与值）。
     */
    void updateMetadata(Long id, ConfigMetaUpdateRequest request);

    /**
     * 软删除自定义配置项（内置项不可删）。
     */
    void delete(Long id);

    /**
     * 面向前端的公开配置快照。
     */
    AppPublicConfig getAppPublicConfig();

    /**
     * 刷新内存缓存（配置更新后调用）。
     */
    void refreshCache();

    /**
     * 按配置键读取字符串，无值或空串时返回默认值。
     */
    String getString(String configKey, String defaultValue);

    /**
     * 按配置键读取布尔值。
     */
    boolean getBoolean(String configKey, boolean defaultValue);

    /**
     * 按配置键读取整数，非法时返回默认值。
     */
    int getInt(String configKey, int defaultValue);

    boolean isMaintenanceMode();

    boolean isCaptchaEnabled();

    int getPasswordMinLength();

    int getSessionIdleMinutes();

    int getAuditRetentionDays();

    /**
     * 按系统配置校验密码长度，不通过则抛出业务异常。
     */
    void validatePasswordLength(String password);
}
