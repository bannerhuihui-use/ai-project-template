package com.example.template.auth.service;

import com.example.template.auth.dto.LoginDeviceItem;
import com.example.template.auth.dto.RoleItem;
import com.example.template.auth.dto.UserAdminItem;
import com.example.template.auth.dto.UserCreateRequest;
import com.example.template.auth.dto.UserUpdateRequest;
import com.example.template.common.PageResult;

import java.util.List;

/**
 * 管理端用户 CRUD 与角色下拉。
 */
public interface AuthUserAdminService {

    PageResult<UserAdminItem> pageUsers(String username, String status, String roleCode, int pageNum, int pageSize);

    UserAdminItem getUser(Long userId);

    Long createUser(UserCreateRequest request);

    void updateUser(Long userId, UserUpdateRequest request);

    void resetPassword(Long userId, String newPassword);

    void enableUser(Long userId);

    List<LoginDeviceItem> listLoginDevices(Long userId, int limit);

    List<RoleItem> listRoles();
}
