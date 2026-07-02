import axios, {
  AxiosError,
  type AxiosInstance,
  type AxiosRequestConfig,
  type InternalAxiosRequestConfig,
} from 'axios';
import { handleSessionExpired, isSessionExpiredCode } from '@/auth/sessionGuard';
import { tokenStore } from '@/auth/tokenStore';
import { feedback } from '@/utils/feedback';
import { ApiError, type LoginResponse, type Result } from '@/types/api';

/** 业务成功码。 */
const CODE_SUCCESS = 0;
/** 刷新接口路径，命中后不做 401 自动续签（避免递归）。 */
const REFRESH_PATH = '/v1/auth/token/refresh';

/** 标记请求是否已重试过一次，防止刷新后仍失败导致死循环。 */
interface RetriableConfig extends InternalAxiosRequestConfig {
  _retried?: boolean;
  /** 静默处理：错误不弹全局提示，由调用方自行处理。 */
  silent?: boolean;
}

const http: AxiosInstance = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api',
  timeout: 15000,
});

http.interceptors.request.use((config: InternalAxiosRequestConfig) => {
  const token = tokenStore.getAccessToken();
  if (token && config.headers) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

/** 刷新单飞：并发 401 只触发一次刷新，其余请求等待同一个 Promise。 */
let refreshing: Promise<string | null> | null = null;

async function doRefresh(): Promise<string | null> {
  const refreshToken = tokenStore.getRefreshToken();
  if (!refreshToken) return null;
  try {
    // 用裸 axios 调刷新，避免走拦截器再次触发 401 续签。
    const resp = await axios.post<Result<LoginResponse>>(
      `${import.meta.env.VITE_API_BASE_URL || '/api'}${REFRESH_PATH}`,
      { refreshToken },
      { timeout: 15000 },
    );
    const body = resp.data;
    if (body.code === CODE_SUCCESS && body.data) {
      tokenStore.setTokens(body.data.accessToken, body.data.refreshToken);
      return body.data.accessToken;
    }
    return null;
  } catch {
    return null;
  }
}

function isRefreshRequest(url?: string): boolean {
  return Boolean(url?.includes(REFRESH_PATH));
}

http.interceptors.response.use(
  (response) => {
    const body = response.data as Result<unknown>;
    // 非标准包装（如二进制流）直接放行。
    if (body == null || typeof body.code !== 'number') {
      return response;
    }
    if (body.code === CODE_SUCCESS) {
      return response;
    }

    const config = response.config as RetriableConfig;

    // HTTP 200 但业务码为会话失效：清理并跳转登录（refresh 接口除外，由 401 分支统一处理）。
    if (isSessionExpiredCode(body.code) && !isRefreshRequest(config.url)) {
      handleSessionExpired(body.message || '登录已失效，请重新登录');
      return Promise.reject(new ApiError(body.code, body.message || '登录已失效，请重新登录'));
    }

    // HTTP 200 但其他业务失败：抛出 ApiError，按需提示。
    if (!config.silent) {
      feedback.error(body.message || '请求失败');
    }
    return Promise.reject(new ApiError(body.code, body.message || '请求失败'));
  },
  async (error: AxiosError<Result<unknown>>) => {
    const config = error.config as RetriableConfig | undefined;
    const status = error.response?.status;

    // 401：尝试一次刷新后重放原请求。
    if (status === 401 && config && !config._retried && !isRefreshRequest(config.url)) {
      config._retried = true;
      if (!refreshing) {
        refreshing = doRefresh().finally(() => {
          refreshing = null;
        });
      }
      const newToken = await refreshing;
      if (newToken) {
        if (config.headers) config.headers.Authorization = `Bearer ${newToken}`;
        return http.request(config);
      }
      handleSessionExpired('登录已失效，请重新登录');
      return Promise.reject(new ApiError(40107, '登录已失效，请重新登录'));
    }

    if (status === 401) {
      handleSessionExpired('登录已失效，请重新登录');
      return Promise.reject(new ApiError(40100, '未登录或登录已失效'));
    }

    if (status === 403) {
      if (!config?.silent) feedback.error('无权限访问该资源');
      return Promise.reject(new ApiError(40300, '无权限访问该资源'));
    }

    // 5xx / 网络错误。
    const body = error.response?.data;
    const msg = body?.message || (status && status >= 500 ? '服务异常，请稍后重试' : '网络异常');
    if (!config?.silent) feedback.error(msg);
    return Promise.reject(new ApiError(body?.code ?? status ?? -1, msg));
  },
);

/** 扩展请求配置：支持静默错误（不弹 toast）。 */
export interface RequestConfig extends AxiosRequestConfig {
  silent?: boolean;
}

/** 统一解包 Result<T>.data，业务调用直接拿到数据。 */
export async function request<T>(config: RequestConfig): Promise<T> {
  const resp = await http.request<Result<T>>(config as InternalAxiosRequestConfig);
  return (resp.data as Result<T>).data;
}

export default http;
