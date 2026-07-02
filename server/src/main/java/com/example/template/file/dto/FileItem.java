package com.example.template.file.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 文件元数据 DTO。
 */
@Schema(name = "FileItem", description = "文件元数据")
public class FileItem {

    @Schema(description = "对外文件标识", example = "a1b2c3d4e5f6478990abcdef12345678")
    private String fileKey;

    @Schema(description = "原始文件名", example = "logo.png")
    private String originalName;

    @Schema(description = "MIME 类型", example = "image/png")
    private String contentType;

    @Schema(description = "文件大小（字节）", example = "12345")
    private long fileSize;

    @Schema(description = "业务分类", example = "image")
    private String bizType;

    @Schema(description = "访问级别", example = "public")
    private String accessLevel;

    @Schema(description = "公开访问 URL（public 文件）或受保护下载 URL", example = "https://example.com/api/v1/public/files/xxx/download")
    private String url;

    @Schema(description = "上传时间（ISO-8601）", example = "2026-07-01T10:00:00Z")
    private String createdAt;

    public String getFileKey() {
        return fileKey;
    }

    public void setFileKey(String fileKey) {
        this.fileKey = fileKey;
    }

    public String getOriginalName() {
        return originalName;
    }

    public void setOriginalName(String originalName) {
        this.originalName = originalName;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public long getFileSize() {
        return fileSize;
    }

    public void setFileSize(long fileSize) {
        this.fileSize = fileSize;
    }

    public String getBizType() {
        return bizType;
    }

    public void setBizType(String bizType) {
        this.bizType = bizType;
    }

    public String getAccessLevel() {
        return accessLevel;
    }

    public void setAccessLevel(String accessLevel) {
        this.accessLevel = accessLevel;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }
}
