package com.example.template.auth.controller;

import com.example.template.auth.dto.LoginDeviceItem;
import com.example.template.auth.dto.RoleItem;
import com.example.template.auth.dto.UserAdminItem;
import com.example.template.auth.dto.UserCreateRequest;
import com.example.template.auth.dto.UserResetPasswordRequest;
import com.example.template.auth.dto.UserUpdateRequest;
import com.example.template.auth.service.AuthUserAdminService;
import com.example.template.common.PageResult;
import com.example.template.common.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 管理端用户 CRUD。Controller 仅做参数接收/校验，业务逻辑在 {@link AuthUserAdminService}。
 */
@Tag(name = "鉴权-用户管理", description = "后台用户分页、新建、编辑、重置密码、启用（需 ROLE_ADMIN + 权限码）")
@RestController
@RequestMapping("/api/v1/auth/admin")
public class AuthUserAdminController {

    private final AuthUserAdminService authUserAdminService;

    public AuthUserAdminController(AuthUserAdminService authUserAdminService) {
        this.authUserAdminService = authUserAdminService;
    }

    @Operation(summary = "用户分页列表",
            description = "分页查询后台用户，支持用户名/状态/角色筛选。需 auth:user:read。",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "成功",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = Result.class))),
            @ApiResponse(responseCode = "403", description = "无权限（40300）")
    })
    @PreAuthorize("@perm.has('auth:user:read')")
    @GetMapping("/users")
    public Result<PageResult<UserAdminItem>> pageUsers(
            @Parameter(description = "页码，从 1 开始") @RequestParam(defaultValue = "1") int pageNum,
            @Parameter(description = "每页大小，最大 100") @RequestParam(defaultValue = "10") int pageSize,
            @Parameter(description = "用户名模糊") @RequestParam(required = false) String username,
            @Parameter(description = "状态：NORMAL / DISABLED") @RequestParam(required = false) String status,
            @Parameter(description = "角色编码") @RequestParam(required = false) String roleCode) {
        return Result.success(authUserAdminService.pageUsers(username, status, roleCode, pageNum, pageSize));
    }

    @Operation(summary = "用户详情", description = "查询单个后台用户。需 auth:user:read。",
            security = @SecurityRequirement(name = "bearerAuth"))
    @PreAuthorize("@perm.has('auth:user:read')")
    @GetMapping("/users/{id}")
    public Result<UserAdminItem> getUser(@PathVariable("id") Long id) {
        return Result.success(authUserAdminService.getUser(id));
    }

    @Operation(summary = "新建用户", description = "创建后台账号（用户名+密码+可选角色）。需 auth:user:create。",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "成功，返回新用户 ID"),
            @ApiResponse(responseCode = "200", description = "失败：40011 用户名已存在")
    })
    @PreAuthorize("@perm.has('auth:user:create')")
    @PostMapping("/users")
    public Result<Long> createUser(@Valid @RequestBody UserCreateRequest request) {
        return Result.success(authUserAdminService.createUser(request));
    }

    @Operation(summary = "编辑用户",
            description = "更新昵称/邮箱/手机/状态/角色（角色覆盖式）。禁用时会令目标旧令牌失效。需 auth:user:update。",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "成功"),
            @ApiResponse(responseCode = "200", description = "失败：40400 用户不存在；40012 不能操作自己")
    })
    @PreAuthorize("@perm.has('auth:user:update')")
    @PutMapping("/users/{id}")
    public Result<Void> updateUser(@PathVariable("id") Long id,
                                   @Valid @RequestBody UserUpdateRequest request) {
        authUserAdminService.updateUser(id, request);
        return Result.success();
    }

    @Operation(summary = "重置密码",
            description = "管理员重置目标用户密码，并使该用户旧令牌全部失效。不能重置自己。需 auth:user:reset_password。",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "成功"),
            @ApiResponse(responseCode = "200", description = "失败：40400 用户不存在；40012 不能操作自己")
    })
    @PreAuthorize("@perm.has('auth:user:reset_password')")
    @PostMapping("/users/{id}/reset-password")
    public Result<Void> resetPassword(@PathVariable("id") Long id,
                                      @Valid @RequestBody UserResetPasswordRequest request) {
        authUserAdminService.resetPassword(id, request.getNewPassword());
        return Result.success();
    }

    @Operation(summary = "启用账号",
            description = "将 DISABLED 账号恢复为 NORMAL（幂等）。需 auth:user:update。",
            security = @SecurityRequirement(name = "bearerAuth"))
    @PreAuthorize("@perm.has('auth:user:update')")
    @PostMapping("/users/{id}/enable")
    public Result<Void> enableUser(@PathVariable("id") Long id) {
        authUserAdminService.enableUser(id);
        return Result.success();
    }

    @Operation(summary = "用户登录设备",
            description = "查询用户最近登录设备记录（按最近登录时间倒序，最多 100 条）。需 auth:user:read。",
            security = @SecurityRequirement(name = "bearerAuth"))
    @PreAuthorize("@perm.has('auth:user:read')")
    @GetMapping("/users/{id}/login-devices")
    public Result<List<LoginDeviceItem>> listLoginDevices(
            @PathVariable("id") Long id,
            @Parameter(description = "最大条数，默认 20，最大 100") @RequestParam(defaultValue = "20") int limit) {
        return Result.success(authUserAdminService.listLoginDevices(id, limit));
    }

    @Operation(summary = "角色下拉列表", description = "查询全部有效角色，供新建/编辑用户选择。需 auth:user:read。",
            security = @SecurityRequirement(name = "bearerAuth"))
    @PreAuthorize("@perm.has('auth:user:read')")
    @GetMapping("/roles")
    public Result<List<RoleItem>> listRoles() {
        return Result.success(authUserAdminService.listRoles());
    }
}
