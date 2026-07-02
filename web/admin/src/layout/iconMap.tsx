import type { ReactNode } from 'react';
import {
  AppstoreOutlined,
  AuditOutlined,
  BarChartOutlined,
  DashboardOutlined,
  FileTextOutlined,
  SafetyOutlined,
  SettingOutlined,
  TeamOutlined,
  UserOutlined,
} from '@ant-design/icons';

/** 菜单 icon-key → antd 图标。后端只存 key，前端统一映射，未知 key 回退默认。 */
const ICONS: Record<string, ReactNode> = {
  'icon-dashboard': <DashboardOutlined />,
  'icon-user': <UserOutlined />,
  'icon-team': <TeamOutlined />,
  'icon-setting': <SettingOutlined />,
  'icon-audit': <AuditOutlined />,
  'icon-report': <BarChartOutlined />,
  'icon-file': <FileTextOutlined />,
  'icon-safety': <SafetyOutlined />,
  'icon-app': <AppstoreOutlined />,
};

export function renderMenuIcon(iconKey?: string): ReactNode {
  if (!iconKey) return <AppstoreOutlined />;
  return ICONS[iconKey] ?? <AppstoreOutlined />;
}
