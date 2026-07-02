package com.example.template.file.controller;

import com.example.template.common.PageResult;
import com.example.template.common.Result;
import com.example.template.file.dto.FileItem;
import com.example.template.file.dto.FileUploadResult;
import com.example.template.file.service.FileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * 文件上传与管理（需登录）。
 */
@Tag(name = "文件", description = "文件上传、下载、删除（需 JWT + 权限码）")
@RestController
@RequestMapping("/api/v1/files")
@Validated
public class FileController {

    private final FileService fileService;

    public FileController(FileService fileService) {
        this.fileService = fileService;
    }

    @Operation(summary = "上传文件",
            description = "multipart 单文件上传。bizType 必填；avatar/image 默认 public，document/attachment 默认 private。需 file:upload。",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "上传成功",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = Result.class))),
            @ApiResponse(responseCode = "200", description = "失败：40030 类型不允许；40031 超出大小；40300 无权限")
    })
    @PreAuthorize("@perm.has('file:upload')")
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result<FileUploadResult> upload(
            @Parameter(description = "文件内容", required = true) @RequestParam("file") MultipartFile file,
            @Parameter(description = "业务分类：avatar/image/document/attachment", required = true, example = "image")
            @RequestParam("bizType") String bizType,
            @Parameter(description = "访问级别：public/private，可选")
            @RequestParam(value = "accessLevel", required = false) String accessLevel) {
        return Result.success(fileService.upload(file, bizType, accessLevel));
    }

    @Operation(summary = "查询文件元数据", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "成功"),
            @ApiResponse(responseCode = "200", description = "失败：40410 文件不存在；40310 无权访问")
    })
    @PreAuthorize("@perm.has('file:read')")
    @GetMapping("/{fileKey}")
    public Result<FileItem> getMetadata(
            @Parameter(description = "文件标识", required = true) @PathVariable String fileKey) {
        return Result.success(fileService.getMetadata(fileKey));
    }

    @Operation(summary = "下载/预览文件（私有或本人上传）", security = @SecurityRequirement(name = "bearerAuth"))
    @PreAuthorize("@perm.has('file:read')")
    @GetMapping("/{fileKey}/download")
    public ResponseEntity<Resource> download(
            @Parameter(description = "文件标识", required = true) @PathVariable String fileKey) {
        FileItem meta = fileService.getMetadata(fileKey);
        Resource resource = fileService.loadAsResource(fileKey, false);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition(meta.getOriginalName()))
                .contentType(MediaType.parseMediaType(meta.getContentType()))
                .body(resource);
    }

    @Operation(summary = "删除文件", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "删除成功"),
            @ApiResponse(responseCode = "200", description = "失败：40410；40311 无权删除")
    })
    @PreAuthorize("@perm.has('file:delete')")
    @DeleteMapping("/{fileKey}")
    public Result<Void> delete(
            @Parameter(description = "文件标识", required = true) @PathVariable String fileKey) {
        fileService.delete(fileKey);
        return Result.success();
    }

    @Operation(summary = "我的上传列表", security = @SecurityRequirement(name = "bearerAuth"))
    @PreAuthorize("@perm.has('file:read')")
    @GetMapping("/mine")
    public Result<PageResult<FileItem>> listMine(
            @Parameter(description = "页码，从 1 开始", example = "1")
            @RequestParam(defaultValue = "1") @Min(1) int pageNum,
            @Parameter(description = "每页条数，最大 100", example = "10")
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) int pageSize) {
        return Result.success(fileService.listMine(pageNum, pageSize));
    }

    private static String contentDisposition(String filename) {
        String safe = filename == null ? "file" : filename.replace("\"", "'");
        return "inline; filename=\"" + safe + "\"";
    }
}
