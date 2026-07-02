import type { NavigateFunction } from 'react-router-dom';
import { feedback } from '@/utils/feedback';

/** 视为「会话失效、需重新登录」的业务码（与后端 ResultCode 对齐）。 */
const SESSION_EXPIRED_CODES = new Set([
  40100, // UNAUTHORIZED
  40103, // AUTH_TOKEN_INVALID
  40104, // AUTH_REFRESH_TOKEN_INVALID
  40105, // AUTH_TOKEN_BLACKLISTED
  40106, // AUTH_TOKEN_TYPE_INVALID
  40107, // AUTH_TOKEN_VERSION_EXPIRED
]);

let navigateFn: NavigateFunction | null = null;
let onSessionReset: (() => void) | null = null;
let expiring = false;

export function isSessionExpiredCode(code: number): boolean {
  return SESSION_EXPIRED_CODES.has(code);
}

/** 在 Router 内注册 navigate 与会话清理回调（App 启动时调用一次）。 */
export function bindSessionGuard(navigate: NavigateFunction, resetSession: () => void): void {
  navigateFn = navigate;
  onSessionReset = resetSession;
}

/**
 * 统一会话失效：清理令牌与 store，提示并跳转登录页。
 * 可被 axios 拦截器、bootstrap 等复用，内部防抖避免并发 401 重复跳转。
 */
export function handleSessionExpired(message = '登录已失效，请重新登录'): void {
  if (expiring) return;
  expiring = true;

  onSessionReset?.();

  if (message) {
    feedback.warning(message);
  }

  const pathname = window.location.pathname;
  navigateFn?.('/login', {
    replace: true,
    state: pathname && pathname !== '/login' ? { from: pathname } : undefined,
  });

  window.setTimeout(() => {
    expiring = false;
  }, 800);
}
