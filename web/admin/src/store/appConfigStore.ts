import { create } from 'zustand';
import { publicApi } from '@/api/publicApi';
import type { AppPublicConfig } from '@/types/api';

const DEFAULT_CONFIG: AppPublicConfig = {
  siteName: '管理后台',
  siteLogo: '',
  siteCopyright: '© 2026 Template',
  maintenanceMode: false,
  captchaEnabled: false,
  passwordMinLength: 8,
  sessionIdleMinutes: 120,
  auditRetentionDays: 30,
};

interface AppConfigState {
  config: AppPublicConfig;
  loaded: boolean;
  load: () => Promise<void>;
  refresh: () => Promise<void>;
}

export const useAppConfigStore = create<AppConfigState>((set) => ({
  config: DEFAULT_CONFIG,
  loaded: false,

  async load() {
    try {
      const config = await publicApi.getAppConfig();
      set({ config: { ...DEFAULT_CONFIG, ...config }, loaded: true });
    } catch {
      set({ loaded: true });
    }
  },

  async refresh() {
    await useAppConfigStore.getState().load();
  },
}));

/** 非 React 场景读取密码最小长度等策略。 */
export function getAppConfig(): AppPublicConfig {
  return useAppConfigStore.getState().config;
}
