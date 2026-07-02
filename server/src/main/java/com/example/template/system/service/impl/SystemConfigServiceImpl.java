package com.example.template.system.service.impl;

import com.example.template.common.BusinessException;
import com.example.template.common.ResultCode;
import com.example.template.system.SystemConfigKeys;
import com.example.template.system.dto.AppPublicConfig;
import com.example.template.system.dto.ConfigBatchUpdateRequest;
import com.example.template.system.dto.ConfigCreateRequest;
import com.example.template.system.dto.ConfigItem;
import com.example.template.system.dto.ConfigMetaUpdateRequest;
import com.example.template.system.dto.ConfigUpdateItem;
import com.example.template.system.repository.SysConfigRepository;
import com.example.template.system.service.SystemConfigService;
import com.example.template.system.support.BuiltinConfigValidator;
import com.example.template.system.support.ConfigChangeAuditLogger;
import com.example.template.system.support.ConfigValueValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 系统配置服务实现。
 */
@Service
public class SystemConfigServiceImpl implements SystemConfigService {

    private static final Logger log = LoggerFactory.getLogger(SystemConfigServiceImpl.class);

    private static final int DEFAULT_PASSWORD_MIN_LENGTH = 8;
    private static final int DEFAULT_SESSION_IDLE_MINUTES = 120;
    private static final int DEFAULT_AUDIT_RETENTION_DAYS = 30;
    private static final String DEFAULT_SITE_NAME = "管理后台";
    private static final String DEFAULT_SITE_COPYRIGHT = "© 2026 Template";

    private final SysConfigRepository sysConfigRepository;

    private final ConcurrentHashMap<String, String> cache = new ConcurrentHashMap<>();

    public SystemConfigServiceImpl(SysConfigRepository sysConfigRepository) {
        this.sysConfigRepository = sysConfigRepository;
    }

    @Override
    public List<ConfigItem> listConfigs(String group) {
        List<ConfigItem> list = sysConfigRepository.findAll(group);
        list.forEach(this::enrichFlags);
        return list;
    }

    @Override
    @Transactional
    public void batchUpdate(ConfigBatchUpdateRequest request) {
        for (ConfigUpdateItem item : request.getItems()) {
            String configKey = item.getConfigKey().trim();
            ConfigItem existing = sysConfigRepository.findByKey(configKey)
                    .orElseThrow(() -> new BusinessException(ResultCode.CONFIG_NOT_FOUND.getCode(),
                            "配置项不存在: " + configKey));
            if (existing.getEditable() == null || existing.getEditable() != 1) {
                throw new BusinessException(ResultCode.CONFIG_KEY_NOT_EDITABLE.getCode(),
                        "配置项不可编辑: " + configKey);
            }
            String value = item.getConfigValue() == null ? "" : item.getConfigValue();
            String oldValue = existing.getConfigValue() == null ? "" : existing.getConfigValue();
            if (oldValue.equals(value)) {
                continue;
            }
            value = validateAndNormalize(existing.getValueType(), value);
            BuiltinConfigValidator.validate(configKey, value);
            int updated = sysConfigRepository.updateValue(configKey, value);
            if (updated == 0) {
                throw new BusinessException(ResultCode.CONFIG_NOT_FOUND.getCode(),
                        "配置项更新失败: " + configKey);
            }
            ConfigChangeAuditLogger.auditUpdateValue(configKey, oldValue, value);
            log.info("系统配置已更新: configKey={}", configKey);
        }
        refreshCache();
    }

    @Override
    @Transactional
    public Long create(ConfigCreateRequest request) {
        String configKey = request.getConfigKey().trim();
        if (sysConfigRepository.findByKey(configKey).isPresent()) {
            throw new BusinessException(ResultCode.CONFIG_KEY_EXISTS);
        }

        String valueType = normalizeValueType(request.getValueType());
        String value = request.getConfigValue() == null ? "" : request.getConfigValue();
        value = validateAndNormalize(valueType, value);
        BuiltinConfigValidator.validate(configKey, value);

        String configGroup = request.getConfigGroup().trim();
        int sortNo = request.getSortNo() == null ? 0 : request.getSortNo();
        String description = StringUtils.hasText(request.getDescription())
                ? request.getDescription().trim() : null;

        Long id = sysConfigRepository.insert(
                configKey,
                request.getConfigName().trim(),
                value,
                configGroup,
                valueType,
                description,
                1,
                sortNo);
        refreshCache();
        ConfigChangeAuditLogger.auditCreate(configKey, valueType, configGroup);
        log.info("新建系统配置: configKey={}, id={}", configKey, id);
        return id;
    }

