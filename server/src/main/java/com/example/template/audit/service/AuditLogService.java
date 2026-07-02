package com.example.template.audit.service;

import com.example.template.audit.dto.AuditLogItem;
import com.example.template.common.PageResult;

import java.util.Date;

/**
 * 审计日志查询服务（数据源：Mongo 中央日志库）。
 */
public interface AuditLogService {

    /**
     * 分页查询集中日志。
     *
     * @param pageNum  页码，从 1 开始
     * @param pageSize 每页条数
     * @param traceId  链路 ID（精确）
     * @param level    日志级别（精确）
     * @param keyword  消息/Logger 关键字（模糊）
     * @param from     起始时间（含）
     * @param to       结束时间（含）
     */
    PageResult<AuditLogItem> page(int pageNum, int pageSize, String traceId, String level,
                                  String keyword, Date from, Date to);
}
