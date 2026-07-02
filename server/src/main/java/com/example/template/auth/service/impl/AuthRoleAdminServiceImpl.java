package com.example.template.auth.service.impl;

import com.example.template.auth.dto.RoleCreateRequest;
import com.example.template.auth.dto.RoleItem;
import com.example.template.auth.repository.SysRoleRepository;
import com.example.template.auth.service.AuthRoleAdminService;
import com.example.template.auth.support.RbacChangeAuditLogger;
import com.example.template.common.BusinessException;
import com.example.template.common.ResultCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

/**
 * 管理端角色维护实现。
 */
@Service
public class AuthRoleAdminServiceImpl implements AuthRoleAdminService {

    private static final Logger log = LoggerFactory.getLogger(AuthRoleAdminServiceImpl.class);

    /** 禁止通过接口创建的系统保留角色编码。 */
    private static final Set<String> RESERVED_ROLE_CODES = Set.of("SUPER_ADMIN");

    private final SysRoleRepository sysRoleRepository;

    public AuthRoleAdminServiceImpl(SysRoleRepository sysRoleRepository) {
        this.sysRoleRepository = sysRoleRepository;
    }

    @Override
    @Transactional
    public RoleItem createRole(RoleCreateRequest request) {
        String roleCode = request.getRoleCode().trim().toUpperCase();
        String roleName = request.getRoleName().trim();

        if (RESERVED_ROLE_CODES.contains(roleCode)) {
            throw new BusinessException(ResultCode.AUTH_ROLE_CODE_RESERVED);
        }
        if (sysRoleRepository.existsActiveByCode(roleCode)) {
            throw new BusinessException(ResultCode.AUTH_ROLE_CODE_EXISTS);
        }

        Long roleId = sysRoleRepository.insert(roleCode, roleName);
        RbacChangeAuditLogger.auditRoleCreate(roleId, roleCode);
        log.info("新建角色成功: roleId={}, roleCode={}", roleId, roleCode);
        return new RoleItem(roleId, roleCode, roleName);
    }
}
