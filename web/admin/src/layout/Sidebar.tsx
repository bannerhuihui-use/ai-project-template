import { useMemo, useState } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import { Menu } from 'antd';
import type { MenuProps } from 'antd';
import { useAppConfigStore } from '@/store/appConfigStore';
import { useSessionStore } from '@/store/sessionStore';
import type { MenuNode } from '@/types/api';
import { isVisible } from '@/router/menuUtils';
import { renderMenuIcon } from './iconMap';
import { isSafeLogoUrl } from '@/utils/configConstraints';

type AntMenuItem = Required<MenuProps>['items'][number];

/** 把后端菜单树转换为 antd Menu items；clickable=false 作为不可点击分组。 */
function toMenuItems(nodes: MenuNode[]): AntMenuItem[] {
  return nodes
    .filter(isVisible)
    .map((node) => {
      const children = node.children?.length ? toMenuItems(node.children) : undefined;
      const key = node.path || node.permissionCode;
      if (children && children.length > 0) {
        return {
          key,
          icon: renderMenuIcon(node.icon),
          label: node.title,
          children,
        } as AntMenuItem;
      }
      return {
        key,
        icon: renderMenuIcon(node.icon),
        label: node.title,
        disabled: node.clickable === false,
      } as AntMenuItem;
    });
}

/** 收集所有作为父级（含子菜单）的 key，用于默认展开。 */
function collectParentKeys(nodes: MenuNode[]): string[] {
  const keys: string[] = [];
  nodes.forEach((node) => {
    if (node.children?.length) {
      keys.push(node.path || node.permissionCode);
      keys.push(...collectParentKeys(node.children));
    }
  });
  return keys;
}

interface SidebarProps {
  collapsed: boolean;
}

export default function Sidebar({ collapsed }: SidebarProps) {
  const navigate = useNavigate();
  const location = useLocation();
  const menus = useSessionStore((s) => s.menus);
  const siteName = useAppConfigStore((s) => s.config.siteName);
  const siteLogo = useAppConfigStore((s) => s.config.siteLogo);
  const [logoBroken, setLogoBroken] = useState(false);

  const safeLogo = isSafeLogoUrl(siteLogo) && !logoBroken ? siteLogo : undefined;

  const items = useMemo(() => toMenuItems(menus), [menus]);
  const openKeys = useMemo(() => collectParentKeys(menus), [menus]);

  const brandText = collapsed ? siteName.slice(0, 2) : siteName;

  const onClick: MenuProps['onClick'] = ({ key }) => {
    if (key.startsWith('/')) navigate(key);
  };

  return (
    <div style={{ display: 'flex', flexDirection: 'column', height: '100%' }}>
      <div
        style={{
          height: 56,
          display: 'flex',
          alignItems: 'center',
          gap: 10,
          padding: collapsed ? '0 16px' : '0 24px',
          fontWeight: 700,
          fontSize: collapsed ? 16 : 18,
          color: '#0057c2',
          whiteSpace: 'nowrap',
          overflow: 'hidden',
        }}
      >
        {safeLogo && !collapsed ? (
          <img
            src={safeLogo}
            alt={siteName}
            style={{ height: 28, maxWidth: 120, objectFit: 'contain' }}
            onError={() => setLogoBroken(true)}
          />
        ) : null}
        <span>{brandText}</span>
      </div>
      <Menu
        mode="inline"
        items={items}
        selectedKeys={[location.pathname]}
        defaultOpenKeys={collapsed ? undefined : openKeys}
        onClick={onClick}
        style={{ borderInlineEnd: 'none', flex: 1, overflowY: 'auto' }}
      />
    </div>
  );
}
