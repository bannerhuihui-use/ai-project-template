package com.example.template.auth.controller;

import com.example.template.auth.service.AuthAdminService;
import com.example.template.common.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 管理员账号管理接口。整个 /admin/** 由 SecurityConfig 统一要求 ROLE_ADMIN。
 * Controller 仅做参数接收，业务逻辑在 {@link AuthAdminService}。
 */
@Tag(name = "鉴权-管理员", description = "禁用账号、强制下线等管理员账号管理接口（需 ROLE_ADMIN）")
@RestController
@RequestMapping("/api/v1/auth/admin")
public class AuthAdminController {

    private final AuthAdminService authAdminService;

    public AuthAdminController(AuthAdminService authAdminService) {
        this.authAdminService = authAdminService;
    }

    @Operation(summary = "禁用账号",
            description = "将目标账号状态置为 DISABLED，并使其所有令牌立即失效。不能禁用自己；幂等。",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "禁用成功",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = Result.class))),
            @ApiResponse(responseCode = "200", description = "失败：40302 不能禁用自己；40400 用户不存在",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = Result.class),
                            examples = @ExampleObject(value = "{\"code\": 40302, \"message\": \"不能禁用自己\", \"data\": null}"))),
            @ApiResponse(responseCode = "401", description = "未认证或令牌失效"),
            @ApiResponse(responseCode = "403", description = "非管理员（40300）")
    })
    @PreAuthorize("@perm.has('auth:user:disable')")
    @PostMapping("/users/{id}/disable")
    public Result<Void> disableUser(@PathVariable("id") Long id) {
        authAdminService.disableUser(id);
        return Result.success();
    }

    @Operation(summary = "强制下线",
            description = "使目标账号所有令牌立即失效（不改变账号状态），用于强制重新登录。幂等。",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "操作成功",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = Result.class))),
            @ApiResponse(responseCode = "200", description = "失败：40400 用户不存在",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = Result.class),
                            examples = @ExampleObject(value = "{\"code\": 40400, \"message\": \"资源不存在\", \"data\": null}"))),
            @ApiResponse(responseCode = "401", description = "未认证或令牌失效"),
            @ApiResponse(responseCode = "403", description = "非管理员（40300）")
    })
    @PreAuthorize("@perm.has('auth:user:force_logout')")
    @PostMapping("/users/{id}/force-logout")
    public Result<Void> forceLogout(@PathVariable("id") Long id) {
        authAdminService.forceLogout(id);
        return Result.success();
    }
}
