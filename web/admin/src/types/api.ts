/** 后端统一响应包装 Result<T>，code===0 表示成功。 */
export interface Result<T> {
  code: number;
  message: string;
  data: T;
}

/** 分页结果 PageResult<T>。 */
export interface PageResult<T> {
  total: number;
  pageNum: number;
  pageSize: number;
  list: T[];
}

/** 登录用户基本信息（登录响应内嵌 user）。 */
export interface AuthUser {
  userId: number;
  userType: string;
  nickname?: string;
  roles: string[];
}

/** 登录 / 刷新 / 改密 统一返回的双令牌结构。 */
export interface LoginResponse {
  accessToken: string;
  refreshToken: string;
  tokenType: string;
  accessExpireSeconds: number;
  refreshExpireSeconds: number;
  user: AuthUser;
}

/** /me 返回的当前用户信息。 */
export interface AuthUserInfo {
  userId: number;
  userType: string;
  nickname?: string;
  roles: string[];
}

/** 菜单树节点，对应 GET /api/v1/auth/menus（契约见 docs/API/auth-v2.0-rbac.md）。 */
export interface MenuNode {
  permissionCode: string;
  permissionType: string;
  title: string;
  path?: string;
  name?: string;
  component?: string;
  redirect?: string;
  icon?: string;
  breadcrumb?: boolean;
  clickable?: boolean;
  alwaysShow?: boolean;
  visible?: boolean;
  isExternalLink?: boolean;
  externalLinkUrl?: string;
  children?: MenuNode[];
}

/** 业务错误：携带后端返回的 code，便于上层按码分支处理。 */
export class ApiError extends Error {
  code: number;

  constructor(code: number, message: string) {
    super(message);
    this.name = 'ApiError';
    this.code = code;
  }
}

/** 管理端用户列表项。 */
export interface UserAdminItem {
  userId: number;
  username: string;
  nickname?: string;
  email?: string;
  phone?: string;
  status: string;
  roles: string[];
  createdAt?: string;
  lastLoginTime?: string;
}

/** 用户登录设备记录。 */
export interface LoginDeviceItem {
  id: number;
  identityType?: string;
  deviceId?: string;
  deviceName?: string;
  platform?: string;
  ip?: string;
  lastLoginTime?: string;
}

/** 角色下拉项。 */
export interface RoleItem {
  roleId: number;
  roleCode: string;
  roleName: string;
}

/** 新建用户请求。 */
export interface UserCreateParams {
  username: string;
  password: string;
  nickname?: string;
  email?: string;
  phone?: string;
  roleCodes?: string[];
}

/** 更新用户请求。 */
export interface UserUpdateParams {
  nickname?: string;
  email?: string;
  phone?: string;
  status?: string;
  roleCodes?: string[];
}

/** 权限点列表项。 */
export interface PermissionItem {
  id: number;
  permissionCode: string;
  permissionName: string;
  permissionType: string;
  module?: string;
  status?: string;
}

/** 系统配置项。 */
export interface ConfigItem {
  id: number;
  configKey: string;
  configName: string;
  configValue?: string;
  configGroup: string;
  valueType: 'STRING' | 'NUMBER' | 'BOOLEAN' | 'JSON' | string;
  description?: string;
  editable: number;
  sortNo?: number;
  /** 是否为内置种子配置（不可删除） */
  builtin?: boolean;
  /** 是否允许删除 */
  deletable?: boolean;
  /** 是否已接入业务 */
  wired?: boolean;
  /** 已接入时的生效范围说明 */
  wiredScope?: string;
}

/** 新建角色请求。 */
export interface RoleCreateParams {
  roleCode: string;
  roleName: string;
}

/** Dashboard 概览统计（由前端聚合现有接口，无需专用后端）。 */
export interface DashboardStats {
  userCount: number;
  roleCount: number;
  permissionCount: number;
  systemOk: boolean;
}

/**
 * 审计/集中日志条目（契约对齐 Mongo app_logs，联调时对接 GET /api/v1/admin/audit/logs）。
 * @see docs/logging/README.md
 */
export interface AuditLogItem {
  id?: string;
  service?: string;
  env?: string;
  level: string;
  traceId?: string;
  logger?: string;
  message: string;
  stackTrace?: string;
  host?: string;
  timestamp: string;
}

/** 公开应用配置（GET /api/v1/public/app-config）。 */
export interface AppPublicConfig {
  siteName: string;
  siteLogo?: string;
  siteCopyright?: string;
  maintenanceMode: boolean;
  captchaEnabled: boolean;
  passwordMinLength: number;
  sessionIdleMinutes: number;
  auditRetentionDays: number;
}

/** 登录验证码挑战。 */
export interface CaptchaChallenge {
  captchaId: string;
  question: string;
}

/** 文件上传结果（POST /api/v1/files/upload）。 */
export interface FileUploadResult {
  fileKey: string;
  originalName: string;
  contentType: string;
  fileSize: number;
  bizType: string;
  accessLevel: string;
  url: string;
}
