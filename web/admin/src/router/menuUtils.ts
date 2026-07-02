import type { MenuNode } from '@/types/api';

export interface FlatMenuRoute {
  path: string;
  component?: string;
  title: string;
  redirect?: string;
}

/** 仅保留可见菜单。 */
export function isVisible(node: MenuNode): boolean {
  return node.visible !== false;
}

/**
 * 把菜单树展平成可注册的路由列表：取所有带 path 且非外链的节点。
 * 目录节点（clickable=false）若有 path 也注册，便于 redirect 生效。
 */
export function flattenMenuRoutes(menus: MenuNode[]): FlatMenuRoute[] {
  const result: FlatMenuRoute[] = [];
  const walk = (nodes: MenuNode[]) => {
    nodes.forEach((node) => {
      if (!isVisible(node)) return;
      if (node.path && !node.isExternalLink) {
        result.push({
          path: node.path,
          component: node.component,
          title: node.title,
          redirect: node.redirect,
        });
      }
      if (node.children?.length) walk(node.children);
    });
  };
  walk(menus);
  return result;
}

/** 根据当前路径匹配出面包屑链（从根到当前节点的 title 列表）。 */
export function findBreadcrumbTrail(menus: MenuNode[], pathname: string): string[] {
  const trail: string[] = [];
  const dfs = (nodes: MenuNode[], acc: string[]): boolean => {
    for (const node of nodes) {
      const next = [...acc, node.title];
      if (node.path === pathname) {
        trail.push(...next);
        return true;
      }
      if (node.children?.length && dfs(node.children, next)) return true;
    }
    return false;
  };
  dfs(menus, []);
  return trail;
}
