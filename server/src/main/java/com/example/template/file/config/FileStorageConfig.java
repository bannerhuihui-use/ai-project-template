package com.example.template.file.config;

import com.example.template.file.storage.FileStorageProvider;
import com.example.template.file.storage.LocalFileStorageProvider;
import com.example.template.file.storage.OssFileStorageProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 按 file.storage-type 装配存储实现。
 */
@Configuration
@EnableConfigurationProperties(FileProperties.class)
public class FileStorageConfig {

    @Bean
    @ConditionalOnProperty(name = "file.storage-type", havingValue = "local", matchIfMissing = true)
    public FileStorageProvider localFileStorageProvider(FileProperties fileProperties) {
        return new LocalFileStorageProvider(fileProperties);
    }

    @Bean
    @ConditionalOnProperty(name = "file.storage-type", havingValue = "oss")
    public FileStorageProvider ossFileStorageProvider(FileProperties fileProperties) {
        return new OssFileStorageProvider(fileProperties);
    }
}
