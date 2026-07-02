import { lazy, type ComponentType, type LazyExoticComponent } from 'react';
import PlaceholderPage from '@/pages/common/PlaceholderPage';

/**
 * 页面组件注册表：把后端菜单的 component 字段（如 "/pages/auth/user/index"）
 * 映射到 src/pages 下的真实文件。未实现的页面回退到占位页，保证菜单可用、不白屏。
 */
const modules = import.meta.glob('/src/pages/**/*.tsx');

/** 兼容多种写法：/pages/x、pages/x、/pages/x/index、带或不带 .tsx。 */
function candidatePaths(component: string): string[] {
  const normalized = component.replace(/^\/+/, '').replace(/\.tsx$/, '');
  const base = normalized.startsWith('pages/') ? normalized : `pages/${normalized}`;
  return [`/src/${base}.tsx`, `/src/${base}/index.tsx`];
}

export function resolvePageComponent(component?: string): LazyExoticComponent<ComponentType> {
  if (!component) {
    return lazy(async () => ({ default: PlaceholderPage }));
  }
  for (const key of candidatePaths(component)) {
    const loader = modules[key];
    if (loader) {
      return lazy(loader as () => Promise<{ default: ComponentType }>);
    }
  }
  return lazy(async () => ({ default: PlaceholderPage }));
}
