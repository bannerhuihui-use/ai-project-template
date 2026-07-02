package com.example.template.logging.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 中央日志 Mongo 配置，与 logback MongoLogAppender 共用 {@code logging.mongo.*} 前缀。
 */
@ConfigurationProperties(prefix = "logging.mongo")
public class MongoLogProperties {

    private boolean enabled;
    private String uri = "";
    private String database = "central_logs";
    private String collection = "app_logs";
    private int ttlDays = 30;
    private int queueSize = 2048;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    public String getDatabase() {
        return database;
    }

    public void setDatabase(String database) {
        this.database = database;
    }

    public String getCollection() {
        return collection;
    }

    public void setCollection(String collection) {
        this.collection = collection;
    }

    public int getTtlDays() {
        return ttlDays;
    }

    public void setTtlDays(int ttlDays) {
        this.ttlDays = ttlDays;
    }

    public int getQueueSize() {
        return queueSize;
    }

    public void setQueueSize(int queueSize) {
        this.queueSize = queueSize;
    }
}
