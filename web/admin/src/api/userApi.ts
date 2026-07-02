import { request } from './request';
import type { LoginDeviceItem, PageResult, RoleItem, UserAdminItem, UserCreateParams, UserUpdateParams } from '@/types/api';

export interface UserPageQuery {
  pageNum?: number;
  pageSize?: number;
  username?: string;
  status?: string;
  roleCode?: string;
}

export const userApi = {
  /** 用户分页列表。 */
  page(params: UserPageQuery): Promise<PageResult<UserAdminItem>> {
    return request<PageResult<UserAdminItem>>({ url: '/v1/auth/admin/users', method: 'GET', params });
  },

  /** 用户详情。 */
  get(userId: number): Promise<UserAdminItem> {
    return request<UserAdminItem>({ url: `/v1/auth/admin/users/${userId}`, method: 'GET' });
  },

  /** 新建用户，返回新用户 ID。 */
  create(data: UserCreateParams): Promise<number> {
    return request<number>({ url: '/v1/auth/admin/users', method: 'POST', data });
  },

  /** 编辑用户。 */
  update(userId: number, data: UserUpdateParams): Promise<void> {
    return request<void>({ url: `/v1/auth/admin/users/${userId}`, method: 'PUT', data });
  },

  /** 管理员重置密码。 */
  resetPassword(userId: number, newPassword: string): Promise<void> {
    return request<void>({
      url: `/v1/auth/admin/users/${userId}/reset-password`,
      method: 'POST',
      data: { newPassword },
    });
  },

  /** 启用账号。 */
  enable(userId: number): Promise<void> {
    return request<void>({ url: `/v1/auth/admin/users/${userId}/enable`, method: 'POST' });
  },

  /** 禁用账号。 */
  disable(userId: number): Promise<void> {
    return request<void>({ url: `/v1/auth/admin/users/${userId}/disable`, method: 'POST' });
  },

  /** 强制下线。 */
  forceLogout(userId: number): Promise<void> {
    return request<void>({ url: `/v1/auth/admin/users/${userId}/force-logout`, method: 'POST' });
  },

  /** 用户登录设备列表。 */
  listLoginDevices(userId: number, limit = 20): Promise<LoginDeviceItem[]> {
    return request<LoginDeviceItem[]>({
      url: `/v1/auth/admin/users/${userId}/login-devices`,
      method: 'GET',
      params: { limit },
    });
  },

  /** 角色下拉列表。 */
  listRoles(): Promise<RoleItem[]> {
    return request<RoleItem[]>({ url: '/v1/auth/admin/roles', method: 'GET' });
  },
};
