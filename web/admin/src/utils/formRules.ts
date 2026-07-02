import type { Rule } from 'antd/es/form';

const EMAIL_PATTERN = /^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$/;
/** 中国大陆手机号：1 开头 11 位 */
const PHONE_PATTERN = /^1[3-9]\d{9}$/;
const USERNAME_PATTERN = /^[a-zA-Z0-9_]{2,64}$/;

/** 选填；有值时校验邮箱格式。 */
export const optionalEmailRule: Rule = {
  validator: async (_, value?: string) => {
    const trimmed = value?.trim();
    if (!trimmed) return;
    if (!EMAIL_PATTERN.test(trimmed)) {
      throw new Error('邮箱格式不正确');
    }
  },
};

/** 选填；有值时校验手机号格式。 */
export const optionalPhoneRule: Rule = {
  validator: async (_, value?: string) => {
    const trimmed = value?.trim();
    if (!trimmed) return;
    if (!PHONE_PATTERN.test(trimmed)) {
      throw new Error('请输入正确的 11 位手机号');
    }
  },
};

export const usernameRules: Rule[] = [
  { required: true, message: '请输入用户名' },
  { min: 2, max: 64, message: '长度 2~64' },
  { pattern: USERNAME_PATTERN, message: '仅支持字母、数字、下划线' },
];

export const passwordRules: Rule[] = [
  { required: true, message: '请输入密码' },
  { min: 8, message: '至少 8 位' },
];

/** 按系统配置动态生成密码校验规则。 */
export function buildPasswordRules(minLength?: number): Rule[] {
  const min = minLength && minLength > 0 ? minLength : 8;
  return [
    { required: true, message: '请输入密码' },
    { min, message: `至少 ${min} 位` },
  ];
}

/** 提交前将空字符串转为 undefined。 */
export function emptyToUndefined(value?: string): string | undefined {
  const trimmed = value?.trim();
  return trimmed ? trimmed : undefined;
}
