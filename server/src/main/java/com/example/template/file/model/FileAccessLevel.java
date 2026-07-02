package com.example.template.file.model;

/**
 * 文件访问级别。
 */
public enum FileAccessLevel {
    /** 可通过 /api/v1/public/files/{fileKey}/download 或 OSS 公网 URL 访问。 */
    PUBLIC,
    /** 仅登录用户且具备读权限时可下载。 */
    PRIVATE;

    public static FileAccessLevel parse(String value) {
        if (value == null) {
            return null;
        }
        try {
            return FileAccessLevel.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    /** 持久化与 API 返回值（小写）。 */
    public String externalName() {
        return name().toLowerCase();
    }

    public static FileAccessLevel fromStored(String stored) {
        return parse(stored);
    }
}
