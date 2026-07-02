package com.example.template.common.logging;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import org.bson.Document;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * MongoLogAppender 集成测试：验证日志确实写入 MongoDB。
 *
 * <p>依赖本地可达的 MongoDB（默认 mongodb://localhost:27017）。标记为 {@code integration}，
 * 默认 {@code mvn test} 不执行（surefire 已排除该分组，避免无中间件时等待连接超时）；
 * 需要时用 {@code mvn test -Dgroups=integration} 单独运行。
 * 可用系统属性 -Dtest.mongo.uri 或环境变量 TEST_MONGO_URI 覆盖连接串。</p>
 */
@Tag("integration")
class MongoLogAppenderTest {

    private static final String MONGO_URI = resolveMongoUri();
    private static final String DB = "central_logs_it";
    private static final String COLLECTION = "app_logs_it";
    private static final String TRACE_ID = "it-trace-001";

    @Test
    void shouldWriteWarnAndErrorLogsIntoMongo() {
        assumeTrue(mongoReachable(), "本地 MongoDB 不可达，跳过该集成测试");

        dropTestDatabase();

        LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
        MongoLogAppender appender = newAppender(context);
        appender.start();

        Logger logger = context.getLogger("com.example.template.it.MongoLogAppenderTest");
        logger.setLevel(Level.DEBUG);
        logger.setAdditive(false);
        logger.addAppender(appender);

        try {
            MDC.put("traceId", TRACE_ID);
            logger.warn("集成测试 WARN 日志");
            logger.error("集成测试 ERROR 日志", new RuntimeException("boom for test"));
        } finally {
            MDC.clear();
            logger.detachAppender(appender);
            appender.stop();
        }

        verifyStored();
        dropTestDatabase();
    }

    private MongoLogAppender newAppender(LoggerContext context) {
        MongoLogAppender appender = new MongoLogAppender();
        appender.setContext(context);
        appender.setEnabled(true);
        appender.setUri(MONGO_URI);
        appender.setDatabase(DB);
        appender.setCollection(COLLECTION);
        appender.setTtlDays(30);
        appender.setService("template-server");
        appender.setEnv("test");
        return appender;
    }

    private void verifyStored() {
        try (MongoClient client = MongoClients.create(MONGO_URI)) {
            MongoCollection<Document> collection = client.getDatabase(DB).getCollection(COLLECTION);

            assertEquals(2, collection.countDocuments(), "应写入 2 条日志（WARN + ERROR）");

            Document error = collection.find(new Document("level", "ERROR")).first();
            assertNotNull(error, "应能查到 ERROR 日志");
            assertEquals(TRACE_ID, error.getString("traceId"), "traceId 应正确入库");
            assertEquals("template-server", error.getString("service"), "service 应正确入库");
            assertEquals("test", error.getString("env"));
            assertNotNull(error.getString("stackTrace"), "ERROR 日志应包含异常堆栈");
            assertNotNull(error.getDate("timestamp"), "timestamp 应为日期类型（TTL 依赖）");

            boolean hasTtlIndex = false;
            for (Document index : collection.listIndexes()) {
                if (index.containsKey("expireAfterSeconds")) {
                    hasTtlIndex = true;
                    break;
                }
            }
            assertTrue(hasTtlIndex, "应创建 TTL 索引用于自动过期");
        }
    }

    /**
     * 解析测试用 Mongo 连接串：优先环境变量 TEST_MONGO_URI，其次系统属性 test.mongo.uri，
     * 最后回退本地无认证默认值。凭据通过环境变量传入，避免出现在命令行/源码中。
     *
     * @return Mongo 连接串
     */
    private static String resolveMongoUri() {
        String fromEnv = System.getenv("TEST_MONGO_URI");
        if (fromEnv != null && !fromEnv.trim().isEmpty()) {
            return fromEnv;
        }
        return System.getProperty("test.mongo.uri", "mongodb://localhost:27017");
    }

    private boolean mongoReachable() {
        try (MongoClient client = MongoClients.create(MONGO_URI)) {
            // 执行一次需要授权的真实操作：不可达或未授权（无凭据）都视为不可测，跳过而非失败
            client.getDatabase(DB).getCollection(COLLECTION).estimatedDocumentCount();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private void dropTestDatabase() {
        try (MongoClient client = MongoClients.create(MONGO_URI)) {
            client.getDatabase(DB).drop();
        } catch (Exception ignored) {
            // 清理失败不影响结论
        }
    }
}
