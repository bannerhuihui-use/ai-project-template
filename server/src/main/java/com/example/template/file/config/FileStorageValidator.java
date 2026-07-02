package com.example.template.file.config;

import com.example.template.common.BusinessException;
import com.example.template.common.ResultCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 启动时校验 file.storage-type=oss 时的必要配置。
 */
@Component
public class FileStorageValidator {

    private static final Logger log = LoggerFactory.getLogger(FileStorageValidator.class);

    private final FileProperties fileProperties;

    public FileStorageValidator(FileProperties fileProperties) {
        this.fileProperties = fileProperties;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void validateOnReady() {
        if (!"oss".equalsIgnoreCase(fileProperties.getStorageType())) {
            return;
        }
        FileProperties.Oss oss = fileProperties.getOss();
        if (!StringUtils.hasText(oss.getEndpoint())
                || !StringUtils.hasText(oss.getAccessKeyId())
                || !StringUtils.hasText(oss.getAccessKeySecret())
                || !StringUtils.hasText(oss.getBucket())) {
            log.error("file.storage-type=oss 但 OSS 配置不完整，请检查 OSS_ENDPOINT/OSS_ACCESS_KEY_ID/OSS_ACCESS_KEY_SECRET/OSS_BUCKET");
            throw new BusinessException(ResultCode.FILE_STORAGE_CONFIG_INVALID);
        }
        log.info("文件存储已启用 OSS: bucket={}", oss.getBucket());
    }
}
