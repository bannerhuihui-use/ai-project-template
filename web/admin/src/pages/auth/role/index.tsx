import { useCallback, useEffect, useMemo, useState } from 'react';
import {
  Alert,
  Button,
  Card,
  Checkbox,
  Col,
  Collapse,
  Empty,
  Form,
  Input,
  List,
  Modal,
  Row,
  Space,
  Spin,
  Tag,
  Typography,
} from 'antd';
import { PlusOutlined, ReloadOutlined, SaveOutlined } from '@ant-design/icons';
import {
  HIDDEN_PERMISSION_CODES,
  PERMISSION_BUNDLES,
  allBundledCodes,
  expandPermissionCodes,
  isBundleEnabled,
  isBundleFullyChecked,
  isBundleIndeterminate,
  syncBundleMenuCode,
  type PermissionBundle,
} from '@/auth/permissionBundles';
import { rbacApi } from '@/api/rbacApi';
import { userApi } from '@/api/userApi';
import { Permission } from '@/auth/Permission';
import type { RoleItem } from '@/types/api';
import { feedback } from '@/utils/feedback';

const { Text, Paragraph } = Typography;

interface RoleFormValues {
  roleCode: string;
  roleName: string;
}

const SUPER_ADMIN = 'SUPER_ADMIN';

