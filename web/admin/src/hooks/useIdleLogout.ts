import { useEffect, useRef } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAppConfigStore } from '@/store/appConfigStore';
import { useSessionStore } from '@/store/sessionStore';
import { feedback } from '@/utils/feedback';

const ACTIVITY_EVENTS: (keyof WindowEventMap)[] = [
  'mousedown',
  'mousemove',
  'keydown',
  'scroll',
  'touchstart',
];

/**
 * 按系统配置的空闲超时自动登出（仅已登录态生效）。
 */
export function useIdleLogout() {
  const navigate = useNavigate();
  const sessionIdleMinutes = useAppConfigStore((s) => s.config.sessionIdleMinutes);
  const status = useSessionStore((s) => s.status);
  const logout = useSessionStore((s) => s.logout);
  const timerRef = useRef<ReturnType<typeof setTimeout> | null>(null);

  useEffect(() => {
    if (status !== 'authenticated' || sessionIdleMinutes <= 0) {
      return;
    }

    const timeoutMs = sessionIdleMinutes * 60 * 1000;

    const resetTimer = () => {
      if (timerRef.current) clearTimeout(timerRef.current);
      timerRef.current = setTimeout(() => {
        void (async () => {
          await logout();
          feedback.warning('长时间未操作，已自动退出登录');
          navigate('/login', { replace: true });
        })();
      }, timeoutMs);
    };

    resetTimer();
    ACTIVITY_EVENTS.forEach((event) => window.addEventListener(event, resetTimer, { passive: true }));

    return () => {
      if (timerRef.current) clearTimeout(timerRef.current);
      ACTIVITY_EVENTS.forEach((event) => window.removeEventListener(event, resetTimer));
    };
  }, [status, sessionIdleMinutes, logout, navigate]);
}
