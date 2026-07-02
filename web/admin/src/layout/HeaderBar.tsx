import { useNavigate } from 'react-router-dom';
import { Avatar, Button, Dropdown, Space, Tag, Typography } from 'antd';
import type { MenuProps } from 'antd';
import {
  KeyOutlined,
  LogoutOutlined,
  MenuFoldOutlined,
  MenuUnfoldOutlined,
  UserOutlined,
} from '@ant-design/icons';
import { useSessionStore } from '@/store/sessionStore';
import { feedback } from '@/utils/feedback';

const { Text } = Typography;

interface HeaderBarProps {
  collapsed: boolean;
  onToggle: () => void;
}

export default function HeaderBar({ collapsed, onToggle }: HeaderBarProps) {
  const navigate = useNavigate();
  const user = useSessionStore((s) => s.user);
  const logout = useSessionStore((s) => s.logout);

  const onMenuClick: MenuProps['onClick'] = async ({ key }) => {
    if (key === 'password') {
      navigate('/profile/password');
    } else if (key === 'logout') {
      await logout();
      feedback.success('已退出登录');
      navigate('/login', { replace: true });
    }
  };

  const items: MenuProps['items'] = [
    { key: 'password', icon: <KeyOutlined />, label: '修改密码' },
    { type: 'divider' },
    { key: 'logout', icon: <LogoutOutlined />, label: '退出登录', danger: true },
  ];

  return (
    <div
      style={{
        height: 56,
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'space-between',
        padding: '0 16px',
        borderBottom: '1px solid #f0f0f0',
        background: '#fff',
      }}
    >
      <Button
        type="text"
        icon={collapsed ? <MenuUnfoldOutlined /> : <MenuFoldOutlined />}
        onClick={onToggle}
      />
      <Dropdown menu={{ items, onClick: onMenuClick }} placement="bottomRight">
        <Space style={{ cursor: 'pointer' }}>
          <Avatar size="small" icon={<UserOutlined />} style={{ background: '#0057c2' }} />
          <Text strong>{user?.nickname || '未命名用户'}</Text>
          {user?.roles?.[0] && <Tag color="blue">{user.roles[0]}</Tag>}
        </Space>
      </Dropdown>
    </div>
  );
}
