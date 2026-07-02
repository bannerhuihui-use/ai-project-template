import { create } from 'zustand';
import { authApi, type AdminLoginParams } from '@/api/authApi';
import { tokenStore } from '@/auth/tokenStore';
import type { AuthUserInfo, MenuNode } from '@/types/api';

/** 与后端约定的超级管理员角色码：前端同样短路所有按钮权限。 */
const SUPER_ADMIN_ROLE = 'SUPER_ADMIN';

export type SessionStatus = 'idle' | 'loading' | 'authenticated' | 'anonymous';

interface SessionState {
  status: SessionStatus;
  user: AuthUserInfo | null;
  menus: MenuNode[];
  permissionCodes: Set<string>;

  /** 拉取 me + menus + buttons，构建完整会话。用于登录后与刷新页面恢复。 */
  bootstrap: () => Promise<void>;
  login: (params: AdminLoginParams) => Promise<void>;
  logout: () => Promise<void>;
  /** 仅清理本地会话（令牌失效时由请求层调用）。 */
  reset: () => void;
  hasPermission: (code: string) => boolean;
  isSuperAdmin: () => boolean;
}

export const useSessionStore = create<SessionState>((set, get) => ({
  // 无本地令牌时直接匿名，避免登录页首屏卡在 bootstrap 加载态（尤其无痕窗口）。
  status: tokenStore.hasSession() ? 'idle' : 'anonymous',
  user: null,
  menus: [],
  permissionCodes: new Set<string>(),

  async bootstrap() {
    if (!tokenStore.hasSession()) {
      set({ status: 'anonymous', user: null, menus: [], permissionCodes: new Set() });
      return;
    }
    set({ status: 'loading' });
    try {
      const [user, menus, codes] = await Promise.all([
        authApi.me(),
        authApi.menus(),
        authApi.buttonPermissions(),
      ]);
      set({ status: 'authenticated', user, menus, permissionCodes: new Set(codes) });
    } catch {
      // me/menus 失败说明会话不可用，回到匿名态（401 场景由 axios 拦截器负责跳转登录）。
      tokenStore.clear();
      set({ status: 'anonymous', user: null, menus: [], permissionCodes: new Set() });
    }
  },

  async login(params) {
    const resp = await authApi.login(params);
    tokenStore.setTokens(resp.accessToken, resp.refreshToken);
    await get().bootstrap();
  },

  async logout() {
    try {
      await authApi.logout(tokenStore.getRefreshToken() ?? undefined);
    } catch {
      // 退出接口失败不阻塞本地清理。
    }
    get().reset();
  },

  reset() {
    tokenStore.clear();
    set({ status: 'anonymous', user: null, menus: [], permissionCodes: new Set() });
  },

  hasPermission(code: string) {
    if (get().isSuperAdmin()) return true;
    return get().permissionCodes.has(code);
  },

  isSuperAdmin() {
    return get().user?.roles?.includes(SUPER_ADMIN_ROLE) ?? false;
  },
}));
