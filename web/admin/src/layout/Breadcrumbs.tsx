import { useMemo } from 'react';
import { useLocation } from 'react-router-dom';
import { Breadcrumb } from 'antd';
import { HomeOutlined } from '@ant-design/icons';
import { useSessionStore } from '@/store/sessionStore';
import { findBreadcrumbTrail } from '@/router/menuUtils';

export default function Breadcrumbs() {
  const location = useLocation();
  const menus = useSessionStore((s) => s.menus);

  const items = useMemo(() => {
    const trail = findBreadcrumbTrail(menus, location.pathname);
    const base = [{ title: <HomeOutlined /> }];
    return base.concat(trail.map((title) => ({ title: <span>{title}</span> })));
  }, [menus, location.pathname]);

  return <Breadcrumb items={items} style={{ marginBottom: 16 }} />;
}
