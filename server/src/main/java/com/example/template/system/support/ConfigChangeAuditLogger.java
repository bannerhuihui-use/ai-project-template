package com.example.template.system.support;

import com.example.template.auth.security.CurrentUser;
import com.example.template.auth.security.SecurityUtils;
import com.example.template.common.filter.TraceIdFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.util.StringUtils;

/**
 * 系统配置变更审计日志（写入集中日志，可在审计页按关键字「配置变更审计」检索）。
 */
public final class ConfigChangeAuditLogger {

    private static final Logger log = LoggerFactory.getLogger(ConfigChangeAuditLogger.class);

    private static final int MAX_VALUE_LOG_LEN = 200;

    private ConfigChangeAuditLogger() {
    }

    public static void auditCreate(String configKey, String valueType, String configGroup) {
        log.info("配置变更审计: action=CREATE, traceId={}, operatorId={}, configKey={}, valueType={}, configGroup={}",
                traceId(), operatorId(), configKey, valueType, configGroup);
    }

    public static void auditUpdateValue(String configKey, String oldValue, String newValue) {
        log.info("配置变更审计: action=UPDATE_VALUE, traceId={}, operatorId={}, configKey={}, oldValue={}, newValue={}",
                traceId(), operatorId(), configKey, sanitize(oldValue), sanitize(newValue));
    }

    public static void auditUpdateMeta(String configKey, String summary) {
        log.info("配置变更审计: action=UPDATE_META, traceId={}, operatorId={}, configKey={}, changes={}",
                traceId(), operatorId(), configKey, sanitize(summary));
    }

    public static void auditDelete(String configKey) {
        log.info("配置变更审计: action=DELETE, traceId={}, operatorId={}, configKey={}",
                traceId(), operatorId(), configKey);
    }

    private static Long operatorId() {
        CurrentUser user = SecurityUtils.getCurrentUser();
        return user == null ? null : user.getUserId();
    }

    private static String traceId() {
        String id = MDC.get(TraceIdFilter.MDC_KEY);
        return id == null ? "-" : id;
    }

    private static String sanitize(String value) {
        if (!StringUtils.hasText(value)) {
            return "";
        }
        String cleaned = value.replace('\n', ' ').replace('\r', ' ');
        if (cleaned.length() > MAX_VALUE_LOG_LEN) {
            return cleaned.substring(0, MAX_VALUE_LOG_LEN) + "...";
        }
        return cleaned;
    }
}
