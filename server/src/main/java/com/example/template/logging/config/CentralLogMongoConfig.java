package com.example.template.logging.config;

import com.mongodb.ConnectionString;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.mongo.MongoProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

/**
 * 中央日志库 MongoClient（与业务库分离，复用 {@code logging.mongo.*} 配置）。
 */
@Configuration
@EnableConfigurationProperties(MongoLogProperties.class)
public class CentralLogMongoConfig {

    private static final Logger log = LoggerFactory.getLogger(CentralLogMongoConfig.class);

    @Bean(destroyMethod = "close")
    public CentralLogMongoHolder centralLogMongoHolder(MongoLogProperties mongoLogProperties,
                                                       MongoProperties springMongoProperties) {
        String uri = resolveUri(mongoLogProperties, springMongoProperties);
        if (!StringUtils.hasText(uri)) {
            log.warn("中央日志 Mongo URI 未配置，审计日志查询将返回空结果");
            return CentralLogMongoHolder.disabled();
        }
        try {
            new ConnectionString(uri);
            return CentralLogMongoHolder.enabled(MongoClients.create(uri));
        } catch (Exception e) {
            log.error("中央日志 Mongo URI 无效，审计日志查询将不可用", e);
            return CentralLogMongoHolder.disabled();
        }
    }

    /**
     * 优先使用 LOG_MONGO_URI；未配置时回退到 spring.data.mongodb 连接信息拼装 URI。
     */
    static String resolveUri(MongoLogProperties mongoLogProperties, MongoProperties springMongoProperties) {
        if (StringUtils.hasText(mongoLogProperties.getUri())) {
            return mongoLogProperties.getUri().trim();
        }
        String host = springMongoProperties.getHost();
        if (!StringUtils.hasText(host)) {
            return null;
        }
        int port = springMongoProperties.getPort() == null ? 27017 : springMongoProperties.getPort();
        String username = springMongoProperties.getUsername();
        String password = springMongoProperties.getPassword() == null
                ? null
                : new String(springMongoProperties.getPassword());
        String authDb = StringUtils.hasText(springMongoProperties.getAuthenticationDatabase())
                ? springMongoProperties.getAuthenticationDatabase()
                : "admin";
        if (StringUtils.hasText(username) && StringUtils.hasText(password)) {
            return String.format("mongodb://%s:%s@%s:%d/?authSource=%s",
                    urlEncode(username), urlEncode(password), host, port, authDb);
        }
        return String.format("mongodb://%s:%d", host, port);
    }

    private static String urlEncode(String value) {
        return value.replace("@", "%40").replace(":", "%3A");
    }
}
