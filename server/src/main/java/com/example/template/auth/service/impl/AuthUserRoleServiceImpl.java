package com.example.template.auth.service.impl;

import com.example.template.auth.repository.SysRoleRepository;
import com.example.template.auth.repository.SysUserRepository;
import com.example.template.auth.repository.SysUserRoleRepository;
import com.example.template.auth.security.UserPermissionCacheService;
import com.example.template.auth.service.AuthUserRoleService;
import com.example.template.auth.support.SuperAdminGuard;
import com.example.template.auth.support.RbacChangeAuditLogger;
import com.example.template.common.BusinessException;
import com.example.template.common.ResultCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 用户角色配置实现。
 */
@Service
public class AuthUserRoleServiceImpl implements AuthUserRoleService {

    private static final Logger log = LoggerFactory.getLogger(AuthUserRoleServiceImpl.class);

    private final SysUserRoleRepository sysUserRoleRepository;
    private final SysRoleRepository sysRoleRepository;
    private final SysUserRepository sysUserRepository;
    private final UserPermissionCacheService userPermissionCacheService;
    private final SuperAdminGuard superAdminGuard;

    public AuthUserRoleServiceImpl(SysUserRoleRepository sysUserRoleRepository,
                                   SysRoleRepository sysRoleRepository,
                                   SysUserRepository sysUserRepository,
                                   UserPermissionCacheService userPermissionCacheService,
                                   SuperAdminGuard superAdminGuard) {
        this.sysUserRoleRepository = sysUserRoleRepository;
        this.sysRoleRepository = sysRoleRepository;
        this.sysUserRepository = sysUserRepository;
        this.userPermissionCacheService = userPermissionCacheService;
        this.superAdminGuard = superAdminGuard;
    }

    @Override
    @Transactional
    public void replaceUserRoles(Long userId, List<String> roleCodes) {
        if (sysUserRepository.findActiveById(userId).isEmpty()) {
            throw new BusinessException(ResultCode.NOT_FOUND.getCode(), ResultCode.NOT_FOUND.getMessage());
        }

        superAdminGuard.validateRoleReplacement(userId, roleCodes);

        Set<String> codes = distinctNonBlank(roleCodes);
        List<Long> roleIds = new ArrayList<>();
        if (!codes.isEmpty()) {
            roleIds = sysRoleRepository.findActiveIdsByCodes(codes);
            if (roleIds.size() != codes.size()) {
                throw new BusinessException(ResultCode.AUTH_ROLE_NOT_FOUND.getCode(),
                        ResultCode.AUTH_ROLE_NOT_FOUND.getMessage());
            }
        }

        sysUserRoleRepository.softDeleteByUserId(userId);
        sysUserRoleRepository.insertUserRoles(userId, roleIds);
        sysUserRepository.incrementTokenVersion(userId);
        userPermissionCacheService.invalidateUser(userId);
        String roleSummary = String.join(",", codes);
        RbacChangeAuditLogger.auditUserRoles(userId, roleIds.size(), roleSummary);
        log.info("用户角色已更新: userId={}, roleCount={}", userId, roleIds.size());
    }

    private Set<String> distinctNonBlank(List<String> codes) {
        Set<String> result = new LinkedHashSet<>();
        if (codes != null) {
            for (String code : codes) {
                if (code != null && !code.isBlank()) {
                    result.add(code.trim());
                }
            }
        }
        return result;
    }
}
