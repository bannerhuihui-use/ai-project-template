package com.example.template.file.controller;

import com.example.template.file.dto.PublicFileView;
import com.example.template.file.service.FileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 公开文件访问（仅 access=public 的文件，已在 auth 白名单 /api/v1/public/**）。
 */
@Tag(name = "文件-公开读", description = "公开文件下载（无需登录，仅 public 文件）")
@RestController
@RequestMapping("/api/v1/public/files")
public class PublicFileController {

    private final FileService fileService;

    public PublicFileController(FileService fileService) {
        this.fileService = fileService;
    }

    @Operation(summary = "公开下载/预览",
            description = "仅 access=public 的文件可访问。OSS 存储时 302 到公网 URL；本地存储时流式输出。")
    @GetMapping("/{fileKey}/download")
    public ResponseEntity<?> download(
            @Parameter(description = "文件标识", required = true) @PathVariable String fileKey) {
        PublicFileView view = fileService.openPublicFile(fileKey);
        if (view.getRedirectUrl() != null) {
            return ResponseEntity.status(302).header(HttpHeaders.LOCATION, view.getRedirectUrl()).build();
        }
        return ResponseEntity.ok()
                .header(HttpHeaders.CACHE_CONTROL, "public, max-age=86400")
                .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition(view.getOriginalName()))
                .contentType(MediaType.parseMediaType(view.getContentType()))
                .body(view.getResource());
    }

    private static String contentDisposition(String filename) {
        String safe = filename == null ? "file" : filename.replace("\"", "'");
        return "inline; filename=\"" + safe + "\"";
    }
}
