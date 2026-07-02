package com.example.template.file.support;

import com.example.template.auth.security.CurrentUser;
import com.example.template.auth.security.SecurityUtils;
import com.example.template.common.filter.TraceIdFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

/**
 * 文件操作审计日志（关键字「文件上传审计」）。
 */
public final class FileAuditLogger {

    private static final Logger log = LoggerFactory.getLogger(FileAuditLogger.class);

    private FileAuditLogger() {
    }

    public static void auditUpload(String fileKey, String bizType, String accessLevel, long fileSize) {
        log.info("文件上传审计: action=UPLOAD, traceId={}, operatorId={}, fileKey={}, bizType={}, accessLevel={}, fileSize={}",
                traceId(), operatorId(), fileKey, bizType, accessLevel, fileSize);
    }

    public static void auditDelete(String fileKey, String bizType) {
        log.info("文件上传审计: action=DELETE, traceId={}, operatorId={}, fileKey={}, bizType={}",
                traceId(), operatorId(), fileKey, bizType);
    }

    private static Long operatorId() {
        CurrentUser user = SecurityUtils.getCurrentUser();
        return user == null ? null : user.getUserId();
    }

    private static String traceId() {
        String id = MDC.get(TraceIdFilter.MDC_KEY);
        return id == null ? "-" : id;
    }
}
