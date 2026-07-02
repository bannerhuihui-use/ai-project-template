import { useEffect, useMemo, type ReactElement } from 'react';
import { Navigate, Route, Routes, useLocation } from 'react-router-dom';
import { Spin } from 'antd';
import AdminLayout from '@/layout/AdminLayout';
import LoginPage from '@/pages/login/LoginPage';
import DashboardPage from '@/pages/dashboard/DashboardPage';
import ChangePasswordPage from '@/pages/profile/ChangePasswordPage';
import { Forbidden, NotFound, ServerError } from '@/pages/error/ErrorPage';
import { useSessionStore } from '@/store/sessionStore';
import { flattenMenuRoutes } from './menuUtils';
import { resolvePageComponent } from './pageRegistry';

/** 静态已实现路由，动态菜单不重复注册这些路径。 */
const RESERVED_PATHS = new Set(['/dashboard', '/profile/password', '/403', '/500']);

function FullScreenSpin() {
  return (
    <div style={{ minHeight: '100vh', display: 'grid', placeItems: 'center' }}>
      <Spin size="large" />
    </div>
  );
}

function RequireAuth({ children }: { children: ReactElement }): ReactElement {
  const status = useSessionStore((s) => s.status);
  const location = useLocation();
  if (status !== 'authenticated') {
    return <Navigate to="/login" replace state={{ from: location.pathname }} />;
  }
  return children;
}

export default function AppRouter() {
  const status = useSessionStore((s) => s.status);
  const menus = useSessionStore((s) => s.menus);
  const bootstrap = useSessionStore((s) => s.bootstrap);

  useEffect(() => {
    if (status === 'idle') void bootstrap();
  }, [status, bootstrap]);

  const dynamicRoutes = useMemo(() => {
    return flattenMenuRoutes(menus)
      .filter((r) => !RESERVED_PATHS.has(r.path))
      .map((r) => {
        if (r.redirect) {
          return <Route key={r.path} path={r.path} element={<Navigate to={r.redirect} replace />} />;
        }
        const Page = resolvePageComponent(r.component);
        return <Route key={r.path} path={r.path} element={<Page />} />;
      });
  }, [menus]);

  if (status === 'idle' || status === 'loading') {
    return <FullScreenSpin />;
  }

  return (
    <Routes>
      <Route
        path="/login"
        element={status === 'authenticated' ? <Navigate to="/dashboard" replace /> : <LoginPage />}
      />
      <Route
        element={
          <RequireAuth>
            <AdminLayout />
          </RequireAuth>
        }
      >
        <Route path="/" element={<Navigate to="/dashboard" replace />} />
        <Route path="/dashboard" element={<DashboardPage />} />
        <Route path="/profile/password" element={<ChangePasswordPage />} />
        {dynamicRoutes}
        <Route path="/403" element={<Forbidden />} />
        <Route path="/500" element={<ServerError />} />
        <Route path="*" element={<NotFound />} />
      </Route>
    </Routes>
  );
}
