import { useLocation } from 'react-router-dom';
import { Card, Empty } from 'antd';

/** 占位页：菜单已配置但对应页面尚未实现时展示，避免白屏。 */
export default function PlaceholderPage() {
  const location = useLocation();
  return (
    <Card>
      <Empty
        description={
          <span>
            页面「{location.pathname}」尚未实现
            <br />
            该菜单已在后端配置，前端页面待开发
          </span>
        }
      />
    </Card>
  );
}
