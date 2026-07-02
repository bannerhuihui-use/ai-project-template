package com.example.template.auth.service.impl;

import com.example.template.auth.model.AccountStatus;
import com.example.template.auth.model.SysUser;
import com.example.template.auth.repository.SysUserRepository;
import com.example.template.auth.security.CurrentUser;
import com.example.template.auth.security.SecurityUtils;
import com.example.template.auth.service.AuthAdminService;
import com.example.template.auth.support.SuperAdminGuard;
import com.example.template.common.BusinessException;
import com.example.template.common.ResultCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

/**
 * 管理员账号管理实现。
 */
@Service
public class AuthAdminServiceImpl implements AuthAdminService {

    private static final Logger log = LoggerFactory.getLogger(AuthAdminServiceImpl.class);

    private final SysUserRepository sysUserRepository;
    private final SuperAdminGuard superAdminGuard;

    public AuthAdminServiceImpl(SysUserRepository sysUserRepository, SuperAdminGuard superAdminGuard) {
        this.sysUserRepository = sysUserRepository;
        this.superAdminGuard = superAdminGuard;
    }

    @Override
    @Transactional
    public void disableUser(Long targetUserId) {
        CurrentUser operator = SecurityUtils.getCurrentUser();
        if (operator != null && Objects.equals(operator.getUserId(), targetUserId)) {
            throw new BusinessException(ResultCode.AUTH_CANNOT_DISABLE_SELF.getCode(),
                    ResultCode.AUTH_CANNOT_DISABLE_SELF.getMessage());
        }

        SysUser user = sysUserRepository.findActiveById(targetUserId)
                .orElseThrow(() -> new BusinessException(ResultCode.NOT_FOUND.getCode(),
                        ResultCode.NOT_FOUND.getMessage()));

        superAdminGuard.assertTargetOperable(targetUserId);

        if (AccountStatus.DISABLED.name().equals(user.getStatus())) {
            // 幂等：已禁用直接返回
            return;
        }

        sysUserRepository.updateStatus(targetUserId, AccountStatus.DISABLED.name());
        sysUserRepository.incrementTokenVersion(targetUserId);
        log.info("禁用账号: operatorId={}, targetUserId={}",
                operator == null ? null : operator.getUserId(), targetUserId);
    }

    @Override
    @Transactional
    public void forceLogout(Long targetUserId) {
        CurrentUser operator = SecurityUtils.getCurrentUser();
        sysUserRepository.findActiveById(targetUserId)
                .orElseThrow(() -> new BusinessException(ResultCode.NOT_FOUND.getCode(),
                        ResultCode.NOT_FOUND.getMessage()));

        sysUserRepository.incrementTokenVersion(targetUserId);
        log.info("强制下线: operatorId={}, targetUserId={}",
                operator == null ? null : operator.getUserId(), targetUserId);
    }
}
