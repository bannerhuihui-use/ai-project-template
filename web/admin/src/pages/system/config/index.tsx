import { useCallback, useEffect, useMemo, useState } from 'react';
import {
  Alert,
  App,
  Button,
  Card,
  Form,
  Input,
  InputNumber,
  Modal,
  Popconfirm,
  Select,
  Space,
  Spin,
  Switch,
  Tabs,
  Tag,
  Tooltip,
  Typography,
} from 'antd';
import { DeleteOutlined, EditOutlined, PlusOutlined, ReloadOutlined, SaveOutlined } from '@ant-design/icons';
import { configApi } from '@/api/configApi';
import { Permission } from '@/auth/Permission';
import { useAppConfigStore } from '@/store/appConfigStore';
import type { ConfigItem } from '@/types/api';
import { feedback } from '@/utils/feedback';
import { FileUpload } from '@/components/FileUpload';
import { builtinNumberRange, builtinValueHint, builtinValueRules, jsonValueRules, JSON_VALUE_HINT } from '@/utils/configConstraints';

const { Text, Paragraph } = Typography;
const { TextArea } = Input;

const GROUP_LABELS: Record<string, string> = {
  site: '站点',
  auth: '认证与安全',
  system: '系统',
  custom: '自定义',
  default: '其他',
};

const GROUP_OPTIONS = [
  { label: '自定义 (custom)', value: 'custom' },
  { label: '站点 (site)', value: 'site' },
  { label: '认证与安全 (auth)', value: 'auth' },
  { label: '系统 (system)', value: 'system' },
];

const VALUE_TYPE_OPTIONS = [
  { label: '字符串 STRING', value: 'STRING' },
  { label: '数字 NUMBER', value: 'NUMBER' },
  { label: '布尔 BOOLEAN', value: 'BOOLEAN' },
  { label: 'JSON', value: 'JSON' },
];

const CONFIG_KEY_PATTERN = /^[a-z][a-z0-9._]{1,127}$/;

interface CreateFormValues {
  configKey: string;
  configName: string;
  configValue?: string;
  configGroup: string;
  valueType: string;
  description?: string;
  sortNo?: number;
}

interface MetaFormValues {
  configName: string;
  configGroup: string;
  description?: string;
  sortNo?: number;
}

function groupLabel(group: string) {
  return GROUP_LABELS[group] ?? group;
}

function normalizeFieldValue(value: unknown): string {
  if (value == null) return '';
  return String(value);
}

/** 按配置类型归一化后再比较，避免 NUMBER 的 8 与 "8" 误判为脏。 */
function normalizeConfigForCompare(value: unknown, valueType: string): string {
  if (value == null || value === '') return '';
  switch (valueType) {
    case 'BOOLEAN':
      return value === true || value === 'true' ? 'true' : 'false';
    case 'NUMBER': {
      const num = typeof value === 'number' ? value : Number(String(value).trim());
      return Number.isFinite(num) ? String(num) : '';
    }
    default:
      return String(value).trim();
  }
}

