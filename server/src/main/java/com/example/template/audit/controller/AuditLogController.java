package com.example.template.audit.controller;

import com.example.template.audit.dto.AuditLogItem;
import com.example.template.audit.service.AuditLogService;
import com.example.template.common.PageResult;
import com.example.template.common.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Date;

/**
 * 审计日志查询（Mongo 中央日志库）。
 */
@Tag(name = "系统-审计日志", description = "分页查询集中式 WARN/ERROR 日志（需 ROLE_ADMIN + auth:audit:read）")
@RestController
@RequestMapping("/api/v1/auth/admin/audit")
public class AuditLogController {

    private final AuditLogService auditLogService;

    public AuditLogController(AuditLogService auditLogService) {
        this.auditLogService = auditLogService;
    }

    @Operation(summary = "分页查询审计日志",
            description = "查询 Mongo central_logs.app_logs，默认仅本服务 WARN/ERROR。"
                    + "需开启 LOG_MONGO_ENABLED=true。需 auth:audit:read。",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "成功",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = Result.class))),
            @ApiResponse(responseCode = "403", description = "无权限（40300）")
    })
    @PreAuthorize("@perm.has('auth:audit:read')")
    @GetMapping("/logs")
    public Result<PageResult<AuditLogItem>> pageLogs(
            @Parameter(description = "页码，从 1 开始") @RequestParam(defaultValue = "1") int pageNum,
            @Parameter(description = "每页大小，最大 100") @RequestParam(defaultValue = "10") int pageSize,
            @Parameter(description = "链路 ID（精确）") @RequestParam(required = false) String traceId,
            @Parameter(description = "日志级别：WARN / ERROR") @RequestParam(required = false) String level,
            @Parameter(description = "消息或 Logger 关键字") @RequestParam(required = false) String keyword,
            @Parameter(description = "起始时间", example = "2026-07-01 00:00:00")
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") Date from,
            @Parameter(description = "结束时间", example = "2026-07-01 23:59:59")
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") Date to) {
        return Result.success(auditLogService.page(pageNum, pageSize, traceId, level, keyword, from, to));
    }
}
