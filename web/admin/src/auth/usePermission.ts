import { useSessionStore } from '@/store/sessionStore';

/** 权限判断 hook：返回判定函数，组件内可直接调用。 */
export function usePermission() {
  const hasPermission = useSessionStore((s) => s.hasPermission);
  const isSuperAdmin = useSessionStore((s) => s.isSuperAdmin);
  return { hasPermission, isSuperAdmin };
}