/** 角色权限配置：按功能模块授权，自动关联菜单与接口权限。 */
export default function RolePermissionPage() {
  const [roles, setRoles] = useState<RoleItem[]>([]);
  const [selectedRoleId, setSelectedRoleId] = useState<number | null>(null);
  const [checkedCodes, setCheckedCodes] = useState<Set<string>>(new Set());
  const [loading, setLoading] = useState(false);
  const [saving, setSaving] = useState(false);
  const [createOpen, setCreateOpen] = useState(false);
  const [creating, setCreating] = useState(false);
  const [roleForm] = Form.useForm<RoleFormValues>();

  const selectedRole = roles.find((r) => r.roleId === selectedRoleId) ?? null;
  const isSuperAdmin = selectedRole?.roleCode === SUPER_ADMIN;
  const bundledSet = useMemo(() => allBundledCodes(), []);

  const extraCodes = useMemo(
    () => [...checkedCodes].filter((c) => !bundledSet.has(c) && !HIDDEN_PERMISSION_CODES.has(c)),
    [checkedCodes, bundledSet],
  );

  const enabledBundleCount = useMemo(
    () => PERMISSION_BUNDLES.filter((b) => isBundleEnabled(b, checkedCodes)).length,
    [checkedCodes],
  );

  const loadRoles = useCallback(async () => {
    const list = await userApi.listRoles();
    setRoles(list);
    if (list.length > 0 && selectedRoleId == null) {
      setSelectedRoleId(list[0].roleId);
    }
  }, [selectedRoleId]);

  const loadRolePermissions = useCallback(async (roleId: number) => {
    setLoading(true);
    try {
      const bound = await rbacApi.getRolePermissions(roleId);
      setCheckedCodes(new Set(bound.map((p) => p.permissionCode)));
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    void loadRoles();
  }, [loadRoles]);

  useEffect(() => {
    if (selectedRoleId != null) {
      void loadRolePermissions(selectedRoleId);
    }
  }, [selectedRoleId, loadRolePermissions]);

  const updateChecked = (updater: (prev: Set<string>) => Set<string>) => {
    setCheckedCodes((prev) => updater(new Set(prev)));
  };

  const setBundleChecked = (bundle: PermissionBundle, checked: boolean) => {
    updateChecked((prev) => {
      const next = new Set(prev);
      if (checked) {
        next.add(bundle.menuCode);
        bundle.operations.forEach((op) => next.add(op.code));
      } else {
        next.delete(bundle.menuCode);
        bundle.operations.forEach((op) => next.delete(op.code));
      }
      return next;
    });
  };

  const toggleOperation = (code: string, checked: boolean) => {
    updateChecked((prev) => {
      const next = new Set(prev);
      const bundle = PERMISSION_BUNDLES.find((b) => b.operations.some((op) => op.code === code));
      if (checked) next.add(code);
      else next.delete(code);
      if (bundle) syncBundleMenuCode(bundle, next);
      return next;
    });
  };

  const onSave = async () => {
    if (!selectedRoleId || isSuperAdmin) return;
    setSaving(true);
    try {
      const payload = expandPermissionCodes(checkedCodes);
      await rbacApi.replaceRolePermissions(selectedRoleId, payload);
      feedback.success('角色权限已保存，该角色下用户需重新登录后生效');
      void loadRolePermissions(selectedRoleId);
    } finally {
      setSaving(false);
    }
  };

  const onCreateRole = async () => {
    const values = await roleForm.validateFields();
    setCreating(true);
    try {
      const created = await rbacApi.createRole({
        roleCode: values.roleCode.trim().toUpperCase(),
        roleName: values.roleName.trim(),
      });
      feedback.success('角色创建成功，请勾选功能模块并保存');
      setCreateOpen(false);
      roleForm.resetFields();
      await loadRoles();
      setSelectedRoleId(created.roleId);
    } finally {
      setCreating(false);
    }
  };

  const collapseItems = useMemo(
    () =>
      PERMISSION_BUNDLES.map((bundle) => {
        const full = isBundleFullyChecked(bundle, checkedCodes);
        const indeterminate = isBundleIndeterminate(bundle, checkedCodes);
        return {
          key: bundle.key,
          label: (
            <Checkbox
              indeterminate={indeterminate}
              checked={full}
              disabled={isSuperAdmin}
              onClick={(e) => e.stopPropagation()}
              onChange={(e) => setBundleChecked(bundle, e.target.checked)}
            >
              <Text strong>{bundle.title}</Text>
              <Text type="secondary" style={{ marginLeft: 8, fontSize: 12 }}>
                {bundle.description}
              </Text>
            </Checkbox>
          ),
          children: (
            <Space direction="vertical" size={8} style={{ width: '100%' }}>
              <Text type="secondary" style={{ fontSize: 12 }}>
                菜单：<Text code>{bundle.menuCode}</Text>
              </Text>
              <Row gutter={[8, 8]}>
                {bundle.operations.map((op) => (
                  <Col xs={24} sm={12} md={8} key={op.code}>
                    <Checkbox
                      checked={checkedCodes.has(op.code)}
                      disabled={isSuperAdmin}
                      onChange={(e) => toggleOperation(op.code, e.target.checked)}
                    >
                      {op.label}
                      <Text type="secondary" style={{ fontSize: 11, marginLeft: 4 }}>
                        ({op.code})
                      </Text>
                    </Checkbox>
                  </Col>
                ))}
              </Row>
            </Space>
          ),
        };
      }),
    // eslint-disable-next-line react-hooks/exhaustive-deps -- setBundleChecked/toggleOperation 稳定
    [checkedCodes, isSuperAdmin],
  );

  return (
    <>
      <Row gutter={16}>
        <Col xs={24} md={7} lg={6}>
          <Card
            title="角色列表"
            size="small"
            extra={
              <Permission code="auth:role:create">
                <Button type="link" size="small" icon={<PlusOutlined />} onClick={() => setCreateOpen(true)}>
                  新建
                </Button>
              </Permission>
            }
          >
            <List
              dataSource={roles}
              locale={{ emptyText: '暂无角色' }}
              renderItem={(role) => (
                <List.Item
                  style={{
                    cursor: 'pointer',
                    padding: '10px 12px',
                    borderRadius: 8,
                    background: role.roleId === selectedRoleId ? '#e8f0fe' : undefined,
                  }}
                  onClick={() => setSelectedRoleId(role.roleId)}
                >
                  <Space direction="vertical" size={0}>
                    <Text strong>{role.roleName}</Text>
                    <Text type="secondary" style={{ fontSize: 12 }}>
                      {role.roleCode}
                    </Text>
                  </Space>
                </List.Item>
              )}
            />
          </Card>
        </Col>

        <Col xs={24} md={17} lg={18}>
          <Card
            title={
              <Space>
                <span>功能授权</span>
                {selectedRole && <Tag color="blue">{selectedRole.roleName}</Tag>}
              </Space>
            }
            extra={
              <Space>
                <Button icon={<ReloadOutlined />} onClick={() => selectedRoleId && loadRolePermissions(selectedRoleId)}>
                  刷新
                </Button>
                <Permission code="auth:role:grant_permission">
                  <Button
                    type="primary"
                    icon={<SaveOutlined />}
                    loading={saving}
                    disabled={!selectedRoleId || isSuperAdmin}
                    onClick={() => void onSave()}
                  >
                    保存
                  </Button>
                </Permission>
              </Space>
            }
          >
            {isSuperAdmin && (
              <Alert
                type="info"
                showIcon
                message="SUPER_ADMIN 在代码层短路全部权限校验，无需配置。"
                style={{ marginBottom: 16 }}
              />
            )}

            {!isSuperAdmin && selectedRoleId && (
              <Alert
                type="info"
                showIcon
                style={{ marginBottom: 16 }}
                message="按功能模块授权"
                description="勾选功能会自动关联「菜单 + 接口」权限，避免只勾菜单却调不通接口。保存后该角色用户需重新登录。"
              />
            )}

            {!selectedRoleId ? (
              <Empty description="请选择左侧角色" />
            ) : (
              <Spin spinning={loading}>
                <Paragraph type="secondary" style={{ marginBottom: 12 }}>
                  已启用 {enabledBundleCount} / {PERMISSION_BUNDLES.length} 个功能模块，共 {checkedCodes.size} 项权限码
                </Paragraph>

                <Collapse
                  defaultActiveKey={PERMISSION_BUNDLES.map((b) => b.key)}
                  bordered={false}
                  style={{ background: 'transparent' }}
                  items={collapseItems}
                />

                {extraCodes.length > 0 && (
                  <Card size="small" title="其他权限" style={{ marginTop: 16 }}>
                    <Text type="secondary" style={{ fontSize: 12 }}>
                      以下权限不在功能模块中，保存时仍会保留：
                    </Text>
                    <div style={{ marginTop: 8 }}>
                      {extraCodes.map((code) => (
                        <Tag key={code} style={{ marginBottom: 4 }}>
                          {code}
                        </Tag>
                      ))}
                    </div>
                  </Card>
                )}
              </Spin>
            )}
          </Card>
        </Col>
      </Row>

      <Modal
        title="新建角色"
        open={createOpen}
        confirmLoading={creating}
        onOk={() => void onCreateRole()}
        onCancel={() => {
          setCreateOpen(false);
          roleForm.resetFields();
        }}
        destroyOnHidden
      >
        <Form<RoleFormValues> form={roleForm} layout="vertical" style={{ marginTop: 16 }}>
          <Form.Item
            name="roleCode"
            label="角色编码"
            rules={[
              { required: true, message: '请输入角色编码' },
              { pattern: /^[A-Za-z][A-Za-z0-9_]{1,63}$/, message: '字母开头，仅允许字母、数字、下划线' },
            ]}
            extra="保存时自动转为大写，如 OPERATOR"
          >
            <Input placeholder="OPERATOR" />
          </Form.Item>
          <Form.Item name="roleName" label="角色名称" rules={[{ required: true, message: '请输入角色名称' }]}>
            <Input placeholder="运营人员" />
          </Form.Item>
        </Form>
      </Modal>
    </>
  );
}
