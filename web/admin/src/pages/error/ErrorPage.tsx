import { useNavigate } from 'react-router-dom';
import { Button, Result } from 'antd';
import type { ResultStatusType } from 'antd/es/result';

interface ErrorPageProps {
  status: ResultStatusType;
  title: string;
  subTitle: string;
}

/** 通用异常页，403/404/500 复用。 */
export default function ErrorPage({ status, title, subTitle }: ErrorPageProps) {
  const navigate = useNavigate();
  return (
    <Result
      status={status}
      title={title}
      subTitle={subTitle}
      extra={
        <Button type="primary" onClick={() => navigate('/dashboard')}>
          返回首页
        </Button>
      }
      style={{ paddingTop: 80 }}
    />
  );
}

export const Forbidden = () => (
  <ErrorPage status="403" title="403" subTitle="您没有权限访问此资源，如有疑问请联系系统管理员。" />
);
export const NotFound = () => (
  <ErrorPage status="404" title="404" subTitle="对不起，您访问的页面不存在。" />
);
export const ServerError = () => (
  <ErrorPage status="500" title="500" subTitle="服务器发生内部错误，请稍后重试。" />
);
