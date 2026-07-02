package com.example.template.file.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 文件上传成功响应。
 */
@Schema(name = "FileUploadResult", description = "文件上传结果")
public class FileUploadResult {

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

    @Schema(description = "访问 URL（public 为可直接引用的地址）", example = "https://bucket.oss.com/files/image/xxx.png")
    private String url;

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
}
