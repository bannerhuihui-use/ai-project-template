package com.example.template.file.storage;

import java.io.InputStream;

/**
 * 文件存储抽象：本地磁盘或 OSS。
 */
public interface FileStorageProvider {

    /** 当前存储类型标识：local / oss。 */
    String storageType();

    /**
     * 写入文件。
     *
     * @param storagePath  相对路径或 OSS object key
     * @param contentType  MIME
     * @param inputStream  内容流（由调用方关闭）
     * @param fileSize     字节数
     */
    void store(String storagePath, String contentType, InputStream inputStream, long fileSize);

    /** 删除物理文件。 */
    void delete(String storagePath);

    /**
     * 打开只读流（用于受保护下载）。
     *
     * @return 流由调用方关闭
     */
    InputStream open(String storagePath);

    /**
     * 解析 public 文件的直接访问 URL（OSS 公网地址或 API 公开下载地址）。
     */
    String resolvePublicUrl(String fileKey, String storagePath);

    /**
     * 解析受保护下载 API 地址（private 文件给前端展示用）。
     */
    String resolveProtectedDownloadUrl(String fileKey);
}
