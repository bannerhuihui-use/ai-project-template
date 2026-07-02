import { useCallback, useEffect, useState } from 'react';
import {
  Button,
  Card,
  Drawer,
  Form,
  Input,
  Modal,
  Select,
  Space,
  Table,
  Tag,
  Typography,
} from 'antd';
import type { ColumnsType, TablePaginationConfig } from 'antd/es/table';
import {
  EditOutlined,
  KeyOutlined,
  LogoutOutlined,
  MobileOutlined,
  PlusOutlined,
  ReloadOutlined,
  SearchOutlined,
  StopOutlined,
} from '@ant-design/icons';
import { userApi } from '@/api/userApi';
import { Permission } from '@/auth/Permission';
import { useSessionStore } from '@/store/sessionStore';
import type { LoginDeviceItem, RoleItem, UserAdminItem, UserCreateParams, UserUpdateParams } from '@/types/api';
import { renderDateTimeCell } from '@/utils/datetime';
import { emptyToUndefined, optionalEmailRule, optionalPhoneRule, buildPasswordRules, usernameRules } from '@/utils/formRules';
import { useAppConfigStore } from '@/store/appConfigStore';
import { feedback } from '@/utils/feedback';

const { Text } = Typography;

interface SearchFormValues {
  username?: string;
  status?: string;
  roleCode?: string;
}

interface UserFormValues {
  username?: string;
  password?: string;
  nickname?: string;
  email?: string;
  phone?: string;
  status?: string;
  roleCodes?: string[];
}

const STATUS_OPTIONS = [
  { label: '全部状态', value: '' },
  { label: '正常', value: 'NORMAL' },
  { label: '禁用', value: 'DISABLED' },
];

function statusTag(status: string) {
  return status === 'NORMAL' ? (
    <Tag color="success">正常</Tag>
  ) : (
    <Tag color="error">禁用</Tag>
  );
}