    @Override
    @Transactional
    public void updateMetadata(Long id, ConfigMetaUpdateRequest request) {
        ConfigItem existing = sysConfigRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ResultCode.CONFIG_NOT_FOUND));
        if (existing.getEditable() == null || existing.getEditable() != 1) {
            throw new BusinessException(ResultCode.CONFIG_KEY_NOT_EDITABLE.getCode(),
                    "配置项不可编辑: " + existing.getConfigKey());
        }
        int sortNo = request.getSortNo() == null ? 0 : request.getSortNo();
        String description = StringUtils.hasText(request.getDescription())
                ? request.getDescription().trim() : null;
        int updated = sysConfigRepository.updateMetadata(
                id,
                request.getConfigName().trim(),
                request.getConfigGroup().trim(),
                description,
                sortNo);
        if (updated == 0) {
            throw new BusinessException(ResultCode.CONFIG_NOT_FOUND);
        }
        String summary = buildMetaChangeSummary(existing, request, sortNo, description);
        ConfigChangeAuditLogger.auditUpdateMeta(existing.getConfigKey(), summary);
        log.info("更新系统配置元数据: id={}, configKey={}", id, existing.getConfigKey());
    }

    @Override
    @Transactional
    public void delete(Long id) {
        ConfigItem existing = sysConfigRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ResultCode.CONFIG_NOT_FOUND));
        if (SystemConfigKeys.isBuiltin(existing.getConfigKey())) {
            throw new BusinessException(ResultCode.CONFIG_NOT_DELETABLE);
        }
        int deleted = sysConfigRepository.softDeleteById(id);
        if (deleted == 0) {
            throw new BusinessException(ResultCode.CONFIG_NOT_FOUND);
        }
        refreshCache();
        ConfigChangeAuditLogger.auditDelete(existing.getConfigKey());
        log.info("删除系统配置: id={}, configKey={}", id, existing.getConfigKey());
    }

    @Override
    public AppPublicConfig getAppPublicConfig() {
        AppPublicConfig config = new AppPublicConfig();
        config.setSiteName(getString(SystemConfigKeys.SITE_NAME, DEFAULT_SITE_NAME));
        config.setSiteLogo(getString(SystemConfigKeys.SITE_LOGO, ""));
        config.setSiteCopyright(getString(SystemConfigKeys.SITE_COPYRIGHT, DEFAULT_SITE_COPYRIGHT));
        config.setMaintenanceMode(isMaintenanceMode());
        config.setCaptchaEnabled(isCaptchaEnabled());
        config.setPasswordMinLength(getPasswordMinLength());
        config.setSessionIdleMinutes(getSessionIdleMinutes());
        config.setAuditRetentionDays(getAuditRetentionDays());
        return config;
    }

    @Override
    public void refreshCache() {
        cache.clear();
        cache.putAll(sysConfigRepository.findAllValues());
        log.debug("系统配置缓存已刷新: size={}", cache.size());
    }

    @Override
    public String getString(String configKey, String defaultValue) {
        if (!StringUtils.hasText(configKey)) {
            return defaultValue;
        }
        String value = resolve(configKey.trim());
        return StringUtils.hasText(value) ? value : defaultValue;
    }

    @Override
    public int getInt(String configKey, int defaultValue) {
        if (!StringUtils.hasText(configKey)) {
            return defaultValue;
        }
        String value = resolve(configKey.trim());
        if (!StringUtils.hasText(value)) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            log.warn("配置值非合法整数，使用默认值: key={}", configKey);
            return defaultValue;
        }
    }

    @Override
    public boolean getBoolean(String configKey, boolean defaultValue) {
        if (!StringUtils.hasText(configKey)) {
            return defaultValue;
        }
        String value = resolve(configKey.trim());
        if (!StringUtils.hasText(value)) {
            return defaultValue;
        }
        return "true".equalsIgnoreCase(value.trim());
    }

    @Override
    public boolean isMaintenanceMode() {
        return getBoolean(SystemConfigKeys.MAINTENANCE_MODE, false);
    }

    @Override
    public boolean isCaptchaEnabled() {
        return getBoolean(SystemConfigKeys.CAPTCHA_ENABLED, false);
    }

    @Override
    public int getPasswordMinLength() {
        int min = getInt(SystemConfigKeys.PASSWORD_MIN_LENGTH, DEFAULT_PASSWORD_MIN_LENGTH);
        return BuiltinConfigValidator.clampPasswordMinLength(min);
    }

    @Override
    public int getSessionIdleMinutes() {
        int minutes = getInt(SystemConfigKeys.SESSION_IDLE_MINUTES, DEFAULT_SESSION_IDLE_MINUTES);
        return BuiltinConfigValidator.clampSessionIdleMinutes(minutes);
    }

    @Override
    public int getAuditRetentionDays() {
        int days = getInt(SystemConfigKeys.AUDIT_RETENTION_DAYS, DEFAULT_AUDIT_RETENTION_DAYS);
        return BuiltinConfigValidator.clampAuditRetentionDays(days);
    }

    @Override
    public void validatePasswordLength(String password) {
        int min = getPasswordMinLength();
        if (!StringUtils.hasText(password) || password.length() < min) {
            throw new BusinessException(ResultCode.AUTH_PASSWORD_TOO_SHORT.getCode(),
                    "密码至少 " + min + " 位");
        }
        if (password.length() > BuiltinConfigValidator.PASSWORD_MAX_LENGTH) {
            throw new BusinessException(ResultCode.AUTH_PASSWORD_TOO_LONG.getCode(),
                    "密码不能超过 " + BuiltinConfigValidator.PASSWORD_MAX_LENGTH + " 位");
        }
    }

    private String resolve(String configKey) {
        String cached = cache.get(configKey);
        if (cached != null) {
            return cached;
        }
        ensureCacheLoaded();
        return cache.getOrDefault(configKey, "");
    }

    private void ensureCacheLoaded() {
        if (!cache.isEmpty()) {
            return;
        }
        synchronized (cache) {
            if (cache.isEmpty()) {
                cache.putAll(sysConfigRepository.findAllValues());
            }
        }
    }

    private String validateAndNormalize(String valueType, String value) {
        String normalized = value == null ? "" : value;
        if ("JSON".equalsIgnoreCase(valueType)) {
            return ConfigValueValidator.normalizeJson(normalized);
        }
        ConfigValueValidator.validate(valueType, normalized);
        return normalized;
    }

    private String normalizeValueType(String valueType) {
        if (!StringUtils.hasText(valueType)) {
            throw new BusinessException(ResultCode.CONFIG_VALUE_INVALID.getCode(), "值类型不能为空");
        }
        return valueType.trim().toUpperCase(Locale.ROOT);
    }

    private void enrichFlags(ConfigItem item) {
        boolean builtin = SystemConfigKeys.isBuiltin(item.getConfigKey());
        boolean wired = SystemConfigKeys.isWired(item.getConfigKey());
        item.setBuiltin(builtin);
        item.setDeletable(!builtin);
        item.setWired(wired);
        item.setWiredScope(wired ? SystemConfigKeys.wiredScope(item.getConfigKey()) : null);
    }

    private String buildMetaChangeSummary(ConfigItem existing, ConfigMetaUpdateRequest request,
                                        int sortNo, String description) {
        StringBuilder sb = new StringBuilder();
        appendChange(sb, "name", existing.getConfigName(), request.getConfigName().trim());
        appendChange(sb, "group", existing.getConfigGroup(), request.getConfigGroup().trim());
        appendChange(sb, "description", existing.getDescription(), description);
        int oldSort = existing.getSortNo() == null ? 0 : existing.getSortNo();
        if (oldSort != sortNo) {
            if (!sb.isEmpty()) {
                sb.append("; ");
            }
            sb.append("sortNo: ").append(oldSort).append("->").append(sortNo);
        }
        return sb.isEmpty() ? "no-op" : sb.toString();
    }

    private void appendChange(StringBuilder sb, String field, String oldVal, String newVal) {
        String o = oldVal == null ? "" : oldVal;
        String n = newVal == null ? "" : newVal;
        if (o.equals(n)) {
            return;
        }
        if (!sb.isEmpty()) {
            sb.append("; ");
        }
        sb.append(field).append(": ").append(o).append("->").append(n);
    }
}
