package com.example.template.audit.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Date;

/**
 * 审计/集中日志条目，对齐 Mongo {@code central_logs.app_logs} 文档结构。
 */
@Schema(description = "审计日志条目")
public class AuditLogItem {

    @Schema(description = "Mongo 文档 ID")
    private String id;

    @Schema(description = "服务名", example = "template-server")
    private String service;

    @Schema(description = "环境", example = "local")
    private String env;

    @Schema(description = "日志级别", example = "ERROR")
    private String level;

    @Schema(description = "链路 ID")
    private String traceId;

    @Schema(description = "Logger 名称")
    private String logger;

    @Schema(description = "日志消息")
    private String message;

    @Schema(description = "堆栈（如有）")
    private String stackTrace;

    @Schema(description = "主机名")
    private String host;

    @Schema(description = "日志时间", example = "2026-07-01 12:00:00")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date timestamp;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getService() {
        return service;
    }

    public void setService(String service) {
        this.service = service;
    }

    public String getEnv() {
        return env;
    }

    public void setEnv(String env) {
        this.env = env;
    }

    public String getLevel() {
        return level;
    }

    public void setLevel(String level) {
        this.level = level;
    }

    public String getTraceId() {
        return traceId;
    }

    public void setTraceId(String traceId) {
        this.traceId = traceId;
    }

    public String getLogger() {
        return logger;
    }

    public void setLogger(String logger) {
        this.logger = logger;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getStackTrace() {
        return stackTrace;
    }

    public void setStackTrace(String stackTrace) {
        this.stackTrace = stackTrace;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public Date getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }
}
