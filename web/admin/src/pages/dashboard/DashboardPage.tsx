import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { Button, Card, Col, Row, Space, Spin, Statistic, Table, Tag, Typography } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import {
  ApiOutlined,
  CheckCircleOutlined,
  CloseCircleOutlined,
  SafetyOutlined,
  TeamOutlined,
  UserOutlined,
} from '@ant-design/icons';
import { auditApi } from '@/api/auditApi';
import { dashboardApi } from '@/api/dashboardApi';
import { useSessionStore } from '@/store/sessionStore';
import type { AuditLogItem, DashboardStats } from '@/types/api';

const { Title, Text } = Typography;

function levelTag(level: string) {
  const upper = level?.toUpperCase() ?? '';
  if (upper === 'ERROR') return <Tag color="error">{level}</Tag>;
  if (upper === 'WARN' || upper === 'WARNING') return <Tag color="warning">{level}</Tag>;
  return <Tag color="processing">{level}</Tag>;
}

export default function DashboardPage() {
  const user = useSessionStore((s) => s.user);
  const [loading, setLoading] = useState(true);
  const [stats, setStats] = useState<DashboardStats | null>(null);
  const [recentLogs, setRecentLogs] = useState<AuditLogItem[]>([]);
  const [auditLoadFailed, setAuditLoadFailed] = useState(false);

  useEffect(() => {
    void (async () => {
      setLoading(true);
      try {
        const s = await dashboardApi.getStats();
        setStats(s);
      } catch {
        setStats({ userCount: 0, roleCount: 0, permissionCount: 0, systemOk: false });
      }
      try {
        const logs = await auditApi.page({ pageNum: 1, pageSize: 5 });
        setRecentLogs(logs.list);
        setAuditLoadFailed(false);
      } catch {
        setRecentLogs([]);
        setAuditLoadFailed(true);
      } finally {
        setLoading(false);
      }
    })();
  }, []);

  const logColumns: ColumnsType<AuditLogItem> = [
    { title: '级别', dataIndex: 'level', width: 80, render: (v: string) => levelTag(v) },
    {
      title: '内容',
      dataIndex: 'message',
      ellipsis: true,
      render: (msg: string, row) => (
        <Space direction="vertical" size={0}>
          <Text ellipsis>{msg}</Text>
          {row.traceId && (
            <Text type="secondary" style={{ fontSize: 11 }}>
              trace: {row.traceId}
            </Text>
          )}
        </Space>
      ),
    },
    { title: '时间', dataIndex: 'timestamp', width: 180 },
  ];

  return (
    <Spin spinning={loading}>
      <Title level={4} style={{ marginTop: 0 }}>
        早安，{user?.nickname || '管理员'} 👋
      </Title>
      <Text type="secondary">这是您的管理概览，数据来自当前系统真实接口。</Text>

      <Row gutter={16} style={{ marginTop: 24 }}>
        <Col xs={24} sm={12} lg={6}>
          <Card>
            <Statistic
              title="后台用户数"
              value={stats?.userCount ?? 0}
              prefix={<TeamOutlined style={{ color: '#0057c2' }} />}
            />
          </Card>
        </Col>
        <Col xs={24} sm={12} lg={6}>
          <Card>
            <Statistic
              title="角色数"
              value={stats?.roleCount ?? 0}
              prefix={<UserOutlined style={{ color: '#52c41a' }} />}
            />
          </Card>
        </Col>
        <Col xs={24} sm={12} lg={6}>
          <Card>
            <Statistic
              title="权限点数"
              value={stats?.permissionCount ?? 0}
              prefix={<ApiOutlined style={{ color: '#faad14' }} />}
            />
          </Card>
        </Col>
        <Col xs={24} sm={12} lg={6}>
          <Card>
            <Statistic
              title="鉴权服务"
              value={stats?.systemOk ? '正常' : '异常'}
              prefix={
                stats?.systemOk ? (
                  <CheckCircleOutlined style={{ color: '#52c41a' }} />
                ) : (
                  <CloseCircleOutlined style={{ color: '#ff4d4f' }} />
                )
              }
            />
          </Card>
        </Col>
      </Row>

      <Row gutter={16} style={{ marginTop: 8 }}>
        <Col xs={24} lg={16}>
          <Card
            title="最近审计日志"
            extra={<Link to="/system/audit">查看全部</Link>}
          >
            {auditLoadFailed && (
              <Text type="secondary" style={{ display: 'block', marginBottom: 8 }}>
                最近日志加载失败，请确认 Mongo 集中日志已开启。
              </Text>
            )}
            <Table<AuditLogItem>
                rowKey={(r) => r.id ?? `${r.timestamp}-${r.message}`}
                columns={logColumns}
                dataSource={recentLogs}
                pagination={false}
                size="small"
                locale={{ emptyText: '暂无日志记录' }}
              />
          </Card>
        </Col>
        <Col xs={24} lg={8}>
          <Card title="快捷操作">
            <Space direction="vertical" style={{ width: '100%' }}>
              <Link to="/system/user">
                <Button block icon={<TeamOutlined />}>
                  用户管理
                </Button>
              </Link>
              <Link to="/system/role">
                <Button block icon={<SafetyOutlined />}>
                  角色权限
                </Button>
              </Link>
              <Link to="/system/audit">
                <Button block>审计日志</Button>
              </Link>
            </Space>
          </Card>
        </Col>
      </Row>
    </Spin>
  );
}
