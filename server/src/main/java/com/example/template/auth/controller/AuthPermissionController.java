package com.example.template.auth.controller;

import com.example.template.auth.dto.MenuNode;
import com.example.template.auth.service.AuthPermissionService;
import com.example.template.common.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 当前用户权限接口（菜单树、按钮/接口权限码）。需登录（access token）。
 * Controller 仅做参数接收，业务逻辑在 {@link AuthPermissionService}。
 */
@Tag(name = "鉴权-我的权限", description = "当前登录用户的菜单树与按钮权限码（前端动态路由/按钮显隐）")
@RestController
@RequestMapping("/api/v1/auth")
public class AuthPermissionController {

    private final AuthPermissionService authPermissionService;

    public AuthPermissionController(AuthPermissionService authPermissionService) {
        this.authPermissionService = authPermissionService;
    }

    @Operation(summary = "我的菜单树",
            description = "返回当前用户可访问的菜单树（后端已按角色过滤、按 sortNo 排序），用于前端动态路由、侧边栏、面包屑。",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "成功",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = Result.class))),
            @ApiResponse(responseCode = "401", description = "未认证或令牌失效")
    })
    @GetMapping("/menus")
    public Result<List<MenuNode>> menus() {
        return Result.success(authPermissionService.listCurrentMenus());
    }

    @Operation(summary = "我的按钮权限",
            description = "返回当前用户的按钮/接口权限编码数组，前端用于按钮显隐与禁用控制。",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "成功",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = Result.class))),
            @ApiResponse(responseCode = "401", description = "未认证或令牌失效")
    })
    @GetMapping("/permissions/buttons")
    public Result<List<String>> buttonPermissions() {
        return Result.success(authPermissionService.listCurrentButtonPermissions());
    }
}
