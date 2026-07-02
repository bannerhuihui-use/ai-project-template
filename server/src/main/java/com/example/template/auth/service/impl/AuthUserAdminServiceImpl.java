package com.example.template.auth.service.impl;

import com.example.template.auth.dto.LoginDeviceItem;
import com.example.template.auth.dto.RoleItem;
import com.example.template.auth.dto.UserAdminItem;
import com.example.template.auth.dto.UserCreateRequest;
import com.example.template.auth.dto.UserUpdateRequest;
import com.example.template.auth.model.AccountStatus;
import com.example.template.auth.model.IdentityType;
import com.example.template.auth.model.SysUser;
import com.example.template.auth.repository.LoginDeviceRepository;
import com.example.template.auth.repository.SysRoleRepository;
import com.example.template.auth.repository.SysUserRepository;
import com.example.template.auth.repository.UserIdentityRepository;
import com.example.template.auth.security.CurrentUser;
import com.example.template.auth.security.SecurityUtils;
import com.example.template.auth.service.AuthUserAdminService;
import com.example.template.auth.service.AuthUserRoleService;
import com.example.template.auth.support.SuperAdminGuard;
import com.example.template.auth.support.UserProfileValidator;
import com.example.template.common.BusinessException;
import com.example.template.common.PageResult;
import com.example.template.common.ResultCode;
import com.example.template.system.service.SystemConfigService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Objects;

/**
 * 管理端用户 CRUD 实现。
 */
@Service
public class AuthUserAdminServiceImpl implements AuthUserAdminService {

    private static final Logger log = LoggerFactory.getLogger(AuthUserAdminServiceImpl.class);
    private static final int MAX_PAGE_SIZE = 100;

    private final SysUserRepository sysUserRepository;
    private final UserIdentityRepository userIdentityRepository;
    private final SysRoleRepository sysRoleRepository;
    private final LoginDeviceRepository loginDeviceRepository;
    private final AuthUserRoleService authUserRoleService;
    private final PasswordEncoder passwordEncoder;
    private final SystemConfigService systemConfigService;
    private final SuperAdminGuard superAdminGuard;

    public AuthUserAdminServiceImpl(SysUserRepository sysUserRepository,
                                    UserIdentityRepository userIdentityRepository,
                                    SysRoleRepository sysRoleRepository,
                                    LoginDeviceRepository loginDeviceRepository,
                                    AuthUserRoleService authUserRoleService,
                                    PasswordEncoder passwordEncoder,
                                    SystemConfigService systemConfigService,
                                    SuperAdminGuard superAdminGuard) {
        this.sysUserRepository = sysUserRepository;
        this.userIdentityRepository = userIdentityRepository;
        this.sysRoleRepository = sysRoleRepository;
        this.loginDeviceRepository = loginDeviceRepository;
        this.authUserRoleService = authUserRoleService;
        this.passwordEncoder = passwordEncoder;
        this.systemConfigService = systemConfigService;
        this.superAdminGuard = superAdminGuard;
    }

    @Override
    public PageResult<UserAdminItem> pageUsers(String username, String status, String roleCode,
                                               int pageNum, int pageSize) {
        UserProfileValidator.validateSearchKeyword(username, "用户名");
        UserProfileValidator.validateSearchKeyword(roleCode, "角色编码");
        if (StringUtils.hasText(status)) {
            validateStatus(status.trim());
        }
        int safePageNum = Math.max(pageNum, 1);
        int safePageSize = Math.min(Math.max(pageSize, 1), MAX_PAGE_SIZE);
        int offset = (safePageNum - 1) * safePageSize;
        long total = sysUserRepository.countAdminPage(username, status, roleCode);
        List<UserAdminItem> list = sysUserRepository.findAdminPage(username, status, roleCode, offset, safePageSize);
        return PageResult.of(total, safePageNum, safePageSize, list);
    }

    @Override
    public UserAdminItem getUser(Long userId) {
        return sysUserRepository.findAdminItemById(userId)
                .orElseThrow(() -> new BusinessException(ResultCode.NOT_FOUND.getCode(),
                        ResultCode.NOT_FOUND.getMessage()));
    }

    @Override
    @Transactional
    public Long createUser(UserCreateRequest request) {
        String username = request.getUsername().trim();
        UserProfileValidator.validateUsername(username);
        UserProfileValidator.validateOptionalProfile(request.getNickname(), request.getEmail(), request.getPhone());
        if (userIdentityRepository.existsActiveByTypeAndIdentifier(IdentityType.USERNAME.name(), username)) {
            throw new BusinessException(ResultCode.AUTH_USERNAME_EXISTS);
        }
        systemConfigService.validatePasswordLength(request.getPassword());

        Long userId = sysUserRepository.insertAdminUser(
                trimToNull(request.getNickname()),
                trimToNull(request.getEmail()),
                trimToNull(request.getPhone()));
        userIdentityRepository.insert(userId, IdentityType.USERNAME.name(), username,
                passwordEncoder.encode(request.getPassword()));

        if (request.getRoleCodes() != null && !request.getRoleCodes().isEmpty()) {
            authUserRoleService.replaceUserRoles(userId, request.getRoleCodes());
        }

        CurrentUser operator = SecurityUtils.getCurrentUser();
        log.info("新建后台用户: operatorId={}, newUserId={}, username={}",
                operator == null ? null : operator.getUserId(), userId, username);
        return userId;
    }

