import type { ReactElement, ReactNode } from 'react';
import { usePermission } from './usePermission';

interface PermissionProps {
  /** 需要的权限码，命中其一即可（数组为“或”关系）。 */
  code: string | string[];
  children: ReactNode;
  /** 无权限时的回退内容，默认不渲染。 */
  fallback?: ReactNode;
}

/**
 * 按钮/区域级权限控制：无权限默认隐藏。
 * SUPER_ADMIN 在 store 层已短路，这里自动放行。
 *
 * 用法：<Permission code="auth:user:create"><Button>新建</Button></Permission>
 */
export function Permission({ code, children, fallback = null }: PermissionProps): ReactElement {
  const { hasPermission } = usePermission();
  const codes = Array.isArray(code) ? code : [code];
  const allowed = codes.some((c) => hasPermission(c));
  return <>{allowed ? children : fallback}</>;
}
