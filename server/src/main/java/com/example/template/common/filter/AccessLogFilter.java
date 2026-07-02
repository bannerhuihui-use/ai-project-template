package com.example.template.common.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * 访问日志过滤器：记录每次请求的方法、URI、状态码、耗时、客户端 IP、User-Agent、traceId。
 *
 * <p>安全要点：</p>
 * <ul>
 *     <li>仅记录白名单字段，<b>绝不</b>打印 Authorization、Cookie 等敏感头。</li>
 *     <li>对 User-Agent 做换行清洗，防止日志注入伪造行。</li>
 *     <li>客户端 IP 默认取 {@code getRemoteAddr()}；如部署在可信反向代理后需要真实 IP，
 *     应在网关层校验后再启用 X-Forwarded-For，避免来源 IP 被伪造。</li>
 * </ul>
 *
 * <p>顺序：晚于 {@link TraceIdFilter}，确保能拿到 traceId。</p>
 */
@Order(TraceIdFilter.ORDER + 10)
@Component
public class AccessLogFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(AccessLogFilter.class);

    private static final String UNKNOWN = "-";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        long start = System.currentTimeMillis();
        try {
            filterChain.doFilter(request, response);
        } finally {
            long cost = System.currentTimeMillis() - start;
            String query = request.getQueryString();
            String uri = query == null ? request.getRequestURI() : request.getRequestURI() + "?" + query;
            log.info("ACCESS method={} uri={} status={} cost={}ms ip={} ua={}",
                    request.getMethod(),
                    sanitize(uri),
                    response.getStatus(),
                    cost,
                    clientIp(request),
                    sanitize(request.getHeader("User-Agent")));
        }
    }

    /**
     * 获取客户端 IP。默认使用 {@code getRemoteAddr()}，安全且不可被请求头伪造。
     *
     * @param request 当前请求
     * @return 客户端 IP
     */
    private String clientIp(HttpServletRequest request) {
        String remoteAddr = request.getRemoteAddr();
        return StringUtils.hasText(remoteAddr) ? remoteAddr : UNKNOWN;
    }

    /**
     * 清洗用户可控字段中的换行符，防止日志注入。
     *
     * @param value 原始值
     * @return 清洗后的值
     */
    private String sanitize(String value) {
        if (!StringUtils.hasText(value)) {
            return UNKNOWN;
        }
        return value.replaceAll("[\\r\\n]+", " ");
    }
}
