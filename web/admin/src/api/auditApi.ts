import { request } from './request';
import type { AuditLogItem, PageResult } from '@/types/api';

/**
 * 审计日志查询参数。
 * 联调契约：GET /api/v1/auth/admin/audit/logs（后端查 Mongo central_logs.app_logs）。
 */
export interface AuditLogQuery {
  pageNum?: number;
  pageSize?: number;
  traceId?: string;
  level?: string;
  keyword?: string;
  from?: string;
  to?: string;
}

export const auditApi = {
  /** 分页查询审计/集中日志。接口未就绪时由页面捕获异常并展示空态。 */
  page(params: AuditLogQuery): Promise<PageResult<AuditLogItem>> {
    return request<PageResult<AuditLogItem>>({
      url: '/v1/auth/admin/audit/logs',
      method: 'GET',
      params,
      silent: true,
    });
  },
};
