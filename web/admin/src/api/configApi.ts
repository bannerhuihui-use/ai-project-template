import { request } from './request';
import type { ConfigItem } from '@/types/api';

export interface ConfigBatchUpdateParams {
  items: Array<{ configKey: string; configValue: string }>;
}

export interface ConfigCreateParams {
  configKey: string;
  configName: string;
  configValue?: string;
  configGroup: string;
  valueType: string;
  description?: string;
  sortNo?: number;
}

export interface ConfigMetaUpdateParams {
  configName: string;
  configGroup: string;
  description?: string;
  sortNo?: number;
}

export const configApi = {
  /** 查询系统配置列表，可按分组过滤。 */
  list(group?: string): Promise<ConfigItem[]> {
    return request<ConfigItem[]>({
      url: '/v1/auth/admin/configs',
      method: 'GET',
      params: group ? { group } : undefined,
    });
  },

  /** 批量更新可编辑配置项。 */
  batchUpdate(params: ConfigBatchUpdateParams): Promise<void> {
    return request<void>({
      url: '/v1/auth/admin/configs',
      method: 'PUT',
      data: params,
    });
  },

  /** 新建配置项。 */
  create(params: ConfigCreateParams): Promise<number> {
    return request<number>({
      url: '/v1/auth/admin/configs',
      method: 'POST',
      data: params,
    });
  },

  /** 更新配置元数据（名称、分组、说明、排序）。 */
  updateMetadata(id: number, params: ConfigMetaUpdateParams): Promise<void> {
    return request<void>({
      url: `/v1/auth/admin/configs/${id}`,
      method: 'PATCH',
      data: params,
    });
  },

  /** 删除自定义配置项。 */
  remove(id: number): Promise<void> {
    return request<void>({
      url: `/v1/auth/admin/configs/${id}`,
      method: 'DELETE',
    });
  },
};
