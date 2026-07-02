/**
 * 令牌本地持久化。存储介质由 VITE_TOKEN_STORAGE 决定：
 *  - local：localStorage（关闭浏览器后仍保留）
 *  - session：sessionStorage（关闭标签页即失效）
 * access token 同时缓存在内存，减少频繁读存储。
 */
const ACCESS_KEY = 'admin.accessToken';
const REFRESH_KEY = 'admin.refreshToken';

const storage: Storage =
  import.meta.env.VITE_TOKEN_STORAGE === 'session' ? window.sessionStorage : window.localStorage;

let accessTokenCache: string | null = null;

export const tokenStore = {
  getAccessToken(): string | null {
    if (accessTokenCache) return accessTokenCache;
    accessTokenCache = storage.getItem(ACCESS_KEY);
    return accessTokenCache;
  },

  getRefreshToken(): string | null {
    return storage.getItem(REFRESH_KEY);
  },

  setTokens(accessToken: string, refreshToken: string): void {
    accessTokenCache = accessToken;
    storage.setItem(ACCESS_KEY, accessToken);
    storage.setItem(REFRESH_KEY, refreshToken);
  },

  clear(): void {
    accessTokenCache = null;
    storage.removeItem(ACCESS_KEY);
    storage.removeItem(REFRESH_KEY);
  },

  hasSession(): boolean {
    return Boolean(this.getAccessToken() && this.getRefreshToken());
  },
};
