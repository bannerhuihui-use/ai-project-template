package com.example.template.logging.config;

import com.mongodb.client.MongoClient;

/**
 * 中央日志 Mongo 连接持有者；未配置 URI 时 {@link #isAvailable()} 为 false。
 */
public final class CentralLogMongoHolder {

    private final MongoClient mongoClient;

    private CentralLogMongoHolder(MongoClient mongoClient) {
        this.mongoClient = mongoClient;
    }

    public static CentralLogMongoHolder disabled() {
        return new CentralLogMongoHolder(null);
    }

    public static CentralLogMongoHolder enabled(MongoClient mongoClient) {
        return new CentralLogMongoHolder(mongoClient);
    }

    public boolean isAvailable() {
        return mongoClient != null;
    }

    public MongoClient getMongoClient() {
        return mongoClient;
    }

    public void close() {
        if (mongoClient != null) {
            mongoClient.close();
        }
    }
}
