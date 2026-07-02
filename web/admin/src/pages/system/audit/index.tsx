import { useCallback, useEffect, useState } from 'react';
import {
  Alert,
  Button,
  Card,
  DatePicker,
  Form,
  Input,
  Select,
  Space,
  Table,
  Tag,
  Typography,
} from 'antd';
import type { ColumnsType, TablePaginationConfig } from 'antd/es/table';
import { ReloadOutlined, SearchOutlined } from '@ant-design/icons';
import type { Dayjs } from 'dayjs';
import { auditApi } from '@/api/auditApi';
import { useAppConfigStore } from '@/store/appConfigStore';
import type { AuditLogItem } from '@/types/api';

const { Text } = Typography;
const { RangePicker } = DatePicker;

interface SearchValues {
  traceId?: string;
  level?: string;
  keyword?: string;
  timeRange?: [Dayjs, Dayjs];
}

const LEVEL_OPTIONS = [
  { label: '全部级别', value: '' },
  { label: 'ERROR', value: 'ERROR' },
  { label: 'WARN', value: 'WARN' },
];

function levelTag(level: string) {
  const upper = level?.toUpperCase() ?? '';
  if (upper === 'ERROR') return <Tag color="error">{level}</Tag>;
  if (upper === 'WARN' || upper === 'WARNING') return <Tag color="warning">{level}</Tag>;
  return <Tag>{level}</Tag>;
}

/** 审计日志页：查询 Mongo 集中日志（GET /api/v1/auth/admin/audit/logs）。 */
export default function AuditLogPage() {
  const auditRetentionDays = useAppConfigStore((s) => s.config.auditRetentionDays);
  const [form] = Form.useForm<SearchValues>();
  const [loading, setLoading] = useState(false);
  const [apiError, setApiError] = useState(false);
  const [data, setData] = useState<AuditLogItem[]>([]);
  const [total, setTotal] = useState(0);
  const [pageNum, setPageNum] = useState(1);
  const [pageSize, setPageSize] = useState(10);

  const loadData = useCallback(
    async (page = pageNum, size = pageSize) => {
      setLoading(true);
      try {
        const values = form.getFieldsValue();
        const range = values.timeRange;
        const resp = await auditApi.page({
          pageNum: page,
          pageSize: size,
          traceId: values.traceId || undefined,
          level: values.level || undefined,
          keyword: values.keyword || undefined,
          from: range?.[0]?.format('YYYY-MM-DD HH:mm:ss'),
          to: range?.[1]?.format('YYYY-MM-DD HH:mm:ss'),
        });
        setData(resp.list);
        setTotal(resp.total);
        setPageNum(resp.pageNum);
        setPageSize(resp.pageSize);
        setApiError(false);
      } catch {
        setData([]);
        setTotal(0);
        setApiError(true);
      } finally {
        setLoading(false);
      }
    },
    [form, pageNum, pageSize],
  );

  useEffect(() => {
    void loadData(1, pageSize);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const columns: ColumnsType<AuditLogItem> = [
    { title: '时间', dataIndex: 'timestamp', width: 180 },
    { title: '级别', dataIndex: 'level', width: 88, render: (v: string) => levelTag(v) },
    { title: '服务', dataIndex: 'service', width: 140, ellipsis: true },
    {
      title: 'traceId',
      dataIndex: 'traceId',
      width: 200,
      ellipsis: true,
      render: (v?: string) => v || <Text type="secondary">-</Text>,
    },
    { title: 'Logger', dataIndex: 'logger', width: 220, ellipsis: true },
    { title: '消息', dataIndex: 'message', ellipsis: true },
    { title: '主机', dataIndex: 'host', width: 120, ellipsis: true },
  ];

  const onTableChange = (pagination: TablePaginationConfig) => {
    void loadData(pagination.current ?? 1, pagination.pageSize ?? 10);
  };

  return (
    <div>
      <Alert
        type="info"
        showIcon
        style={{ marginBottom: 16 }}
        message="审计日志数据来源"
        description={
          <span>
            本页查询 <code>GET /api/v1/auth/admin/audit/logs</code>，数据来自 Mongo 集中日志库{' '}
            <code>central_logs.app_logs</code>。需在服务端开启 <code>LOG_MONGO_ENABLED=true</code> 并配置{' '}
            <code>LOG_MONGO_URI</code>；Mongo TTL 默认约 <code>LOG_MONGO_TTL_DAYS</code> 天，与系统配置「审计保留天数」（
            {auditRetentionDays} 天）建议保持一致。配置/RBAC 变更可检索关键字「配置变更审计」「RBAC变更审计」。
          </span>
        }
      />

      {apiError && (
        <Alert
          type="warning"
          showIcon
          style={{ marginBottom: 16 }}
          message="查询失败"
          description="请确认后端已启动、Mongo 已开启且当前账号具备 auth:audit:read 权限。"
        />
      )}

      <Card style={{ marginBottom: 16 }}>
        <Form<SearchValues> form={form} layout="inline" onFinish={() => loadData(1, pageSize)}>
          <Form.Item name="traceId" label="traceId">
            <Input placeholder="链路 ID" allowClear style={{ width: 160 }} />
          </Form.Item>
          <Form.Item name="level" label="级别" initialValue="">
            <Select options={LEVEL_OPTIONS} style={{ width: 120 }} />
          </Form.Item>
          <Form.Item name="keyword" label="关键字">
            <Input placeholder="消息内容" allowClear style={{ width: 140 }} />
          </Form.Item>
          <Form.Item name="timeRange" label="时间">
            <RangePicker showTime />
          </Form.Item>
          <Form.Item>
            <Space>
              <Button type="primary" htmlType="submit" icon={<SearchOutlined />}>
                查询
              </Button>
              <Button
                icon={<ReloadOutlined />}
                onClick={() => {
                  form.resetFields();
                  void loadData(1, pageSize);
                }}
              >
                重置
              </Button>
            </Space>
          </Form.Item>
        </Form>
      </Card>

      <Card title="审计日志" extra={<Text type="secondary">共 {total} 条 · 保留约 {auditRetentionDays} 天</Text>}>
        <Table<AuditLogItem>
          rowKey={(r) => r.id ?? `${r.timestamp}-${r.traceId}-${r.message}`}
          loading={loading}
          columns={columns}
          dataSource={data}
          scroll={{ x: 1100 }}
          pagination={{
            current: pageNum,
            pageSize,
            total,
            showSizeChanger: true,
            showTotal: (t) => `共 ${t} 条`,
          }}
          onChange={onTableChange}
          locale={{ emptyText: apiError ? '查询失败' : '暂无日志（请确认 Mongo 集中日志已开启）' }}
        />
      </Card>
    </div>
  );
}
