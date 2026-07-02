import { Suspense, useEffect, useState } from 'react';
import { Outlet } from 'react-router-dom';
import { Alert, Layout, Spin } from 'antd';
import Sidebar from './Sidebar';
import HeaderBar from './HeaderBar';
import Breadcrumbs from './Breadcrumbs';
import { useIdleLogout } from '@/hooks/useIdleLogout';
import { useAppConfigStore } from '@/store/appConfigStore';
import { useSessionStore } from '@/store/sessionStore';
import { SIDER_COLLAPSED_WIDTH, SIDER_WIDTH } from '@/theme/theme';

const { Sider, Header, Content } = Layout;

export default function AdminLayout() {
  const [collapsed, setCollapsed] = useState(false);
  const loadConfig = useAppConfigStore((s) => s.load);
  const maintenanceMode = useAppConfigStore((s) => s.config.maintenanceMode);
  const isSuperAdmin = useSessionStore((s) => s.isSuperAdmin());

  useIdleLogout();

  useEffect(() => {
    void loadConfig();
  }, [loadConfig]);

  return (
    <Layout style={{ minHeight: '100vh' }}>
      <Sider
        theme="light"
        collapsible
        collapsed={collapsed}
        trigger={null}
        width={SIDER_WIDTH}
        collapsedWidth={SIDER_COLLAPSED_WIDTH}
        style={{ borderInlineEnd: '1px solid #f0f0f0' }}
      >
        <Sidebar collapsed={collapsed} />
      </Sider>
      <Layout>
        <Header style={{ padding: 0, background: '#fff', height: 56, lineHeight: 'normal' }}>
          <HeaderBar collapsed={collapsed} onToggle={() => setCollapsed((v) => !v)} />
        </Header>
        <Content style={{ margin: 24 }}>
          {maintenanceMode && !isSuperAdmin && (
            <Alert
              type="warning"
              showIcon
              style={{ marginBottom: 16 }}
              message="系统维护模式已开启"
              description="当前系统处于维护状态，部分功能可能受限。"
            />
          )}
          <Breadcrumbs />
          <Suspense
            fallback={
              <div style={{ textAlign: 'center', padding: 64 }}>
                <Spin />
              </div>
            }
          >
            <Outlet />
          </Suspense>
        </Content>
      </Layout>
    </Layout>
  );
}
