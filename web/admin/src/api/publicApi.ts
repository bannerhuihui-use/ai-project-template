import { request } from './request';
import type { AppPublicConfig, CaptchaChallenge } from '@/types/api';

export const publicApi = {
  /** 获取公开应用配置（登录页、布局、策略提示）。 */
  getAppConfig(): Promise<AppPublicConfig> {
    return request<AppPublicConfig>({ url: '/v1/public/app-config', method: 'GET', silent: true });
  },

  /** 获取登录验证码（配置开启时）。 */
  getCaptcha(): Promise<CaptchaChallenge | null> {
    return request<CaptchaChallenge | null>({ url: '/v1/public/captcha', method: 'GET', silent: true });
  },
};
