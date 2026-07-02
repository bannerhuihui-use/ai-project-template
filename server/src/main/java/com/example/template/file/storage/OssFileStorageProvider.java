package com.example.template.file.storage;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.model.OSSObject;
import com.example.template.common.BusinessException;
import com.example.template.common.ResultCode;
import com.example.template.file.config.FileProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

import java.io.InputStream;

/**
 * 阿里云 OSS 存储实现。
 */
public class OssFileStorageProvider implements FileStorageProvider {

    private static final Logger log = LoggerFactory.getLogger(OssFileStorageProvider.class);

    private final FileProperties fileProperties;
    private final OSS ossClient;

    public OssFileStorageProvider(FileProperties fileProperties) {
        this.fileProperties = fileProperties;
        FileProperties.Oss oss = fileProperties.getOss();
        this.ossClient = new OSSClientBuilder().build(
                oss.getEndpoint(), oss.getAccessKeyId(), oss.getAccessKeySecret());
    }

    @Override
    public String storageType() {
        return "oss";
    }

    @Override
    public void store(String storagePath, String contentType, InputStream inputStream, long fileSize) {
        validatePath(storagePath);
        try {
            ossClient.putObject(fileProperties.getOss().getBucket(), storagePath, inputStream);
        } catch (Exception ex) {
            log.error("OSS 文件写入失败: storagePath={}", storagePath, ex);
            throw new BusinessException(ResultCode.SYSTEM_ERROR);
        }
    }

    @Override
    public void delete(String storagePath) {
        validatePath(storagePath);
        try {
            ossClient.deleteObject(fileProperties.getOss().getBucket(), storagePath);
        } catch (Exception ex) {
            log.warn("OSS 文件删除失败: storagePath={}", storagePath, ex);
        }
    }

    @Override
    public InputStream open(String storagePath) {
        validatePath(storagePath);
        try {
            OSSObject object = ossClient.getObject(fileProperties.getOss().getBucket(), storagePath);
            if (object == null) {
                throw new BusinessException(ResultCode.FILE_NOT_FOUND);
            }
            return object.getObjectContent();
        } catch (BusinessException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("OSS 文件读取失败: storagePath={}", storagePath, ex);
            throw new BusinessException(ResultCode.SYSTEM_ERROR);
        }
    }

    @Override
    public String resolvePublicUrl(String fileKey, String storagePath) {
        String configured = fileProperties.getOss().getPublicBaseUrl();
        if (StringUtils.hasText(configured)) {
            return trimTrailingSlash(configured) + "/" + storagePath;
        }
        FileProperties.Oss oss = fileProperties.getOss();
        String endpoint = oss.getEndpoint().replaceFirst("^https?://", "");
        return "https://" + oss.getBucket() + "." + endpoint + "/" + storagePath;
    }

    @Override
    public String resolveProtectedDownloadUrl(String fileKey) {
        return "/api/v1/files/" + fileKey + "/download";
    }

    private void validatePath(String storagePath) {
        if (!StringUtils.hasText(storagePath) || storagePath.contains("..") || storagePath.startsWith("/")) {
            throw new BusinessException(ResultCode.PARAM_ERROR);
        }
    }

    private static String trimTrailingSlash(String url) {
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }
}