    @Override
    @Transactional
    public void updateUser(Long userId, UserUpdateRequest request) {
        SysUser user = sysUserRepository.findActiveById(userId)
                .orElseThrow(() -> new BusinessException(ResultCode.NOT_FOUND.getCode(),
                        ResultCode.NOT_FOUND.getMessage()));

        UserProfileValidator.validateOptionalProfile(request.getNickname(), request.getEmail(), request.getPhone());

        sysUserRepository.updateProfile(userId,
                trimToNull(request.getNickname()),
                trimToNull(request.getEmail()),
                trimToNull(request.getPhone()));

        if (StringUtils.hasText(request.getStatus())) {
            String targetStatus = request.getStatus().trim();
            validateStatus(targetStatus);
            if (!targetStatus.equals(user.getStatus())) {
                assertNotSelf(userId, ResultCode.AUTH_CANNOT_OPERATE_SELF);
                if (AccountStatus.DISABLED.name().equals(targetStatus)) {
                    superAdminGuard.assertTargetOperable(userId);
                }
                sysUserRepository.updateStatus(userId, targetStatus);
                if (AccountStatus.DISABLED.name().equals(targetStatus)) {
                    sysUserRepository.incrementTokenVersion(userId);
                }
            }
        }

        if (request.getRoleCodes() != null) {
            authUserRoleService.replaceUserRoles(userId, request.getRoleCodes());
        }

        CurrentUser operator = SecurityUtils.getCurrentUser();
        log.info("更新后台用户: operatorId={}, targetUserId={}",
                operator == null ? null : operator.getUserId(), userId);
    }

    @Override
    @Transactional
    public void resetPassword(Long userId, String newPassword) {
        assertNotSelf(userId, ResultCode.AUTH_CANNOT_OPERATE_SELF);
        sysUserRepository.findActiveById(userId)
                .orElseThrow(() -> new BusinessException(ResultCode.NOT_FOUND.getCode(),
                        ResultCode.NOT_FOUND.getMessage()));
        userIdentityRepository.findActiveByUserIdAndType(userId, IdentityType.USERNAME.name())
                .orElseThrow(() -> new BusinessException(ResultCode.NOT_FOUND.getCode(),
                        ResultCode.NOT_FOUND.getMessage()));
        systemConfigService.validatePasswordLength(newPassword);

        userIdentityRepository.updateCredentialByUserIdAndType(
                userId, IdentityType.USERNAME.name(), passwordEncoder.encode(newPassword));
        sysUserRepository.updatePasswordUpdatedAt(userId);
        sysUserRepository.incrementTokenVersion(userId);

        CurrentUser operator = SecurityUtils.getCurrentUser();
        log.info("重置用户密码: operatorId={}, targetUserId={}",
                operator == null ? null : operator.getUserId(), userId);
    }

    @Override
    @Transactional
    public void enableUser(Long userId) {
        SysUser user = sysUserRepository.findActiveById(userId)
                .orElseThrow(() -> new BusinessException(ResultCode.NOT_FOUND.getCode(),
                        ResultCode.NOT_FOUND.getMessage()));
        if (AccountStatus.NORMAL.name().equals(user.getStatus())) {
            return;
        }
        sysUserRepository.updateStatus(userId, AccountStatus.NORMAL.name());
        CurrentUser operator = SecurityUtils.getCurrentUser();
        log.info("启用账号: operatorId={}, targetUserId={}",
                operator == null ? null : operator.getUserId(), userId);
    }

    @Override
    public List<LoginDeviceItem> listLoginDevices(Long userId, int limit) {
        if (sysUserRepository.findActiveById(userId).isEmpty()) {
            throw new BusinessException(ResultCode.NOT_FOUND.getCode(), ResultCode.NOT_FOUND.getMessage());
        }
        return loginDeviceRepository.findRecentByUserId(userId, limit).stream()
                .map(this::toLoginDeviceItem)
                .toList();
    }

    @Override
    public List<RoleItem> listRoles() {
        return sysRoleRepository.findAllActiveRoles();
    }

    private void validateStatus(String status) {
        if (!AccountStatus.NORMAL.name().equals(status) && !AccountStatus.DISABLED.name().equals(status)) {
            throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), "状态仅支持 NORMAL 或 DISABLED");
        }
    }

    private void assertNotSelf(Long targetUserId, ResultCode code) {
        CurrentUser operator = SecurityUtils.getCurrentUser();
        if (operator != null && Objects.equals(operator.getUserId(), targetUserId)) {
            throw new BusinessException(code);
        }
    }

    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    private LoginDeviceItem toLoginDeviceItem(LoginDeviceRepository.LoginDeviceRow row) {
        LoginDeviceItem item = new LoginDeviceItem();
        item.setId(row.getId());
        item.setIdentityType(row.getIdentityType());
        item.setDeviceId(row.getDeviceId());
        item.setDeviceName(row.getDeviceName());
        item.setPlatform(row.getPlatform());
        item.setIp(row.getIp());
        item.setLastLoginTime(row.getLastLoginTime());
        return item;
    }
}
