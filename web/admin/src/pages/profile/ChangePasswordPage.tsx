import { useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Alert, Button, Card, Form, Input } from 'antd';
import { authApi } from '@/api/authApi';
import { tokenStore } from '@/auth/tokenStore';
import { useAppConfigStore } from '@/store/appConfigStore';
import { ApiError } from '@/types/api';
import { buildPasswordRules } from '@/utils/formRules';
import { feedback } from '@/utils/feedback';

interface PasswordFormValues {
  oldPassword: string;
  newPassword: string;
  confirmPassword: string;
}

export default function ChangePasswordPage() {
  const navigate = useNavigate();
  const passwordMinLength = useAppConfigStore((s) => s.config.passwordMinLength);
  const [submitting, setSubmitting] = useState(false);
  const [form] = Form.useForm<PasswordFormValues>();
  const newPasswordRules = useMemo(() => buildPasswordRules(passwordMinLength), [passwordMinLength]);

  const onFinish = async (values: PasswordFormValues) => {
    setSubmitting(true);
    try {
      const resp = await authApi.changePassword({
        oldPassword: values.oldPassword,
        newPassword: values.newPassword,
      });
      tokenStore.setTokens(resp.accessToken, resp.refreshToken);
      feedback.success('密码修改成功，已为您续签登录');
      form.resetFields();
      navigate('/dashboard');
    } catch (err) {
      if (!(err instanceof ApiError)) {
        feedback.error('修改失败，请重试');
      }
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <Card title="修改密码" style={{ maxWidth: 520 }}>
      <Alert
        type="info"
        message="为保障账号安全，修改密码后此前签发的其它登录令牌将全部失效。"
        style={{ marginBottom: 16 }}
      />
      <Form<PasswordFormValues> form={form} layout="vertical" requiredMark onFinish={onFinish}>
        <Form.Item
          label="原密码"
          name="oldPassword"
          rules={[{ required: true, message: '请输入原密码' }]}
        >
          <Input.Password placeholder="请输入原密码" />
        </Form.Item>
        <Form.Item label="新密码" name="newPassword" rules={newPasswordRules}>
          <Input.Password placeholder={`至少 ${passwordMinLength} 位`} />
        </Form.Item>
        <Form.Item
          label="确认新密码"
          name="confirmPassword"
          dependencies={['newPassword']}
          rules={[
            { required: true, message: '请再次输入新密码' },
            ({ getFieldValue }) => ({
              validator(_, value) {
                if (!value || getFieldValue('newPassword') === value) return Promise.resolve();
                return Promise.reject(new Error('两次输入的密码不一致'));
              },
            }),
          ]}
        >
          <Input.Password placeholder="请再次输入新密码" />
        </Form.Item>
        <Form.Item style={{ marginBottom: 0 }}>
          <Button type="primary" htmlType="submit" loading={submitting}>
            确认修改
          </Button>
          <Button style={{ marginLeft: 12 }} onClick={() => form.resetFields()}>
            重置
          </Button>
        </Form.Item>
      </Form>
    </Card>
  );
}
