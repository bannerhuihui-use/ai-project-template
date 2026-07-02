package com.example.template.file.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.HashMap;
import java.util.Map;

/**
 * 文件上传模块配置（application.yml 前缀 file.*）。
 */
@ConfigurationProperties(prefix = "file")
public class FileProperties {

    /** 存储类型：local | oss。 */
    private String storageType = "local";

    private final Local local = new Local();

    private final Upload upload = new Upload();

    private final Oss oss = new Oss();

    public String getStorageType() {
        return storageType;
    }

    public void setStorageType(String storageType) {
        this.storageType = storageType;
    }

    public Local getLocal() {
        return local;
    }

    public Upload getUpload() {
        return upload;
    }

    public Oss getOss() {
        return oss;
    }

    public static class Local {
        private String baseDir = "./data/uploads";
        private String publicBaseUrl = "http://localhost:8080/api/v1/public/files";

        public String getBaseDir() {
            return baseDir;
        }

        public void setBaseDir(String baseDir) {
            this.baseDir = baseDir;
        }

        public String getPublicBaseUrl() {
            return publicBaseUrl;
        }

        public void setPublicBaseUrl(String publicBaseUrl) {
            this.publicBaseUrl = publicBaseUrl;
        }
    }

    public static class Upload {
        private Map<String, Long> maxSizeBytes = defaultMaxSizes();
        private Map<String, String> allowedContentTypes = defaultAllowedTypes();

        public Map<String, Long> getMaxSizeBytes() {
            return maxSizeBytes;
        }

        public void setMaxSizeBytes(Map<String, Long> maxSizeBytes) {
            this.maxSizeBytes = maxSizeBytes;
        }

        public Map<String, String> getAllowedContentTypes() {
            return allowedContentTypes;
        }

        public void setAllowedContentTypes(Map<String, String> allowedContentTypes) {
            this.allowedContentTypes = allowedContentTypes;
        }

        private static Map<String, Long> defaultMaxSizes() {
            Map<String, Long> map = new HashMap<>();
            map.put("avatar", 2_097_152L);
            map.put("image", 5_242_880L);
            map.put("document", 10_485_760L);
            map.put("attachment", 20_971_520L);
            return map;
        }

        private static Map<String, String> defaultAllowedTypes() {
            Map<String, String> map = new HashMap<>();
            map.put("avatar", "image/jpeg,image/png,image/gif,image/webp");
            map.put("image", "image/jpeg,image/png,image/gif,image/webp");
            map.put("document",
                    "application/pdf,application/msword,application/vnd.openxmlformats-officedocument.wordprocessingml.document");
            map.put("attachment",
                    "application/pdf,application/msword,application/vnd.openxmlformats-officedocument.wordprocessingml.document,application/zip,application/x-zip-compressed,text/plain");
            return map;
        }
    }

    public static class Oss {
        private String endpoint;
        private String accessKeyId;
        private String accessKeySecret;
        private String bucket;
        /** 自定义域名或 bucket 公网访问前缀；为空时按 bucket+endpoint 拼接。 */
        private String publicBaseUrl;

        public String getEndpoint() {
            return endpoint;
        }

        public void setEndpoint(String endpoint) {
            this.endpoint = endpoint;
        }

        public String getAccessKeyId() {
            return accessKeyId;
        }

        public void setAccessKeyId(String accessKeyId) {
            this.accessKeyId = accessKeyId;
        }

        public String getAccessKeySecret() {
            return accessKeySecret;
        }

        public void setAccessKeySecret(String accessKeySecret) {
            this.accessKeySecret = accessKeySecret;
        }

        public String getBucket() {
            return bucket;
        }

        public void setBucket(String bucket) {
            this.bucket = bucket;
        }

        public String getPublicBaseUrl() {
            return publicBaseUrl;
        }

        public void setPublicBaseUrl(String publicBaseUrl) {
            this.publicBaseUrl = publicBaseUrl;
        }
    }
}
