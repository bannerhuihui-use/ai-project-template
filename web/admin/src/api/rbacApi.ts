import { request } from './request';
import type { PageResult, PermissionItem, RoleCreateParams, RoleItem } from '@/types/api';

export interface PermissionPageQuery {
  pageNum?: number;
  pageSize?: number;
  type?: string;
  keyword?: string;
}

/** 拉取全部权限点（自动翻页，单类最多 500 条）。 */
async function fetchAllPermissions(type?: string): Promise<PermissionItem[]> {
  const pageSize = 100;
  let pageNum = 1;
  let total = 0;
  const all: PermissionItem[] = [];
  do {
    const resp = await request<PageResult<PermissionItem>>({
      url: '/v1/auth/admin/permissions',
      method: 'GET',
      params: { pageNum, pageSize, type },
    });
    total = resp.total;
    all.push(...resp.list);
    pageNum += 1;
  } while (all.length < total && pageNum <= 10);
  return all;
}

export const rbacApi = {
  /** 权限点分页。 */
  pagePermissions(params: PermissionPageQuery): Promise<PageResult<PermissionItem>> {
    return request<PageResult<PermissionItem>>({
      url: '/v1/auth/admin/permissions',
      method: 'GET',
      params,
    });
  },

  /** 拉取全部权限点（菜单+接口，用于角色授权勾选）。 */
  listAllPermissions(): Promise<PermissionItem[]> {
    return fetchAllPermissions();
  },

  /** 查询角色已绑定的权限。 */
  getRolePermissions(roleId: number): Promise<PermissionItem[]> {
    return request<PermissionItem[]>({
      url: `/v1/auth/admin/roles/${roleId}/permissions`,
      method: 'GET',
    });
  },

  /** 覆盖设置角色权限（空数组=清空）。 */
  replaceRolePermissions(roleId: number, permissionCodes: string[]): Promise<void> {
    return request<void>({
      url: `/v1/auth/admin/roles/${roleId}/permissions`,
      method: 'PUT',
      data: { permissionCodes },
    });
  },

  /** 新建角色。 */
  createRole(params: RoleCreateParams): Promise<RoleItem> {
    return request<RoleItem>({
      url: '/v1/auth/admin/roles',
      method: 'POST',
      data: params,
    });
  },
};
