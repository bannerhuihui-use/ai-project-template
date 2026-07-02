import type { Rule } from 'antd/es/form';

/** JSON 配置值最大字符数（与后端 ConfigValueValidator.JSON_MAX_LENGTH 对齐）。 */
export const JSON_MAX_LENGTH = 65_536;

export const JSON_VALUE_HINT = `须为合法 JSON 对象 {} 或数组 []，最长 ${JSON_MAX_LENGTH} 字符`;
export const BUILTIN_VALUE_HINTS: Record<string, string> = {
  'auth.password.min_length': '有效范围 6~64',
  'auth.session.idle_minutes': '有效范围 5~1440 分钟',
  'system.audit.retention_days': '有效范围 1~365 天',
  'site.logo': '留空、填写 http(s):// 地址，或使用下方上传 Logo',
  'site.name': '最长 128 字符',
  'site.copyright': '最长 256 字符',
};

const SAFE_LOGO_URL = /^https?:\/\/.+/i;

export function builtinNumberRange(configKey: string): { min: number; max: number } | null {
  switch (configKey) {
    case 'auth.password.min_length':
      return { min: 6, max: 64 };
    case 'auth.session.idle_minutes':
      return { min: 5, max: 1440 };
    case 'system.audit.retention_days':
      return { min: 1, max: 365 };
    default:
      return null;
  }
}

/** 仅允许 http/https 图片地址，用于展示与表单校验。 */
export function isSafeLogoUrl(url?: string): boolean {
  const trimmed = url?.trim();
  if (!trimmed) return false;
  return SAFE_LOGO_URL.test(trimmed);
}

/** 内置配置项表单校验规则。 */
export function builtinValueRules(item: { configKey: string; valueType: string }): Rule[] {
  const rules: Rule[] = [];
  const range = builtinNumberRange(item.configKey);
  if (item.valueType === 'NUMBER' && range) {
    rules.push({
      validator: async (_, value?: string | number) => {
        if (value == null || value === '') return;
        const num = typeof value === 'number' ? value : Number(value);
        if (!Number.isFinite(num)) {
          throw new Error('须为数字');
        }
        if (num < range.min || num > range.max) {
          throw new Error(`须在 ${range.min}~${range.max} 之间`);
        }
      },
    });
  }
  if (item.configKey === 'site.logo') {
    rules.push({
      validator: async (_, value?: string) => {
        const trimmed = value?.trim();
        if (!trimmed) return;
        if (!isSafeLogoUrl(trimmed)) {
          throw new Error('Logo 须为 http:// 或 https:// 开头的地址');
        }
      },
    });
  }
  if (item.configKey === 'site.name') {
    rules.push({ max: 128, message: '最长 128 字符' });
  }
  if (item.configKey === 'site.copyright') {
    rules.push({ max: 256, message: '最长 256 字符' });
  }
  return rules;
}

export function builtinValueHint(configKey: string): string | undefined {
  return BUILTIN_VALUE_HINTS[configKey];
}

/** JSON 类型配置值校验（与后端 ConfigValueValidator 对齐）。 */
export function jsonValueRules(): Rule[] {
  return [
    {
      validator: async (_, value?: string) => {
        const trimmed = value?.trim();
        if (!trimmed) return;
        if (trimmed.length > JSON_MAX_LENGTH) {
          throw new Error(`JSON 长度不能超过 ${JSON_MAX_LENGTH} 字符`);
        }
        try {
          const parsed: unknown = JSON.parse(trimmed);
          if (parsed === null || typeof parsed !== 'object') {
            throw new Error('须为合法的 JSON 对象或数组');
          }
        } catch (error) {
          if (error instanceof Error && error.message === '须为合法的 JSON 对象或数组') {
            throw error;
          }
          throw new Error('须为合法的 JSON 对象或数组');
        }
      },
    },
  ];
}
