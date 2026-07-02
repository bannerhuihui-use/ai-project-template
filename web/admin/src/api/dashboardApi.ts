import { request } from './request';
import type { DashboardStats, PageResult, RoleItem, UserAdminItem } from '@/types/api';

/** 静默请求：无权限或失败时不弹全局 toast，由调用方降级展示。 */
async function silentTry<T>(fn: () => Promise<T>, fallback: T): Promise<T> {
  try {
    return await fn();
  } catch {
    return fallback;
  }
}

const EMPTY_PAGE: PageResult<UserAdminItem> = { total: 0, pageNum: 1, pageSize: 1, list: [] };

/** 聚合现有管理接口，生成 Dashboard 统计；各指标按当前用户权限静默降级，避免无权限弹窗。 */
export const dashboardApi = {
  async getStats(): Promise<DashboardStats> {
    const [users, roles, permissions, pingOk] = await Promise.all([
      silentTry(
        () =>
          request<PageResult<UserAdminItem>>({
            url: '/v1/auth/admin/users',
            method: 'GET',
            params: { pageNum: 1, pageSize: 1 },
            silent: true,
          }),
        EMPTY_PAGE,
      ),
      silentTry(
        () => request<RoleItem[]>({ url: '/v1/auth/admin/roles', method: 'GET', silent: true }),
        [] as RoleItem[],
      ),
      silentTry(
        () =>
          request<PageResult<unknown>>({
            url: '/v1/auth/admin/permissions',
            method: 'GET',
            params: { pageNum: 1, pageSize: 1 },
            silent: true,
          }),
        { total: 0, pageNum: 1, pageSize: 1, list: [] },
      ),
      silentTry(
        () =>
          request<{ status?: string }>({ url: '/v1/health', method: 'GET', silent: true }).then(
            (h) => h?.status === 'UP',
          ),
        false,
      ),
    ]);
    return {
      userCount: users.total,
      roleCount: roles.length,
      permissionCount: permissions.total,
      systemOk: pingOk,
    };
  },
};
