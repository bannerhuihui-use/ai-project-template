package com.example.template.auth.support;

import com.example.template.auth.security.CurrentUser;
import com.example.template.auth.security.SecurityUtils;
import com.example.template.common.filter.TraceIdFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.util.StringUtils;

/**
 * RBAC 变更审计日志（可在审计页按关键字「RBAC变更审计」检索）。
 */
public final class RbacChangeAuditLogger {

    private static final Logger log = LoggerFactory.getLogger(RbacChangeAuditLogger.class);

    private static final int MAX_SUMMARY_LEN = 300;

    private RbacChangeAuditLogger() {
    }

    public static void auditRoleCreate(Long roleId, String roleCode) {
        log.info("RBAC变更审计: action=ROLE_CREATE, traceId={}, operatorId={}, roleId={}, roleCode={}",
                traceId(), operatorId(), roleId, roleCode);
    }

    public static void auditRolePermissions(Long roleId, int permissionCount) {
        log.info("RBAC变更审计: action=ROLE_PERMISSION_REPLACE, traceId={}, operatorId={}, roleId={}, permissionCount={}",
                traceId(), operatorId(), roleId, permissionCount);
    }

    public static void auditUserRoles(Long userId, int roleCount, String roleCodesSummary) {
        log.info("RBAC变更审计: action=USER_ROLE_REPLACE, traceId={}, operatorId={}, userId={}, roleCount={}, roleCodes={}",
                traceId(), operatorId(), userId, roleCount, sanitize(roleCodesSummary));
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
        if (cleaned.length() > MAX_SUMMARY_LEN) {
            return cleaned.substring(0, MAX_SUMMARY_LEN) + "...";
        }
        return cleaned;
    }
}
