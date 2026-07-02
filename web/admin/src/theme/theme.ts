import type { ThemeConfig } from 'antd';

/**
 * 设计 token，映射自 docs/UI/.../DESIGN.md（Systematic Efficiency / Tech Blue）。
 * 统一在 ConfigProvider 注入，保证全站风格与原型一致。
 */
export const themeConfig: ThemeConfig = {
  token: {
    colorPrimary: '#0057c2',
    colorInfo: '#0057c2',
    colorSuccess: '#52c41a',
    colorWarning: '#faad14',
    colorError: '#ff4d4f',
    colorBgLayout: '#f5f7fa',
    borderRadius: 8,
    fontSize: 14,
    fontFamily:
      "Inter, -apple-system, BlinkMacSystemFont, 'Segoe UI', 'PingFang SC', 'Hiragino Sans GB', 'Microsoft YaHei', sans-serif",
  },
  components: {
    Layout: {
      headerBg: '#ffffff',
      headerHeight: 56,
      siderBg: '#ffffff',
      bodyBg: '#f5f7fa',
    },
    Menu: {
      itemHeight: 44,
      itemSelectedBg: '#e8f0fe',
      itemSelectedColor: '#0057c2',
      iconSize: 16,
    },
    Table: {
      headerBg: '#fafafa',
      rowHoverBg: '#f0f7ff',
      cellPaddingBlock: 12,
      cellPaddingInline: 16,
    },
    Card: {
      borderRadiusLG: 8,
    },
    Button: {
      controlHeight: 36,
    },
  },
};

/** 侧边栏宽度（与 DESIGN.md 对齐）。 */
export const SIDER_WIDTH = 256;
export const SIDER_COLLAPSED_WIDTH = 80;
