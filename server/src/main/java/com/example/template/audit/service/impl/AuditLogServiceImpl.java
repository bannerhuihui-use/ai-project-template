package com.example.template.audit.service.impl;

import com.example.template.audit.dto.AuditLogItem;
import com.example.template.audit.service.AuditLogService;
import com.example.template.common.BusinessException;
import com.example.template.common.PageResult;
import com.example.template.common.ResultCode;
import com.example.template.logging.config.CentralLogMongoHolder;
import com.example.template.logging.config.MongoLogProperties;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Sorts;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * 从 Mongo 中央日志库分页查询 WARN/ERROR 集中日志。
 */
@Service
public class AuditLogServiceImpl implements AuditLogService {

    private static final Logger log = LoggerFactory.getLogger(AuditLogServiceImpl.class);
    private static final int MAX_PAGE_SIZE = 100;
    private static final int MAX_SEARCH_LENGTH = 128;
    private static final Set<String> ALLOWED_LEVELS = Set.of("WARN", "ERROR");
    private static final String FIELD_TIMESTAMP = "timestamp";
    private static final String FIELD_MESSAGE = "message";
    private static final String FIELD_LOGGER = "logger";

    private final CentralLogMongoHolder centralLogMongoHolder;
    private final MongoLogProperties mongoLogProperties;
    private final String applicationName;

    public AuditLogServiceImpl(CentralLogMongoHolder centralLogMongoHolder,
                               MongoLogProperties mongoLogProperties,
                               @Value("${spring.application.name:template-server}") String applicationName) {
        this.centralLogMongoHolder = centralLogMongoHolder;
        this.mongoLogProperties = mongoLogProperties;
        this.applicationName = applicationName;
    }

    @Override
    public PageResult<AuditLogItem> page(int pageNum, int pageSize, String traceId, String level,
                                         String keyword, Date from, Date to) {
        int safePageNum = Math.max(pageNum, 1);
        int safePageSize = Math.min(Math.max(pageSize, 1), MAX_PAGE_SIZE);
        validateQueryParams(traceId, level, keyword, from, to);
        int offset = (safePageNum - 1) * safePageSize;

        if (!mongoLogProperties.isEnabled() || !centralLogMongoHolder.isAvailable()) {
            log.debug("中央日志未开启或 Mongo 未配置，返回空审计日志列表");
            return PageResult.of(0, safePageNum, safePageSize, List.of());
        }

        try {
            MongoDatabase database = centralLogMongoHolder.getMongoClient()
                    .getDatabase(mongoLogProperties.getDatabase());
            MongoCollection<Document> collection = database.getCollection(mongoLogProperties.getCollection());
            Bson filter = buildFilter(traceId, level, keyword, from, to);

            long total = collection.countDocuments(filter);
            List<AuditLogItem> list = collection.find(filter)
                    .sort(Sorts.descending(FIELD_TIMESTAMP))
                    .skip(offset)
                    .limit(safePageSize)
                    .map(this::toItem)
                    .into(new ArrayList<>());
            return PageResult.of(total, safePageNum, safePageSize, list);
        } catch (Exception e) {
            log.error("查询审计日志失败 app={}", applicationName, e);
            throw new BusinessException(ResultCode.SYSTEM_ERROR.getCode(), "审计日志查询失败，请稍后重试");
        }
    }

    private Bson buildFilter(String traceId, String level, String keyword, Date from, Date to) {
        List<Bson> conditions = new ArrayList<>();
        conditions.add(Filters.eq("service", applicationName));

        if (StringUtils.hasText(traceId)) {
            conditions.add(Filters.eq("traceId", traceId.trim()));
        }
        if (StringUtils.hasText(level)) {
            conditions.add(Filters.eq("level", level.trim().toUpperCase()));
        }
        if (StringUtils.hasText(keyword)) {
            String escaped = Pattern.quote(keyword.trim());
            Pattern pattern = Pattern.compile(escaped, Pattern.CASE_INSENSITIVE);
            conditions.add(Filters.or(
                    Filters.regex(FIELD_MESSAGE, pattern),
                    Filters.regex(FIELD_LOGGER, pattern)
            ));
        }
        if (from != null) {
            conditions.add(Filters.gte(FIELD_TIMESTAMP, from));
        }
        if (to != null) {
            conditions.add(Filters.lte(FIELD_TIMESTAMP, to));
        }
        return Filters.and(conditions);
    }

    private void validateQueryParams(String traceId, String level, String keyword, Date from, Date to) {
        if (StringUtils.hasText(traceId) && traceId.trim().length() > MAX_SEARCH_LENGTH) {
            throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), "traceId 长度不能超过 " + MAX_SEARCH_LENGTH);
        }
        if (StringUtils.hasText(keyword) && keyword.trim().length() > MAX_SEARCH_LENGTH) {
            throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), "关键字长度不能超过 " + MAX_SEARCH_LENGTH);
        }
        if (StringUtils.hasText(level) && !ALLOWED_LEVELS.contains(level.trim().toUpperCase())) {
            throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), "日志级别仅支持 WARN 或 ERROR");
        }
        if (from != null && to != null && from.after(to)) {
            throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), "起始时间不能晚于结束时间");
        }
    }

    private AuditLogItem toItem(Document doc) {
        AuditLogItem item = new AuditLogItem();
        ObjectId objectId = doc.getObjectId("_id");
        if (objectId != null) {
            item.setId(objectId.toHexString());
        }
        item.setService(doc.getString("service"));
        item.setEnv(doc.getString("env"));
        item.setLevel(doc.getString("level"));
        item.setTraceId(doc.getString("traceId"));
        item.setLogger(doc.getString("logger"));
        item.setMessage(doc.getString("message"));
        item.setStackTrace(doc.getString("stackTrace"));
        item.setHost(doc.getString("host"));
        item.setTimestamp(doc.getDate(FIELD_TIMESTAMP));
        return item;
    }
}
