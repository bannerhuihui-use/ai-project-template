package com.example.template.system.controller;

import com.example.template.common.Result;
import com.example.template.system.dto.ConfigBatchUpdateRequest;
import com.example.template.system.dto.ConfigCreateRequest;
import com.example.template.system.dto.ConfigItem;
import com.example.template.system.dto.ConfigMetaUpdateRequest;
import com.example.template.system.service.SystemConfigService;
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
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 系统配置管理（键值参数，供后台维护与业务模块读取）。
 */
@Tag(name = "系统-配置管理", description = "系统参数配置查询与批量更新（需 ROLE_ADMIN）")
@RestController
@RequestMapping("/api/v1/auth/admin/configs")
public class SystemConfigController {

    private final SystemConfigService systemConfigService;

    public SystemConfigController(SystemConfigService systemConfigService) {
        this.systemConfigService = systemConfigService;
    }

    @Operation(summary = "查询系统配置列表",
            description = "按分组返回全部配置项。需 auth:config:read。",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "成功",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = Result.class))),
            @ApiResponse(responseCode = "403", description = "无权限（40300）")
    })
    @PreAuthorize("@perm.has('auth:config:read')")
    @GetMapping
    public Result<List<ConfigItem>> listConfigs(
            @Parameter(description = "配置分组，如 site/auth/system") @RequestParam(required = false) String group) {
        return Result.success(systemConfigService.listConfigs(group));
    }

    @Operation(summary = "批量更新系统配置",
            description = "仅更新 editable=1 的配置项，按 value_type 校验格式。需 auth:config:update。",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "更新成功",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = Result.class))),
            @ApiResponse(responseCode = "200", description = "失败：40403 配置不存在；40020 不可编辑；40021 值非法",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = Result.class))),
            @ApiResponse(responseCode = "403", description = "无权限（40300）")
    })
    @PreAuthorize("@perm.has('auth:config:update')")
    @PutMapping
    public Result<Void> batchUpdate(@Valid @RequestBody ConfigBatchUpdateRequest request) {
        systemConfigService.batchUpdate(request);
        return Result.success();
    }

    @Operation(summary = "新建系统配置项",
            description = "创建自定义配置键，默认可编辑。配置键全局唯一。需 auth:config:create。",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "创建成功，返回新配置 ID",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = Result.class))),
            @ApiResponse(responseCode = "200", description = "失败：40022 配置键已存在；40021 值非法",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = Result.class))),
            @ApiResponse(responseCode = "403", description = "无权限（40300）")
    })
    @PreAuthorize("@perm.has('auth:config:create')")
    @PostMapping
    public Result<Long> create(@Valid @RequestBody ConfigCreateRequest request) {
        return Result.success(systemConfigService.create(request));
    }

    @Operation(summary = "更新配置元数据",
            description = "更新名称、分组、说明、排序（不含 configKey 与 configValue）。需 auth:config:update。",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "更新成功"),
            @ApiResponse(responseCode = "200", description = "失败：40403 不存在；40020 不可编辑"),
            @ApiResponse(responseCode = "403", description = "无权限（40300）")
    })
    @PreAuthorize("@perm.has('auth:config:update')")
    @PatchMapping("/{id}")
    public Result<Void> updateMetadata(@Parameter(description = "配置 ID") @PathVariable("id") Long id,
                                       @Valid @RequestBody ConfigMetaUpdateRequest request) {
        systemConfigService.updateMetadata(id, request);
        return Result.success();
    }

    @Operation(summary = "删除配置项",
            description = "软删除自定义配置项，内置种子项不可删。需 auth:config:delete。",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "删除成功"),
            @ApiResponse(responseCode = "200", description = "失败：40403 不存在；40023 不可删除"),
            @ApiResponse(responseCode = "403", description = "无权限（40300）")
    })
    @PreAuthorize("@perm.has('auth:config:delete')")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@Parameter(description = "配置 ID") @PathVariable("id") Long id) {
        systemConfigService.delete(id);
        return Result.success();
    }
}