/** 用户管理页：列表 / 筛选 / 新建 / 编辑 / 禁用启用 / 重置密码 / 强制下线。 */
export default function UserManagePage() {
  const currentUserId = useSessionStore((s) => s.user?.userId);
  const passwordMinLength = useAppConfigStore((s) => s.config.passwordMinLength);
  const passwordRules = buildPasswordRules(passwordMinLength);
  const [searchForm] = Form.useForm<SearchFormValues>();
  const [userForm] = Form.useForm<UserFormValues>();

  const [loading, setLoading] = useState(false);
  const [data, setData] = useState<UserAdminItem[]>([]);
  const [total, setTotal] = useState(0);
  const [pageNum, setPageNum] = useState(1);
  const [pageSize, setPageSize] = useState(10);
  const [roles, setRoles] = useState<RoleItem[]>([]);

  const [drawerOpen, setDrawerOpen] = useState(false);
  const [drawerMode, setDrawerMode] = useState<'create' | 'edit'>('create');
  const [editingUser, setEditingUser] = useState<UserAdminItem | null>(null);
  const [submitting, setSubmitting] = useState(false);

  const [resetModalOpen, setResetModalOpen] = useState(false);
  const [resetTarget, setResetTarget] = useState<UserAdminItem | null>(null);
  const [resetPassword, setResetPassword] = useState('');

  const [deviceModalOpen, setDeviceModalOpen] = useState(false);
  const [deviceTarget, setDeviceTarget] = useState<UserAdminItem | null>(null);
  const [deviceLoading, setDeviceLoading] = useState(false);
  const [devices, setDevices] = useState<LoginDeviceItem[]>([]);

  const loadRoles = useCallback(async () => {
    try {
      const list = await userApi.listRoles();
      setRoles(list);
    } catch {
      // 角色下拉失败不阻塞列表
    }
  }, []);

  const loadData = useCallback(
    async (page = pageNum, size = pageSize) => {
      setLoading(true);
      try {
        const values = searchForm.getFieldsValue();
        const resp = await userApi.page({
          pageNum: page,
          pageSize: size,
          username: values.username || undefined,
          status: values.status || undefined,
          roleCode: values.roleCode || undefined,
        });
        setData(resp.list);
        setTotal(resp.total);
        setPageNum(resp.pageNum);
        setPageSize(resp.pageSize);
      } finally {
        setLoading(false);
      }
    },
    [pageNum, pageSize, searchForm],
  );

  useEffect(() => {
    void loadRoles();
    void loadData(1, pageSize);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const onSearch = () => {
    void loadData(1, pageSize);
  };

  const onResetSearch = () => {
    searchForm.resetFields();
    void loadData(1, pageSize);
  };

  const openCreate = () => {
    setDrawerMode('create');
    setEditingUser(null);
    userForm.resetFields();
    userForm.setFieldsValue({ status: 'NORMAL', roleCodes: [] });
    setDrawerOpen(true);
  };

  const openEdit = async (record: UserAdminItem) => {
    setDrawerMode('edit');
    setEditingUser(record);
    userForm.setFieldsValue({
      nickname: record.nickname,
      email: record.email,
      phone: record.phone,
      status: record.status,
      roleCodes: record.roles ?? [],
    });
    setDrawerOpen(true);
  };

  const onDrawerSubmit = async () => {
    const values = await userForm.validateFields();
    setSubmitting(true);
    try {
      if (drawerMode === 'create') {
        const payload: UserCreateParams = {
          username: values.username!.trim(),
          password: values.password!,
          nickname: emptyToUndefined(values.nickname),
          email: emptyToUndefined(values.email),
          phone: emptyToUndefined(values.phone),
          roleCodes: values.roleCodes,
        };
        await userApi.create(payload);
        feedback.success('用户创建成功');
      } else if (editingUser) {
        const payload: UserUpdateParams = {
          nickname: emptyToUndefined(values.nickname),
          email: emptyToUndefined(values.email),
          phone: emptyToUndefined(values.phone),
          status: values.status,
          roleCodes: values.roleCodes,
        };
        await userApi.update(editingUser.userId, payload);
        feedback.success('用户更新成功');
      }
      setDrawerOpen(false);
      void loadData(pageNum, pageSize);
    } finally {
      setSubmitting(false);
    }
  };

  const confirmDisable = (record: UserAdminItem) => {
    Modal.confirm({
      title: '确认禁用该用户？',
      content: `禁用后「${record.username}」将无法登录，其所有令牌立即失效。`,
      okText: '确认禁用',
      okButtonProps: { danger: true },
      onOk: async () => {
        await userApi.disable(record.userId);
        feedback.success('已禁用');
        void loadData(pageNum, pageSize);
      },
    });
  };

  const onEnable = async (record: UserAdminItem) => {
    await userApi.enable(record.userId);
    feedback.success('已启用');
    void loadData(pageNum, pageSize);
  };

  const confirmForceLogout = (record: UserAdminItem) => {
    Modal.confirm({
      title: '确认强制下线？',
      content: `将使用户「${record.username}」的所有令牌立即失效，需重新登录。`,
      okText: '确认',
      onOk: async () => {
        await userApi.forceLogout(record.userId);
        feedback.success('已强制下线');
      },
    });
  };

  const openResetPassword = (record: UserAdminItem) => {
    setResetTarget(record);
    setResetPassword('');
    setResetModalOpen(true);
  };

  const openLoginDevices = async (record: UserAdminItem) => {
    setDeviceTarget(record);
    setDeviceModalOpen(true);
    setDeviceLoading(true);
    try {
      const list = await userApi.listLoginDevices(record.userId);
      setDevices(list);
    } catch {
      setDevices([]);
    } finally {
      setDeviceLoading(false);
    }
  };

  const onResetPasswordSubmit = async () => {
    if (!resetTarget) return;
    if (resetPassword.length < passwordMinLength) {
      feedback.warning(`新密码至少 ${passwordMinLength} 位`);
      return;
    }
    await userApi.resetPassword(resetTarget.userId, resetPassword);
    feedback.success('密码已重置');
    setResetModalOpen(false);
  };

  const columns: ColumnsType<UserAdminItem> = [
    { title: 'ID', dataIndex: 'userId', width: 72 },
    { title: '用户名', dataIndex: 'username', width: 120 },
    { title: '昵称', dataIndex: 'nickname', ellipsis: true },
    {
      title: '角色',
      dataIndex: 'roles',
      render: (roles: string[]) =>
        roles?.length ? roles.map((r) => <Tag key={r}>{r}</Tag>) : <Text type="secondary">-</Text>,
    },
    {
      title: '状态',
      dataIndex: 'status',
      width: 88,
      render: (s: string) => statusTag(s),
    },
    {
      title: '创建时间',
      dataIndex: 'createdAt',
      width: 120,
      render: (value?: string) => {
        const cell = renderDateTimeCell(value);
        if (!cell) return <Text type="secondary">-</Text>;
        return (
          <div style={{ lineHeight: 1.35, fontVariantNumeric: 'tabular-nums' }}>
            <div>{cell.date}</div>
            <Text type="secondary" style={{ fontSize: 12 }}>
              {cell.time}
            </Text>
          </div>
        );
      },
    },
    {
      title: '操作',
      key: 'actions',
      width: 340,
      fixed: 'right',
      render: (_, record) => {
        const isSelf = record.userId === currentUserId;
        return (
          <Space size="small" wrap>
            <Permission code="auth:user:update">
              <Button type="link" size="small" icon={<EditOutlined />} onClick={() => openEdit(record)}>
                编辑
              </Button>
            </Permission>
            <Permission code="auth:user:reset_password">
              <Button
                type="link"
                size="small"
                icon={<KeyOutlined />}
                disabled={isSelf}
                onClick={() => openResetPassword(record)}
              >
                重置密码
              </Button>
            </Permission>
            <Permission code="auth:user:disable">
              {record.status === 'NORMAL' ? (
                <Button
                  type="link"
                  size="small"
                  danger
                  icon={<StopOutlined />}
                  disabled={isSelf}
                  onClick={() => confirmDisable(record)}
                >
                  禁用
                </Button>
              ) : (
                <Permission code="auth:user:update">
                  <Button type="link" size="small" onClick={() => onEnable(record)}>
                    启用
                  </Button>
                </Permission>
              )}
            </Permission>
            <Permission code="auth:user:read">
              <Button
                type="link"
                size="small"
                icon={<MobileOutlined />}
                onClick={() => void openLoginDevices(record)}
              >
                登录设备
              </Button>
            </Permission>
            <Permission code="auth:user:force_logout">
              <Button
                type="link"
                size="small"
                icon={<LogoutOutlined />}
                disabled={isSelf}
                onClick={() => confirmForceLogout(record)}
              >
                强制下线
              </Button>
            </Permission>
          </Space>
        );
      },
    },
  ];

  const onTableChange = (pagination: TablePaginationConfig) => {
    const nextPage = pagination.current ?? 1;
    const nextSize = pagination.pageSize ?? 10;
    void loadData(nextPage, nextSize);
  };

  return (
    <div>
      <Card style={{ marginBottom: 16 }}>
        <Form<SearchFormValues> form={searchForm} layout="inline" onFinish={onSearch}>
          <Form.Item name="username" label="用户名">
            <Input placeholder="模糊搜索" allowClear style={{ width: 160 }} />
          </Form.Item>
          <Form.Item name="status" label="状态" initialValue="">
            <Select options={STATUS_OPTIONS} style={{ width: 120 }} />
          </Form.Item>
          <Form.Item name="roleCode" label="角色">
            <Select
              allowClear
              placeholder="全部角色"
              style={{ width: 140 }}
              options={roles.map((r) => ({ label: r.roleName, value: r.roleCode }))}
            />
          </Form.Item>
          <Form.Item>
            <Space>
              <Button type="primary" htmlType="submit" icon={<SearchOutlined />}>
                查询
              </Button>
              <Button icon={<ReloadOutlined />} onClick={onResetSearch}>
                重置
              </Button>
            </Space>
          </Form.Item>
        </Form>
      </Card>

      <Card
        title="用户管理"
        extra={
          <Space>
            <Text type="secondary">共 {total} 个用户</Text>
            <Permission code="auth:user:create">
              <Button type="primary" icon={<PlusOutlined />} onClick={openCreate}>
                新增用户
              </Button>
            </Permission>
          </Space>
        }
      >
        <Table<UserAdminItem>
          rowKey="userId"
          loading={loading}
          columns={columns}
          dataSource={data}
          scroll={{ x: 960 }}
          pagination={{
            current: pageNum,
            pageSize,
            total,
            showSizeChanger: true,
            showTotal: (t) => `共 ${t} 条`,
          }}
          onChange={onTableChange}
        />
      </Card>

      <Drawer
        title={drawerMode === 'create' ? '新增用户' : '编辑用户'}
        width={480}
        open={drawerOpen}
        onClose={() => setDrawerOpen(false)}
        destroyOnHidden
        footer={
          <Space style={{ float: 'right' }}>
            <Button onClick={() => setDrawerOpen(false)}>取消</Button>
            <Button type="primary" loading={submitting} onClick={() => void onDrawerSubmit()}>
              保存
            </Button>
          </Space>
        }
      >
        <Form<UserFormValues> form={userForm} layout="vertical" requiredMark>
          {drawerMode === 'create' && (
            <>
              <Form.Item label="用户名" name="username" rules={usernameRules}>
                <Input placeholder="登录账号（字母、数字、下划线）" />
              </Form.Item>
              <Form.Item label="初始密码" name="password" rules={passwordRules}>
                <Input.Password placeholder={`至少 ${passwordMinLength} 位`} />
              </Form.Item>
            </>
          )}
          <Form.Item label="昵称" name="nickname">
            <Input placeholder="显示名称" />
          </Form.Item>
          <Form.Item label="邮箱" name="email" rules={[optionalEmailRule]}>
            <Input placeholder="选填，如 name@example.com" allowClear />
          </Form.Item>
          <Form.Item label="手机号" name="phone" rules={[optionalPhoneRule]}>
            <Input placeholder="选填，11 位手机号" allowClear maxLength={11} />
          </Form.Item>
          {drawerMode === 'edit' && (
            <Form.Item label="账号状态" name="status">
              <Select
                options={[
                  { label: '正常', value: 'NORMAL' },
                  { label: '禁用', value: 'DISABLED' },
                ]}
              />
            </Form.Item>
          )}
          <Form.Item label="分配角色" name="roleCodes">
            <Select
              mode="multiple"
              placeholder="选择角色"
              options={roles.map((r) => ({ label: `${r.roleName} (${r.roleCode})`, value: r.roleCode }))}
            />
          </Form.Item>
        </Form>
      </Drawer>

      <Modal
        title={`重置密码：${resetTarget?.username ?? ''}`}
        open={resetModalOpen}
        onCancel={() => setResetModalOpen(false)}
        onOk={() => void onResetPasswordSubmit()}
        okText="确认重置"
      >
        <Input.Password
          placeholder={`新密码（至少 ${passwordMinLength} 位）`}
          value={resetPassword}
          onChange={(e) => setResetPassword(e.target.value)}
        />
      </Modal>

      <Modal
        title={`登录设备：${deviceTarget?.username ?? ''}`}
        open={deviceModalOpen}
        onCancel={() => setDeviceModalOpen(false)}
        footer={null}
        width={720}
      >
        <Table<LoginDeviceItem>
          rowKey="id"
          loading={deviceLoading}
          dataSource={devices}
          pagination={false}
          size="small"
          locale={{ emptyText: '暂无登录记录' }}
          columns={[
            { title: '平台', dataIndex: 'platform', width: 80 },
            { title: '设备', dataIndex: 'deviceName', ellipsis: true },
            { title: 'IP', dataIndex: 'ip', width: 120 },
            {
              title: '最近登录',
              dataIndex: 'lastLoginTime',
              width: 168,
              render: (value?: string) => {
                const cell = renderDateTimeCell(value);
                if (!cell) return <Text type="secondary">-</Text>;
                return (
                  <span style={{ fontVariantNumeric: 'tabular-nums' }}>
                    {cell.date} {cell.time}
                  </span>
                );
              },
            },
          ]}
        />
      </Modal>
    </div>
  );
}
