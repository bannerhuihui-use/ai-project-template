package com.example.template.auth.support;

import com.example.template.auth.repository.SysRoleRepository;
import com.example.template.auth.security.CurrentUser;
import com.example.template.auth.security.SecurityUtils;
import com.example.template.common.BusinessException;
import com.example.template.common.ResultCode;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;

/**
 * 超级管理员（SUPER_ADMIN）角色保护：禁止禁用、禁止非超管授角色、禁止移除最后一名超管。
 */
@Component
public class SuperAdminGuard {

    /** 系统保留的超级管理员角色编码。 */
    public static final String ROLE_CODE = "SUPER_ADMIN";

    private final SysRoleRepository sysRoleRepository;

    public SuperAdminGuard(SysRoleRepository sysRoleRepository) {
        this.sysRoleRepository = sysRoleRepository;
    }

    /**
     * 目标用户是否拥有 SUPER_ADMIN 角色。
     */
    public boolean userHasSuperAdmin(Long userId) {
        if (userId == null) {
            return false;
        }
        return sysRoleRepository.findRoleCodesByUserId(userId).contains(ROLE_CODE);
    }

    /**
     * 禁止对 SUPER_ADMIN 用户执行禁用等敏感操作。
     */
    public void assertTargetOperable(Long userId) {
        if (userHasSuperAdmin(userId)) {
            throw new BusinessException(ResultCode.AUTH_CANNOT_OPERATE_SUPER_ADMIN);
        }
    }

    /**
     * 校验角色覆盖式分配：授予 SUPER_ADMIN 仅允许当前操作者本人为 SUPER_ADMIN。
     */
    public void validateRoleAssignment(Collection<String> roleCodes) {
        if (!containsSuperAdmin(roleCodes)) {
            return;
        }
        CurrentUser operator = SecurityUtils.getCurrentUser();
        if (operator == null || !operatorHasSuperAdmin(operator)) {
            throw new BusinessException(ResultCode.AUTH_SUPER_ADMIN_GRANT_DENIED);
        }
    }

    /**
     * 校验用户角色覆盖：含授予限制 + 不可移除系统中最后一名 SUPER_ADMIN。
     */
    public void validateRoleReplacement(Long userId, List<String> newRoleCodes) {
        validateRoleAssignment(newRoleCodes);
        if (!userHasSuperAdmin(userId) || containsSuperAdmin(newRoleCodes)) {
            return;
        }
        long count = sysRoleRepository.countUsersWithRoleCode(ROLE_CODE);
        if (count <= 1) {
            throw new BusinessException(ResultCode.AUTH_LAST_SUPER_ADMIN);
        }
    }

    private boolean operatorHasSuperAdmin(CurrentUser operator) {
        List<String> roles = operator.getRoles();
        return roles != null && roles.contains(ROLE_CODE);
    }

    private boolean containsSuperAdmin(Collection<String> roleCodes) {
        if (roleCodes == null || roleCodes.isEmpty()) {
            return false;
        }
        for (String code : roleCodes) {
            if (code != null && ROLE_CODE.equals(code.trim())) {
                return true;
            }
        }
        return false;
    }
}
