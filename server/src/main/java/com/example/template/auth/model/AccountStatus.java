package com.example.template.auth.model;

/**
 * 账号 / 身份状态。sys_user 可取 NORMAL / DISABLED / DELETED；user_identity 取 NORMAL / DISABLED。
 */
public enum AccountStatus {
    /** 正常。 */
    NORMAL,
    /** 禁用。 */
    DISABLED,
    /** 已删除（仅 sys_user）。 */
    DELETED
}
