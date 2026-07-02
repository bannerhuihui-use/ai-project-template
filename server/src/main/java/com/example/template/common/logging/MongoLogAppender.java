package com.example.template.common.logging;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;
import ch.qos.logback.classic.spi.ThrowableProxyUtil;
import ch.qos.logback.core.UnsynchronizedAppenderBase;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.Indexes;
import org.bson.Document;

import java.net.InetAddress;
import java.util.Date;
import java.util.concurrent.TimeUnit;

/**
 * 将日志写入中央 MongoDB 的自定义 logback Appender。
 *
 * <p>设计要点（详见 docs/logging/README.md）：</p>
 * <ul>
 *     <li><b>默认关闭</b>：{@code enabled=false} 或 {@code uri} 为空时为 inert（不连库、不写入）。</li>
 *     <li><b>自持连接</b>：logback 早于 Spring 容器初始化，无法注入 MongoTemplate，故 appender 自建 {@link MongoClient}。</li>
 *     <li><b>防递归</b>：忽略 mongodb 驱动自身日志，避免“写库 → 产生驱动日志 → 又写库”的死循环。</li>
 *     <li><b>不阻塞业务</b>：本 appender 外层由 AsyncAppender(neverBlock) 包裹；内部任何异常只记 status，绝不抛出。</li>
 *     <li><b>自动过期</b>：启动时对 timestamp 建 TTL 索引，按 ttlDays 自动清理。</li>
 * </ul>
 *
 * <p>属性由 logback-spring.xml 注入（setter）。</p>
 */
public class MongoLogAppender extends UnsynchronizedAppenderBase<ILoggingEvent> {

    private static final String TRACE_ID_KEY = "traceId";
    private static final String NONE = "-";
    private static final String MONGO_DRIVER_LOGGER_PREFIX = "org.mongodb";
    private static final long SECONDS_PER_DAY = 86400L;

    private boolean enabled;
    private String uri;
    private String database;
    private String collection;
    private int ttlDays = 30;
    private String service;
    private String env;

    private volatile boolean active;
    private volatile MongoClient mongoClient;
    private volatile MongoCollection<Document> logCollection;
    private String host;

    @Override
    public void start() {
        this.host = resolveHost();
        this.active = enabled && isNotBlank(uri) && isNotBlank(database) && isNotBlank(collection);
        if (!active) {
            addInfo("MongoLogAppender 未启用（enabled=" + enabled + "），日志不会写入 MongoDB。");
        }
        super.start();
    }

    @Override
    protected void append(ILoggingEvent event) {
        if (!active) {
            return;
        }
        String loggerName = event.getLoggerName();
        if (loggerName != null && loggerName.startsWith(MONGO_DRIVER_LOGGER_PREFIX)) {
            return;
        }
        try {
            MongoCollection<Document> target = collection();
            if (target == null) {
                return;
            }
            target.insertOne(toDocument(event));
        } catch (Exception e) {
            addWarn("写入 MongoDB 日志失败，已跳过该条（业务不受影响）：" + e.getMessage());
        }
    }

    /**
     * 懒初始化并返回日志集合；首次连接时创建索引与 TTL。
     *
     * @return 日志集合，初始化失败返回 null
     */
    private MongoCollection<Document> collection() {
        MongoCollection<Document> local = logCollection;
        if (local != null) {
            return local;
        }
        synchronized (this) {
            if (logCollection == null) {
                mongoClient = MongoClients.create(uri);
                MongoCollection<Document> created = mongoClient.getDatabase(database).getCollection(collection);
                ensureIndexes(created);
                logCollection = created;
            }
            return logCollection;
        }
    }

    /**
     * 创建查询索引与 TTL 索引（幂等）。
     *
     * @param target 日志集合
     */
    private void ensureIndexes(MongoCollection<Document> target) {
        target.createIndex(Indexes.descending("service", "timestamp"));
        target.createIndex(Indexes.ascending("traceId"));
        target.createIndex(Indexes.descending("level", "timestamp"));
        long expireSeconds = Math.max(ttlDays, 1) * SECONDS_PER_DAY;
        target.createIndex(Indexes.ascending("timestamp"),
                new IndexOptions().expireAfter(expireSeconds, TimeUnit.SECONDS));
    }

    /**
     * 将日志事件转换为 Mongo 文档。
     *
     * @param event 日志事件
     * @return 文档
     */
    private Document toDocument(ILoggingEvent event) {
        String traceId = event.getMDCPropertyMap().getOrDefault(TRACE_ID_KEY, NONE);
        Document doc = new Document();
        doc.append("service", orNone(service));
        doc.append("env", orNone(env));
        doc.append("level", event.getLevel() == null ? NONE : event.getLevel().toString());
        doc.append("traceId", isNotBlank(traceId) ? traceId : NONE);
        doc.append("logger", orNone(event.getLoggerName()));
        doc.append("thread", orNone(event.getThreadName()));
        doc.append("message", sanitize(event.getFormattedMessage()));
        doc.append("stackTrace", stackTrace(event.getThrowableProxy()));
        doc.append("host", host);
        doc.append("timestamp", new Date(event.getTimeStamp()));
        return doc;
    }

    private String stackTrace(IThrowableProxy throwableProxy) {
        return throwableProxy == null ? null : ThrowableProxyUtil.asString(throwableProxy);
    }

    private String sanitize(String value) {
        if (value == null) {
            return null;
        }
        return value.replaceAll("[\\r\\n]+", " ");
    }

    private String resolveHost() {
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (Exception e) {
            return NONE;
        }
    }

    private String orNone(String value) {
        return isNotBlank(value) ? value : NONE;
    }

    private boolean isNotBlank(String value) {
        return value != null && !value.trim().isEmpty();
    }

    @Override
    public void stop() {
        if (mongoClient != null) {
            try {
                mongoClient.close();
            } catch (Exception e) {
                addWarn("关闭 MongoLogAppender 的 MongoClient 失败：" + e.getMessage());
            } finally {
                mongoClient = null;
                logCollection = null;
            }
        }
        super.stop();
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    public void setDatabase(String database) {
        this.database = database;
    }

    public void setCollection(String collection) {
        this.collection = collection;
    }

    public void setTtlDays(int ttlDays) {
        this.ttlDays = ttlDays;
    }

    public void setService(String service) {
        this.service = service;
    }

    public void setEnv(String env) {
        this.env = env;
    }
}
