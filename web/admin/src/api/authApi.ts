import { request } from './request';
import type { AuthUserInfo, LoginResponse, MenuNode } from '@/types/api';

export interface AdminLoginParams {
  username: string;
  password: string;
  deviceName?: string;
  platform?: string;
  captchaId?: string;
  captchaAnswer?: string;
}

export interface ChangePasswordParams {
  oldPassword: string;
  newPassword: string;
}

export const authApi = {
  /** 后台账号密码登录，返回双令牌 + 用户信息。 */
  login(params: AdminLoginParams): Promise<LoginResponse> {
    return request<LoginResponse>({ url: '/v1/auth/admin/login', method: 'POST', data: params });
  },

  /** 当前登录用户信息。 */
  me(): Promise<AuthUserInfo> {
    return request<AuthUserInfo>({ url: '/v1/auth/me', method: 'GET' });
  },

  /** 当前用户菜单树。 */
  menus(): Promise<MenuNode[]> {
    return request<MenuNode[]>({ url: '/v1/auth/menus', method: 'GET' });
  },

  /** 当前用户按钮/接口权限码。 */
  buttonPermissions(): Promise<string[]> {
    return request<string[]>({ url: '/v1/auth/permissions/buttons', method: 'GET' });
  },

  /** 修改密码，成功后返回续签的新双令牌。 */
  changePassword(params: ChangePasswordParams): Promise<LoginResponse> {
    return request<LoginResponse>({ url: '/v1/auth/password/change', method: 'POST', data: params });
  },

  /** 退出登录，携带 refreshToken 一并拉黑。 */
  logout(refreshToken?: string): Promise<void> {
    return request<void>({ url: '/v1/auth/logout', method: 'POST', data: refreshToken ? { refreshToken } : {} });
  },
};
