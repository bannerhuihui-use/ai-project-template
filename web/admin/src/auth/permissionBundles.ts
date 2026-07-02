/**
 * 功能模块 ↔ 权限码映射。
 * 角色授权页按「功能」展示，勾选模块时自动关联菜单码与接口码，避免 MENU/API 分离导致漏配。
 */

export interface PermissionOperation {
  code: string;
  label: string;
}

export interface PermissionBundle {
  key: string;
  title: string;
  description: string;
  /** 对应 MENU 权限码 */
  menuCode: string;
  /** 该功能下的接口权限 */
  operations: PermissionOperation[];
}

/** 系统管理目录（子菜单勾选时自动带上） */
export const SYSTEM_ROOT_MENU = 'menu:system:root';

/** 不在功能模块中展示、也不建议分配给业务角色的系统级权限 */
export const HIDDEN_PERMISSION_CODES = new Set(['auth:rbac:ping']);

/** 按业务功能划分的权限包（与种子数据 permission_code 对齐） */
export const PERMISSION_BUNDLES: PermissionBundle[] = [
  {
    key: 'user',
    title: '用户管理',
    description: '用户列表、新建、编辑、禁用、重置密码、分配角色',
    menuCode: 'menu:auth:user',
    operations: [
      { code: 'auth:user:read', label: '查看' },
      { code: 'auth:user:create', label: '新建' },
      { code: 'auth:user:update', label: '编辑' },
      { code: 'auth:user:reset_password', label: '重置密码' },
      { code: 'auth:user:disable', label: '禁用/启用' },
      { code: 'auth:user:force_logout', label: '强制下线' },
      { code: 'auth:user:grant_role', label: '分配角色' },
    ],
  },
  {
    key: 'role',
    title: '角色权限',
    description: '查看权限点、配置角色权限、新建角色',
    menuCode: 'menu:auth:role',
    operations: [
      { code: 'auth:permission:read', label: '查看权限点' },
      { code: 'auth:role:read', label: '查看角色权限' },
      { code: 'auth:role:grant_permission', label: '保存角色权限' },
      { code: 'auth:role:create', label: '新建角色' },
    ],
  },
  {
    key: 'config',
    title: '系统配置',
    description: '站点与系统参数维护',
    menuCode: 'menu:system:config',
    operations: [
      { code: 'auth:config:read', label: '查看' },
      { code: 'auth:config:create', label: '新建配置' },
      { code: 'auth:config:update', label: '编辑保存' },
      { code: 'auth:config:delete', label: '删除配置' },
    ],
  },
  {
    key: 'audit',
    title: '审计日志',
    description: '集中日志查询',
    menuCode: 'menu:system:audit',
    operations: [{ code: 'auth:audit:read', label: '查看' }],
  },
];

/** 所有功能包内的权限码（含菜单） */
export function allBundledCodes(): Set<string> {
  const codes = new Set<string>();
  PERMISSION_BUNDLES.forEach((b) => {
    codes.add(b.menuCode);
    b.operations.forEach((op) => codes.add(op.code));
  });
  return codes;
}

/** 根据已选权限码展开：补全系统管理目录菜单 */
export function expandPermissionCodes(checked: Iterable<string>): string[] {
  const set = new Set(checked);
  const hasSystemChild = PERMISSION_BUNDLES.some(
    (b) => b.menuCode.startsWith('menu:system:') && set.has(b.menuCode),
  );
  if (hasSystemChild) {
    set.add(SYSTEM_ROOT_MENU);
  }
  return Array.from(set);
}

/** 判断功能包是否「已启用」（勾选了菜单或任一接口） */
export function isBundleEnabled(bundle: PermissionBundle, checked: Set<string>): boolean {
  if (checked.has(bundle.menuCode)) return true;
  return bundle.operations.some((op) => checked.has(op.code));
}

/** 功能包是否全选 */
export function isBundleFullyChecked(bundle: PermissionBundle, checked: Set<string>): boolean {
  if (!checked.has(bundle.menuCode)) return false;
  return bundle.operations.every((op) => checked.has(op.code));
}

/** 根据子项勾选情况同步模块菜单码：无子项选中时移除菜单，有子项时带上菜单。 */
export function syncBundleMenuCode(bundle: PermissionBundle, checked: Set<string>): void {
  const anyOpChecked = bundle.operations.some((op) => checked.has(op.code));
  if (anyOpChecked) {
    checked.add(bundle.menuCode);
  } else {
    checked.delete(bundle.menuCode);
  }
}

/** 功能包是否部分选中 */
export function isBundleIndeterminate(bundle: PermissionBundle, checked: Set<string>): boolean {
  const opCheckedCount = bundle.operations.filter((op) => checked.has(op.code)).length;
  if (opCheckedCount === 0) return false;
  return !isBundleFullyChecked(bundle, checked);
}
