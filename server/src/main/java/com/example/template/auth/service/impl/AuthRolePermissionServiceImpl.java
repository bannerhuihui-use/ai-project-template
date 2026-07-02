package com.example.template.auth.service.impl;

import com.example.template.auth.dto.PermissionItem;
import com.example.template.auth.model.PermissionType;
import com.example.template.auth.model.SysPermission;
import com.example.template.auth.repository.PermissionRepository;
import com.example.template.auth.repository.SysRoleRepository;
import com.example.template.auth.repository.SysUserRepository;
import com.example.template.auth.security.UserPermissionCacheService;
import com.example.template.auth.service.AuthRolePermissionService;
import com.example.template.auth.support.RbacChangeAuditLogger;
import com.example.template.auth.support.UserProfileValidator;
import com.example.template.common.BusinessException;
import com.example.template.common.PageResult;
import com.example.template.common.ResultCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 管理端权限点查询与角色权限配置实现。
 */
@Service
public class AuthRolePermissionServiceImpl implements AuthRolePermissionService {

    private static final Logger log = LoggerFactory.getLogger(AuthRolePermissionServiceImpl.class);

    private static final int DEFAULT_PAGE_NUM = 1;
    private static final int DEFAULT_PAGE_SIZE = 10;
    private static final int MAX_PAGE_SIZE = 100;

    private final PermissionRepository permissionRepository;
    private final SysRoleRepository sysRoleRepository;
    private final SysUserRepository sysUserRepository;
    private final UserPermissionCacheService userPermissionCacheService;

    public AuthRolePermissionServiceImpl(PermissionRepository permissionRepository,
                                         SysRoleRepository sysRoleRepository,
                                         SysUserRepository sysUserRepository,
                                         UserPermissionCacheService userPermissionCacheService) {
        this.permissionRepository = permissionRepository;
        this.sysRoleRepository = sysRoleRepository;
        this.sysUserRepository = sysUserRepository;
        this.userPermissionCacheService = userPermissionCacheService;
    }

    @Override
    public PageResult<PermissionItem> pagePermissions(String type, String keyword, int pageNum, int pageSize) {
        validatePermissionType(type);
        UserProfileValidator.validateSearchKeyword(keyword, "关键字");
        int safePageNum = pageNum < 1 ? DEFAULT_PAGE_NUM : pageNum;
        int safePageSize = pageSize < 1 ? DEFAULT_PAGE_SIZE : Math.min(pageSize, MAX_PAGE_SIZE);
        int offset = (safePageNum - 1) * safePageSize;

        long total = permissionRepository.countPage(type, keyword);
        List<PermissionItem> list = new ArrayList<>();
        if (total > 0) {
            for (SysPermission p : permissionRepository.findPage(type, keyword, offset, safePageSize)) {
                list.add(toItem(p));
            }
        }
        return PageResult.of(total, safePageNum, safePageSize, list);
    }

    @Override
    public List<PermissionItem> getRolePermissions(Long roleId) {
        if (!sysRoleRepository.existsActiveById(roleId)) {
            throw new BusinessException(ResultCode.AUTH_ROLE_NOT_FOUND.getCode(),
                    ResultCode.AUTH_ROLE_NOT_FOUND.getMessage());
        }
        List<PermissionItem> list = new ArrayList<>();
        for (SysPermission p : permissionRepository.findByRoleId(roleId)) {
            list.add(toItem(p));
        }
        return list;
    }

    @Override
    @Transactional
    public void replaceRolePermissions(Long roleId, List<String> permissionCodes) {
        if (!sysRoleRepository.existsActiveById(roleId)) {
            throw new BusinessException(ResultCode.AUTH_ROLE_NOT_FOUND.getCode(),
                    ResultCode.AUTH_ROLE_NOT_FOUND.getMessage());
        }

        Set<String> codes = distinctNonBlank(permissionCodes);
        List<Long> permissionIds = new ArrayList<>();
        if (!codes.isEmpty()) {
            permissionIds = permissionRepository.findActiveIdsByCodes(codes);
            if (permissionIds.size() != codes.size()) {
                // 存在无效/已禁用的权限编码
                throw new BusinessException(ResultCode.AUTH_PERMISSION_NOT_FOUND.getCode(),
                        ResultCode.AUTH_PERMISSION_NOT_FOUND.getMessage());
            }
        }

        permissionRepository.softDeleteByRoleId(roleId);
        permissionRepository.insertRolePermissions(roleId, permissionIds);
        int affected = sysUserRepository.incrementTokenVersionByRoleId(roleId);
        userPermissionCacheService.invalidateAll();
        RbacChangeAuditLogger.auditRolePermissions(roleId, permissionIds.size());
        log.info("角色权限已更新: roleId={}, permissionCount={}, affectedUsers={}",
                roleId, permissionIds.size(), affected);
    }

    private void validatePermissionType(String type) {
        if (!StringUtils.hasText(type)) {
            return;
        }
        try {
            PermissionType.valueOf(type.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BusinessException(ResultCode.PARAM_ERROR.getCode(),
                    "权限类型仅支持 MENU/BUTTON/API/DATA");
        }
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

    private PermissionItem toItem(SysPermission p) {
        PermissionItem item = new PermissionItem();
        item.setId(p.getId());
        item.setPermissionCode(p.getPermissionCode());
        item.setPermissionName(p.getPermissionName());
        item.setPermissionType(p.getPermissionType());
        item.setModule(p.getModule());
        item.setStatus(p.getStatus());
        return item;
    }
}