/** 系统配置页：按分组维护键值参数。 */
export default function SystemConfigPage() {
  const { modal } = App.useApp();
  const [form] = Form.useForm<Record<string, unknown>>();
  const [loading, setLoading] = useState(false);
  const [saving, setSaving] = useState(false);
  const [configs, setConfigs] = useState<ConfigItem[]>([]);
  const [activeGroup, setActiveGroup] = useState<string>('');
  const [createOpen, setCreateOpen] = useState(false);
  const [creating, setCreating] = useState(false);
  const [createForm] = Form.useForm<CreateFormValues>();
  const createValueType = Form.useWatch('valueType', createForm);
  const [metaOpen, setMetaOpen] = useState(false);
  const [metaSaving, setMetaSaving] = useState(false);
  const [metaTarget, setMetaTarget] = useState<ConfigItem | null>(null);
  const [metaForm] = Form.useForm<MetaFormValues>();
  const [baseline, setBaseline] = useState<Record<string, string>>({});
  const [dirtyKeys, setDirtyKeys] = useState<Set<string>>(new Set());

  const recomputeDirty = useCallback(() => {
    const current = form.getFieldsValue(true);
    const editable = configs.filter((c) => c.editable === 1);
    const dirty = new Set<string>();
    editable.forEach((c) => {
      const key = c.configKey;
      // 未激活 Tab 内的字段可能尚未挂载，undefined 不应视为「已修改」
      if (current[key] === undefined) {
        return;
      }
      const currentNorm = normalizeConfigForCompare(current[key], c.valueType);
      const baselineNorm = normalizeConfigForCompare(baseline[key] ?? '', c.valueType);
      if (currentNorm !== baselineNorm) {
        dirty.add(key);
      }
    });
    setDirtyKeys(dirty);
  }, [baseline, configs, form]);

  const grouped = useMemo(() => {
    const map = new Map<string, ConfigItem[]>();
    configs.forEach((c) => {
      const g = c.configGroup || 'default';
      if (!map.has(g)) map.set(g, []);
      map.get(g)!.push(c);
    });
    return Array.from(map.entries()).sort(([a], [b]) => a.localeCompare(b));
  }, [configs]);

  const loadData = useCallback(async () => {
    setLoading(true);
    try {
      const list = await configApi.list();
      setConfigs(list);
      const values: Record<string, string | number | boolean> = {};
      const baselineValues: Record<string, string> = {};
      list.forEach((c) => {
        baselineValues[c.configKey] = c.configValue ?? '';
        if (c.valueType === 'NUMBER') {
          const raw = c.configValue?.trim();
          const num = raw ? Number(raw) : undefined;
          values[c.configKey] = num != null && Number.isFinite(num) ? num : raw ?? '';
        } else {
          values[c.configKey] = c.configValue ?? '';
        }
      });
      form.setFieldsValue(values);
      setBaseline(baselineValues);
      setDirtyKeys(new Set());
      if (list.length > 0 && !activeGroup) {
        setActiveGroup(list[0].configGroup || 'default');
      }
    } finally {
      setLoading(false);
    }
  }, [activeGroup, form]);

  useEffect(() => {
    void loadData();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  useEffect(() => {
    if (grouped.length > 0 && !grouped.some(([g]) => g === activeGroup)) {
      setActiveGroup(grouped[0][0]);
    }
  }, [grouped, activeGroup]);

  useEffect(() => {
    const onBeforeUnload = (e: BeforeUnloadEvent) => {
      if (dirtyKeys.size > 0) {
        e.preventDefault();
      }
    };
    window.addEventListener('beforeunload', onBeforeUnload);
    return () => window.removeEventListener('beforeunload', onBeforeUnload);
  }, [dirtyKeys]);

  const handleRefresh = () => {
    if (dirtyKeys.size === 0) {
      void loadData();
      return;
    }
    modal.confirm({
      title: '有未保存的修改',
      content: `当前有 ${dirtyKeys.size} 项配置未保存，刷新将放弃这些修改。`,
      okText: '放弃并刷新',
      cancelText: '取消',
      onOk: () => loadData(),
    });
  };

  const renderField = (item: ConfigItem) => {
    const disabled = item.editable !== 1;
    const name = item.configKey;
    const hint = builtinValueHint(item.configKey);
    const rules = builtinValueRules(item);
    const extra = [item.description, hint].filter(Boolean).join(' · ') || undefined;

    if (item.valueType === 'BOOLEAN') {
      return (
        <Form.Item
          key={name}
          name={name}
          label="值"
          valuePropName="checked"
          getValueFromEvent={(checked: boolean) => (checked ? 'true' : 'false')}
          getValueProps={(value?: string) => ({ checked: value === 'true' })}
          style={{ marginBottom: 8 }}
        >
          <Switch disabled={disabled} checkedChildren="开" unCheckedChildren="关" />
        </Form.Item>
      );
    }

    if (item.valueType === 'NUMBER') {
      const numRange = builtinNumberRange(item.configKey);
      return (
        <Form.Item key={name} name={name} label="值" extra={extra} rules={rules} style={{ marginBottom: 8 }}>
          <InputNumber
            disabled={disabled}
            style={{ width: '100%' }}
            placeholder={item.configKey}
            min={numRange?.min}
            max={numRange?.max}
          />
        </Form.Item>
      );
    }

    if (item.valueType === 'JSON') {
      return (
        <Form.Item
          key={name}
          name={name}
          label="值"
          extra={[item.description, JSON_VALUE_HINT].filter(Boolean).join(' · ') || undefined}
          rules={jsonValueRules()}
          style={{ marginBottom: 8 }}
        >
          <TextArea disabled={disabled} rows={4} placeholder="{}" />
        </Form.Item>
      );
    }

    if (item.configKey === 'site.logo') {
      return (
        <Form.Item key={name} name={name} label="值" extra={extra} rules={rules} style={{ marginBottom: 8 }}>
          <Space direction="vertical" style={{ width: '100%' }} size={8}>
            <Input disabled={disabled} placeholder="https://..." />
            {!disabled && (
              <Permission code="file:upload">
                <FileUpload
                  bizType="image"
                  accessLevel="public"
                  maxSizeMb={5}
                  buttonText="上传 Logo"
                  onUploaded={(url) => {
                    form.setFieldValue(name, url);
                    void form.validateFields([name]).catch(() => undefined);
                    recomputeDirty();
                  }}
                />
              </Permission>
            )}
          </Space>
        </Form.Item>
      );
    }

    return (
      <Form.Item key={name} name={name} label="值" extra={extra} rules={rules} style={{ marginBottom: 8 }}>
        <Input disabled={disabled} placeholder={item.configKey} />
      </Form.Item>
    );
  };

  const onSave = async () => {
    if (dirtyKeys.size === 0) {
      feedback.warning('没有需要保存的修改');
      return;
    }

    const fieldsToValidate = [...dirtyKeys];
    try {
      await form.validateFields(fieldsToValidate);
    } catch {
      feedback.error('请修正当前修改项的格式后再保存');
      return;
    }

    const values = form.getFieldsValue();
    const editableKeys = new Set(configs.filter((c) => c.editable === 1).map((c) => c.configKey));
    const items = Object.entries(values)
      .filter(([key]) => editableKeys.has(key) && dirtyKeys.has(key))
      .map(([configKey, configValue]) => ({
        configKey,
        configValue: normalizeFieldValue(configValue),
      }));

    setSaving(true);
    try {
      await configApi.batchUpdate({ items });
      feedback.success(`已保存 ${items.length} 项配置`);
      await useAppConfigStore.getState().refresh();
      void loadData();
    } finally {
      setSaving(false);
    }
  };

  const openCreateModal = () => {
    setCreateOpen(true);
  };

  const initCreateForm = () => {
    createForm.resetFields();
    createForm.setFieldsValue({
      configGroup: 'custom',
      valueType: 'STRING',
      sortNo: 100,
      configValue: '',
    });
  };

  const initMetaForm = (item: ConfigItem) => {
    metaForm.setFieldsValue({
      configName: item.configName,
      configGroup: item.configGroup,
      description: item.description ?? '',
      sortNo: item.sortNo ?? 0,
    });
  };

  const onCreateSubmit = async () => {
    const values = await createForm.validateFields();
    setCreating(true);
    try {
      await configApi.create({
        configKey: values.configKey.trim(),
        configName: values.configName.trim(),
        configValue: values.configValue == null ? '' : String(values.configValue),
        configGroup: values.configGroup,
        valueType: values.valueType,
        description: values.description?.trim() || undefined,
        sortNo: values.sortNo,
      });
      feedback.success('配置项已创建');
      setCreateOpen(false);
      setActiveGroup(values.configGroup);
      await useAppConfigStore.getState().refresh();
      await loadData();
    } finally {
      setCreating(false);
    }
  };

  const renderCreateValueField = () => {
    if (createValueType === 'BOOLEAN') {
      return (
        <Form.Item
          label="初始值"
          name="configValue"
          valuePropName="checked"
          getValueFromEvent={(checked: boolean) => (checked ? 'true' : 'false')}
          getValueProps={(value?: string) => ({ checked: value === 'true' })}
        >
          <Switch checkedChildren="true" unCheckedChildren="false" />
        </Form.Item>
      );
    }
    if (createValueType === 'NUMBER') {
      return (
        <Form.Item label="初始值" name="configValue">
          <InputNumber style={{ width: '100%' }} placeholder="0" />
        </Form.Item>
      );
    }
    if (createValueType === 'JSON') {
      return (
        <Form.Item label="初始值" name="configValue" extra={JSON_VALUE_HINT} rules={jsonValueRules()}>
          <TextArea rows={4} placeholder="{}" />
        </Form.Item>
      );
    }
    return (
      <Form.Item label="初始值" name="configValue">
        <Input placeholder="可为空" />
      </Form.Item>
    );
  };

  const openMetaModal = (item: ConfigItem) => {
    setMetaTarget(item);
    setMetaOpen(true);
  };

  const onMetaSubmit = async () => {
    if (!metaTarget) return;
    const values = await metaForm.validateFields();
    setMetaSaving(true);
    try {
      await configApi.updateMetadata(metaTarget.id, {
        configName: values.configName.trim(),
        configGroup: values.configGroup,
        description: values.description?.trim() || undefined,
        sortNo: values.sortNo,
      });
      feedback.success('配置信息已更新');
      setMetaOpen(false);
      setActiveGroup(values.configGroup);
      await loadData();
    } finally {
      setMetaSaving(false);
    }
  };

  const onDelete = async (item: ConfigItem) => {
    await configApi.remove(item.id);
    feedback.success('配置项已删除');
    await useAppConfigStore.getState().refresh();
    await loadData();
  };

  const renderItemHeader = (item: ConfigItem) => (
    <div style={{ marginBottom: 8 }}>
      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
        <Space size={8} wrap>
          <Text strong>{item.configName}</Text>
          {item.builtin && <Tag color="blue">内置</Tag>}
          {item.wired ? (
            <Tooltip title={item.wiredScope || '改值后即时或刷新后生效'}>
              <Tag color="success">已接入</Tag>
            </Tooltip>
          ) : (
            <Tooltip title="仅存储，需在代码中通过 SystemConfigService 读取后才会生效">
              <Tag color="default">仅存储</Tag>
            </Tooltip>
          )}
          {dirtyKeys.has(item.configKey) && <Tag color="orange">已修改</Tag>}
          {item.editable !== 1 && <Tag color="default">只读</Tag>}
        </Space>
        <Space size={4}>
          <Permission code="auth:config:update">
            <Button
              type="link"
              size="small"
              icon={<EditOutlined />}
              onClick={() => openMetaModal(item)}
              disabled={item.editable !== 1}
            >
              编辑信息
            </Button>
          </Permission>
          {item.deletable && (
            <Permission code="auth:config:delete">
              <Popconfirm
                title="确认删除该配置项？"
                description={`将删除「${item.configKey}」，此操作不可恢复。`}
                onConfirm={() => void onDelete(item)}
                okText="删除"
                okButtonProps={{ danger: true }}
              >
                <Button type="link" size="small" danger icon={<DeleteOutlined />}>
                  删除
                </Button>
              </Popconfirm>
            </Permission>
          )}
        </Space>
      </div>
      {item.wiredScope && (
        <Paragraph type="secondary" style={{ margin: '4px 0 0', fontSize: 12 }}>
          生效范围：{item.wiredScope}
        </Paragraph>
      )}
      {item.description && (
        <Paragraph type="secondary" style={{ margin: '4px 0 0', fontSize: 12 }}>
          {item.description}
        </Paragraph>
      )}
    </div>
  );

  const tabItems = grouped.map(([group, items]) => ({
    key: group,
    label: groupLabel(group),
    forceRender: true,
    children: (
      <div style={{ maxWidth: 640 }}>
        {items.map((item) => (
          <div key={item.configKey}>
            {renderItemHeader(item)}
            {renderField(item)}
            <Paragraph type="secondary" style={{ marginTop: -12, marginBottom: 16, fontSize: 12 }}>
              <Text code>{item.configKey}</Text>
              <Text type="secondary"> · {item.valueType}</Text>
            </Paragraph>
          </div>
        ))}
      </div>
    ),
  }));

  return (
    <div>
      <Alert
        type="info"
        showIcon
        style={{ marginBottom: 16 }}
        message="系统参数配置"
        description="保存时仅提交有改动的配置项。变更会写入审计日志，可在「审计日志」页按关键字「配置变更审计」检索。"
      />

      <Card
        title="系统配置"
        extra={
          <Space>
            {dirtyKeys.size > 0 && (
              <Tag color="warning">{dirtyKeys.size} 项未保存</Tag>
            )}
            <Permission code="auth:config:create">
              <Button icon={<PlusOutlined />} onClick={openCreateModal}>
                新建配置
              </Button>
            </Permission>
            <Button icon={<ReloadOutlined />} onClick={handleRefresh}>
              刷新
            </Button>
            <Permission code="auth:config:update">
              <Button
                type="primary"
                icon={<SaveOutlined />}
                loading={saving}
                disabled={dirtyKeys.size === 0}
                onClick={() => void onSave()}
              >
                保存{dirtyKeys.size > 0 ? ` (${dirtyKeys.size})` : ''}
              </Button>
            </Permission>
          </Space>
        }
      >
        <Spin spinning={loading}>
          {tabItems.length === 0 ? (
            <Text type="secondary">暂无配置项，请确认后端 Flyway 已执行（V5 种子）或手动执行 docs/DB/template-full.sql。</Text>
          ) : (
            <Form form={form} layout="vertical" onValuesChange={recomputeDirty}>
              <Tabs activeKey={activeGroup} items={tabItems} onChange={setActiveGroup} destroyOnHidden={false} />
            </Form>
          )}
        </Spin>
      </Card>

      <Modal
        title="新建配置项"
        open={createOpen}
        onCancel={() => setCreateOpen(false)}
        onOk={() => void onCreateSubmit()}
        confirmLoading={creating}
        okText="创建"
        destroyOnHidden
        afterOpenChange={(open) => {
          if (open) initCreateForm();
        }}
        width={560}
      >
        <Form<CreateFormValues> form={createForm} layout="vertical" style={{ marginTop: 16 }}>
          <Form.Item
            label="配置键"
            name="configKey"
            rules={[
              { required: true, message: '请输入配置键' },
              {
                pattern: CONFIG_KEY_PATTERN,
                message: '小写字母开头，仅支持小写、数字、点、下划线',
              },
            ]}
            extra="全局唯一，建议点分命名，如 custom.notify.enabled"
          >
            <Input placeholder="custom.feature.enabled" />
          </Form.Item>
          <Form.Item
            label="配置名称"
            name="configName"
            rules={[{ required: true, message: '请输入配置名称' }]}
          >
            <Input placeholder="功能开关" />
          </Form.Item>
          <Form.Item
            label="分组"
            name="configGroup"
            rules={[{ required: true, message: '请选择分组' }]}
          >
            <Select options={GROUP_OPTIONS} />
          </Form.Item>
          <Form.Item
            label="值类型"
            name="valueType"
            rules={[{ required: true, message: '请选择值类型' }]}
          >
            <Select options={VALUE_TYPE_OPTIONS} />
          </Form.Item>
          {renderCreateValueField()}
          <Form.Item label="说明" name="description">
            <TextArea rows={2} placeholder="可选，描述该配置的用途" />
          </Form.Item>
          <Form.Item label="排序号" name="sortNo" extra="越小越靠前，默认可填 100">
            <InputNumber style={{ width: '100%' }} min={0} />
          </Form.Item>
        </Form>
      </Modal>

      <Modal
        title={`编辑配置信息：${metaTarget?.configKey ?? ''}`}
        open={metaOpen}
        onCancel={() => setMetaOpen(false)}
        onOk={() => void onMetaSubmit()}
        confirmLoading={metaSaving}
        okText="保存"
        destroyOnHidden
        afterOpenChange={(open) => {
          if (open && metaTarget) initMetaForm(metaTarget);
        }}
        width={520}
      >
        <Form<MetaFormValues> form={metaForm} layout="vertical" style={{ marginTop: 16 }}>
          <Form.Item label="配置键">
            <Input value={metaTarget?.configKey} disabled />
          </Form.Item>
          <Form.Item label="值类型">
            <Input value={metaTarget?.valueType} disabled />
          </Form.Item>
          <Form.Item
            label="配置名称"
            name="configName"
            rules={[{ required: true, message: '请输入配置名称' }]}
          >
            <Input />
          </Form.Item>
          <Form.Item
            label="分组"
            name="configGroup"
            rules={[{ required: true, message: '请选择分组' }]}
          >
            <Select options={GROUP_OPTIONS} />
          </Form.Item>
          <Form.Item label="说明" name="description">
            <TextArea rows={2} />
          </Form.Item>
          <Form.Item label="排序号" name="sortNo">
            <InputNumber style={{ width: '100%' }} min={0} />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
}
