package com.example.template.file.storage;

import com.example.template.common.BusinessException;
import com.example.template.common.ResultCode;
import com.example.template.file.config.FileProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * 本地磁盘存储实现。
 */
public class LocalFileStorageProvider implements FileStorageProvider {

    private static final Logger log = LoggerFactory.getLogger(LocalFileStorageProvider.class);

    private final FileProperties fileProperties;

    public LocalFileStorageProvider(FileProperties fileProperties) {
        this.fileProperties = fileProperties;
    }

    @Override
    public String storageType() {
        return "local";
    }

    @Override
    public void store(String storagePath, String contentType, InputStream inputStream, long fileSize) {
        Path target = resolvePath(storagePath);
        try {
            Files.createDirectories(target.getParent());
            Files.copy(inputStream, target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException ex) {
            log.error("本地文件写入失败: storagePath={}", storagePath, ex);
            throw new BusinessException(ResultCode.SYSTEM_ERROR);
        }
    }

    @Override
    public void delete(String storagePath) {
        Path target = resolvePath(storagePath);
        try {
            Files.deleteIfExists(target);
        } catch (IOException ex) {
            log.warn("本地文件删除失败: storagePath={}", storagePath, ex);
        }
    }

    @Override
    public InputStream open(String storagePath) {
        Path target = resolvePath(storagePath);
        if (!Files.isRegularFile(target)) {
            throw new BusinessException(ResultCode.FILE_NOT_FOUND);
        }
        try {
            return Files.newInputStream(target);
        } catch (IOException ex) {
            log.error("本地文件读取失败: storagePath={}", storagePath, ex);
            throw new BusinessException(ResultCode.SYSTEM_ERROR);
        }
    }

    @Override
    public String resolvePublicUrl(String fileKey, String storagePath) {
        String base = trimTrailingSlash(fileProperties.getLocal().getPublicBaseUrl());
        return base + "/" + fileKey + "/download";
    }

    @Override
    public String resolveProtectedDownloadUrl(String fileKey) {
        return "/api/v1/files/" + fileKey + "/download";
    }

    private Path resolvePath(String storagePath) {
        if (!StringUtils.hasText(storagePath) || storagePath.contains("..")) {
            throw new BusinessException(ResultCode.PARAM_ERROR);
        }
        return Path.of(fileProperties.getLocal().getBaseDir()).resolve(storagePath).normalize();
    }

    private static String trimTrailingSlash(String url) {
        if (!StringUtils.hasText(url)) {
            return "";
        }
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }
}
