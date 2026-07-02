package com.example.template.auth.controller;

import com.example.template.auth.dto.PermissionItem;
import com.example.template.auth.dto.RoleCreateRequest;
import com.example.template.auth.dto.RoleItem;
import com.example.template.auth.dto.RolePermissionUpdateRequest;
import com.example.template.auth.dto.UserRoleUpdateRequest;
import com.example.template.auth.service.AuthRoleAdminService;
import com.example.template.auth.service.AuthRolePermissionService;
import com.example.template.auth.service.AuthUserRoleService;
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
 * 管理端 RBAC 接口：权限点查询、角色权限配置、用户角色配置。
 *
 * <p>整个 /admin/** 由 SecurityConfig 统一要求 ROLE_ADMIN；具体权限码（hasAuthority）的方法级校验
 * 在 v2.0 安全步骤统一开启。Controller 仅做参数接收，业务逻辑在 Service。</p>
 */
@Tag(name = "鉴权-RBAC管理", description = "权限点查询、角色权限配置、用户角色配置（需 ROLE_ADMIN）")
@RestController
@RequestMapping("/api/v1/auth/admin")
public class AuthRbacAdminController {

    private final AuthRolePermissionService authRolePermissionService;
    private final AuthUserRoleService authUserRoleService;
    private final AuthRoleAdminService authRoleAdminService;

    public AuthRbacAdminController(AuthRolePermissionService authRolePermissionService,
                                   AuthUserRoleService authUserRoleService,
                                   AuthRoleAdminService authRoleAdminService) {
        this.authRolePermissionService = authRolePermissionService;
        this.authUserRoleService = authUserRoleService;
        this.authRoleAdminService = authRoleAdminService;
    }

    @Operation(summary = "权限点分页列表",
            description = "分页查询权限点，支持按类型与关键字过滤。需 ROLE_ADMIN + 权限码 auth:permission:read。",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "成功",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = Result.class))),
            @ApiResponse(responseCode = "401", description = "未认证或令牌失效"),
            @ApiResponse(responseCode = "403", description = "非管理员/权限不足（40300）")
    })
    @PreAuthorize("@perm.has('auth:permission:read')")
    @GetMapping("/permissions")
    public Result<PageResult<PermissionItem>> pagePermissions(
            @Parameter(description = "页码，从 1 开始") @RequestParam(defaultValue = "1") int pageNum,
            @Parameter(description = "每页大小，最大 100") @RequestParam(defaultValue = "10") int pageSize,
            @Parameter(description = "权限类型：MENU/BUTTON/API/DATA") @RequestParam(required = false) String type,
            @Parameter(description = "关键字（编码/名称模糊）") @RequestParam(required = false) String keyword) {
        return Result.success(authRolePermissionService.pagePermissions(type, keyword, pageNum, pageSize));
    }

    @Operation(summary = "新建角色",
            description = "创建自定义角色（默认无权限，需另行配置）。禁止创建 SUPER_ADMIN。"
                    + "需 auth:role:create。",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "创建成功",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = Result.class))),
            @ApiResponse(responseCode = "200", description = "失败：40013 角色编码已存在；40014 保留编码",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = Result.class))),
            @ApiResponse(responseCode = "403", description = "无权限（40300）")
    })
    @PreAuthorize("@perm.has('auth:role:create')")
    @PostMapping("/roles")
    public Result<RoleItem> createRole(@Valid @RequestBody RoleCreateRequest request) {
        return Result.success(authRoleAdminService.createRole(request));
    }

    @Operation(summary = "查询角色权限",
            description = "查询指定角色当前绑定的权限。需 ROLE_ADMIN + 权限码 auth:role:read。",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "成功",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = Result.class))),
            @ApiResponse(responseCode = "200", description = "失败：40401 角色不存在",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = Result.class))),
            @ApiResponse(responseCode = "403", description = "非管理员/权限不足（40300）")
    })
    @PreAuthorize("@perm.has('auth:role:read')")
    @GetMapping("/roles/{roleId}/permissions")
    public Result<List<PermissionItem>> getRolePermissions(@PathVariable("roleId") Long roleId) {
        return Result.success(authRolePermissionService.getRolePermissions(roleId));
    }

    @Operation(summary = "覆盖设置角色权限",
            description = "用权限编码列表覆盖角色权限（空数组=清空）；成功后该角色下所有用户旧令牌失效。"
                    + "需 ROLE_ADMIN + 权限码 auth:role:grant_permission。",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "设置成功",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = Result.class))),
            @ApiResponse(responseCode = "200", description = "失败：40401 角色不存在；40402 权限编码非法/已禁用",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = Result.class))),
            @ApiResponse(responseCode = "403", description = "非管理员/权限不足（40300）")
    })
    @PreAuthorize("@perm.has('auth:role:grant_permission')")
    @PutMapping("/roles/{roleId}/permissions")
    public Result<Void> replaceRolePermissions(@PathVariable("roleId") Long roleId,
                                               @Valid @RequestBody RolePermissionUpdateRequest request) {
        authRolePermissionService.replaceRolePermissions(roleId, request.getPermissionCodes());
        return Result.success();
    }

    @Operation(summary = "覆盖设置用户角色",
            description = "用角色编码列表覆盖用户角色（空数组=清空）；成功后该用户旧令牌失效。"
                    + "需 ROLE_ADMIN + 权限码 auth:user:grant_role。",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "设置成功",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = Result.class))),
            @ApiResponse(responseCode = "200", description = "失败：40400 用户不存在；40401 角色编码非法/已禁用",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = Result.class))),
            @ApiResponse(responseCode = "403", description = "非管理员/权限不足（40300）")
    })
    @PreAuthorize("@perm.has('auth:user:grant_role')")
    @PutMapping("/users/{userId}/roles")
    public Result<Void> replaceUserRoles(@PathVariable("userId") Long userId,
                                         @Valid @RequestBody UserRoleUpdateRequest request) {
        authUserRoleService.replaceUserRoles(userId, request.getRoleCodes());
        return Result.success();
    }

    @Operation(summary = "RBAC 探活",
            description = "用于验证 ROLE_ADMIN + hasAuthority 保护链路。需权限码 auth:rbac:ping。",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "成功",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = Result.class))),
            @ApiResponse(responseCode = "403", description = "非管理员/权限不足（40300）")
    })
    @PreAuthorize("@perm.has('auth:rbac:ping')")
    @GetMapping("/rbac/ping")
    public Result<String> ping() {
        return Result.success("pong");
    }
}
