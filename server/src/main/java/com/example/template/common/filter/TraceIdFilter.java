package com.example.template.common.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * 链路追踪过滤器：为每个请求建立 traceId 并贯穿日志。
 *
 * <p>安全要点：</p>
 * <ul>
 *     <li>traceId 来源于客户端头 {@code X-Trace-Id}，必须做白名单校验（仅字母数字和连字符、限长），
 *     防止 CRLF 日志注入与响应头注入；非法值直接丢弃并改为服务端生成。</li>
 *     <li>请求结束后 {@link MDC#clear()}，避免线程池复用导致 traceId 串号/泄漏。</li>
 * </ul>
 *
 * <p>顺序：必须早于 {@link AccessLogFilter}，保证访问日志可拿到 traceId。</p>
 */
@Order(TraceIdFilter.ORDER)
@Component
public class TraceIdFilter extends OncePerRequestFilter {

    /** 过滤器顺序，越小越先执行。 */
    public static final int ORDER = Ordered.HIGHEST_PRECEDENCE + 10;

    /** MDC 中 traceId 的 key，与 logback pattern 中的 %X{traceId} 一致。 */
    public static final String MDC_KEY = "traceId";

    /** 链路追踪请求/响应头名称。 */
    public static final String TRACE_ID_HEADER = "X-Trace-Id";

    /** traceId 合法字符白名单：字母、数字、连字符，长度 1~64。 */
    private static final Pattern TRACE_ID_PATTERN = Pattern.compile("^[A-Za-z0-9-]{1,64}$");

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String traceId = resolveTraceId(request.getHeader(TRACE_ID_HEADER));
        MDC.put(MDC_KEY, traceId);
        response.setHeader(TRACE_ID_HEADER, traceId);
        try {
            filterChain.doFilter(request, response);
        } finally {
            MDC.clear();
        }
    }

    /**
     * 解析 traceId：合法则复用客户端传入值，否则生成新的 UUID（无连字符）。
     *
     * @param headerValue 客户端传入的 X-Trace-Id
     * @return 经校验的安全 traceId
     */
    private String resolveTraceId(String headerValue) {
        if (StringUtils.hasText(headerValue) && TRACE_ID_PATTERN.matcher(headerValue).matches()) {
            return headerValue;
        }
        return UUID.randomUUID().toString().replace("-", "");
    }
}
