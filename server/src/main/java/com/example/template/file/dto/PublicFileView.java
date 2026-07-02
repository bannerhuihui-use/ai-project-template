package com.example.template.file.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.core.io.Resource;

/**
 * 公开文件访问视图（Controller 层使用）。
 */
@Schema(name = "PublicFileView", description = "公开文件访问结果")
public class PublicFileView {

    private String originalName;
    private String contentType;
    private Resource resource;
    /** OSS 公网地址；非空时 Controller 应 302 重定向。 */
    private String redirectUrl;

    public PublicFileView() {
    }

    public PublicFileView(String originalName, String contentType, Resource resource, String redirectUrl) {
        this.originalName = originalName;
        this.contentType = contentType;
        this.resource = resource;
        this.redirectUrl = redirectUrl;
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

    public Resource getResource() {
        return resource;
    }

    public void setResource(Resource resource) {
        this.resource = resource;
    }

    public String getRedirectUrl() {
        return redirectUrl;
    }

    public void setRedirectUrl(String redirectUrl) {
        this.redirectUrl = redirectUrl;
    }
}
