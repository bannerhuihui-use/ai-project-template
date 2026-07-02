import { useCallback, useEffect, useState } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import { Alert, Button, Card, Checkbox, Form, Input, Space, Typography } from 'antd';
import { LockOutlined, ReloadOutlined, SafetyCertificateTwoTone, UserOutlined } from '@ant-design/icons';
import { publicApi } from '@/api/publicApi';
import { useAppConfigStore } from '@/store/appConfigStore';
import { useSessionStore } from '@/store/sessionStore';
import type { CaptchaChallenge } from '@/types/api';
import { ApiError } from '@/types/api';
import { feedback } from '@/utils/feedback';
import { isSafeLogoUrl } from '@/utils/configConstraints';

const { Title, Text } = Typography;

interface LoginFormValues {
  username: string;
  password: string;
  captchaAnswer?: string;
  remember?: boolean;
}

export default function LoginPage() {
  const navigate = useNavigate();
  const location = useLocation();
  const login = useSessionStore((s) => s.login);
  const loadConfig = useAppConfigStore((s) => s.load);
  const config = useAppConfigStore((s) => s.config);
  const [submitting, setSubmitting] = useState(false);
  const [captcha, setCaptcha] = useState<CaptchaChallenge | null>(null);
  const [logoBroken, setLogoBroken] = useState(false);
  const [form] = Form.useForm<LoginFormValues>();

  const redirectTo = (location.state as { from?: string } | null)?.from ?? '/dashboard';

  const refreshCaptcha = useCallback(async () => {
    if (!config.captchaEnabled) {
      setCaptcha(null);
      return;
    }
    try {
      const challenge = await publicApi.getCaptcha();
      setCaptcha(challenge);
      form.setFieldValue('captchaAnswer', '');
    } catch {
      feedback.error('验证码加载失败，请稍后重试');
    }
  }, [config.captchaEnabled, form]);

  useEffect(() => {
    void loadConfig();
  }, [loadConfig]);

  useEffect(() => {
    void refreshCaptcha();
  }, [refreshCaptcha]);

  const onFinish = async (values: LoginFormValues) => {
    setSubmitting(true);
    try {
      await login({
        username: values.username,
        password: values.password,
        platform: 'WEB',
        captchaId: captcha?.captchaId,
        captchaAnswer: values.captchaAnswer,
      });
      feedback.success('登录成功');
      navigate(redirectTo, { replace: true });
    } catch (err) {
      if (config.captchaEnabled) {
        void refreshCaptcha();
      }
      if (!(err instanceof ApiError)) feedback.error('登录失败，请重试');
    } finally {
      setSubmitting(false);
    }
  };

  const siteTitle = config.siteName || '管理后台';
  const copyright = config.siteCopyright || `© ${new Date().getFullYear()} ${siteTitle}`;
  const showLogo = isSafeLogoUrl(config.siteLogo) && !logoBroken;

  return (
    <div
      style={{
        minHeight: '100vh',
        display: 'flex',
        flexDirection: 'column',
        alignItems: 'center',
        justifyContent: 'center',
        background: 'linear-gradient(180deg, #eef3fb 0%, #f5f7fa 100%)',
        padding: 24,
      }}
    >
      <div style={{ textAlign: 'center', marginBottom: 24 }}>
        {showLogo ? (
          <img
            src={config.siteLogo}
            alt={siteTitle}
            style={{ height: 56, maxWidth: 200, objectFit: 'contain' }}
            onError={() => setLogoBroken(true)}
          />
        ) : (
          <SafetyCertificateTwoTone twoToneColor="#0057c2" style={{ fontSize: 48 }} />
        )}
        <Title level={2} style={{ marginTop: 16, marginBottom: 4 }}>
          {siteTitle}
        </Title>
        <Text type="secondary">企业级高效管理的一站式解决方案</Text>
      </div>

      {config.maintenanceMode && (
        <Alert
          type="warning"
          showIcon
          style={{ width: 400, marginBottom: 16 }}
          message="系统维护中"
          description="当前处于维护模式，仅超级管理员可登录。"
        />
      )}

      <Card style={{ width: 400, boxShadow: '0 4px 12px rgba(0,0,0,0.06)' }}>
        <Form<LoginFormValues>
          form={form}
          layout="vertical"
          requiredMark={false}
          initialValues={{ remember: true }}
          onFinish={onFinish}
        >
          <Form.Item
            label="用户名"
            name="username"
            rules={[{ required: true, message: '请输入用户名' }]}
          >
            <Input prefix={<UserOutlined />} placeholder="请输入您的账号" size="large" autoComplete="username" />
          </Form.Item>

          <Form.Item
            label="密码"
            name="password"
            rules={[{ required: true, message: '请输入密码' }]}
          >
            <Input.Password
              prefix={<LockOutlined />}
              placeholder="请输入您的密码"
              size="large"
              autoComplete="current-password"
            />
          </Form.Item>

          {config.captchaEnabled && (
            <Form.Item label="验证码" required>
              <Space.Compact style={{ width: '100%' }}>
                <Form.Item
                  name="captchaAnswer"
                  noStyle
                  rules={[{ required: true, message: '请输入验证码' }]}
                >
                  <Input placeholder="计算结果" size="large" style={{ flex: 1 }} />
                </Form.Item>
                <Button size="large" icon={<ReloadOutlined />} onClick={() => void refreshCaptcha()} />
              </Space.Compact>
              {captcha?.question && (
                <Text type="secondary" style={{ display: 'block', marginTop: 8 }}>
                  {captcha.question}
                </Text>
              )}
            </Form.Item>
          )}

          <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 16 }}>
            <Form.Item name="remember" valuePropName="checked" noStyle>
              <Checkbox>记住我</Checkbox>
            </Form.Item>
            <Button type="link" style={{ padding: 0 }} disabled>
              忘记密码？
            </Button>
          </div>

          <Form.Item style={{ marginBottom: 0 }}>
            <Button type="primary" htmlType="submit" size="large" block loading={submitting}>
              立即登录
            </Button>
          </Form.Item>
        </Form>
      </Card>

      <Text type="secondary" style={{ marginTop: 24, fontSize: 12 }}>
        {copyright}
      </Text>
    </div>
  );
}
