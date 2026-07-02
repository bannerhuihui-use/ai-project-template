import { useEffect } from 'react';
import { BrowserRouter } from 'react-router-dom';
import { App as AntdApp, ConfigProvider } from 'antd';
import zhCN from 'antd/locale/zh_CN';
import 'dayjs/locale/zh-cn';
import AppRouter from '@/router/AppRouter';
import SessionGuardHost from '@/auth/SessionGuardHost';
import { themeConfig } from '@/theme/theme';
import { bindFeedback } from '@/utils/feedback';
import { useAppConfigStore } from '@/store/appConfigStore';

/** 把 antd 的 message/modal 实例注入全局 feedback，并注册会话失效回调。 */
function AppBridge() {
  const { message, modal } = AntdApp.useApp();
  const loadConfig = useAppConfigStore((s) => s.load);

  useEffect(() => {
    bindFeedback(message, modal);
    void loadConfig();
  }, [message, modal, loadConfig]);

  return <AppRouter />;
}

export default function App() {
  return (
    <ConfigProvider locale={zhCN} theme={themeConfig}>
      <AntdApp>
        <BrowserRouter future={{ v7_startTransition: true, v7_relativeSplatPath: true }}>
          <SessionGuardHost />
          <AppBridge />
        </BrowserRouter>
      </AntdApp>
    </ConfigProvider>
  );
}
