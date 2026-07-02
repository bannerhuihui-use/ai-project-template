package com.example.template.auth.security;

import com.example.template.common.ResultCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * 已认证但无权限（如非 ADMIN 访问管理员接口）时返回统一 Result。
 */
@Component
public class RestAccessDeniedHandler implements AccessDeniedHandler {

    private final SecurityResponseWriter responseWriter;

    public RestAccessDeniedHandler(SecurityResponseWriter responseWriter) {
        this.responseWriter = responseWriter;
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException {
        responseWriter.writeError(response, HttpServletResponse.SC_FORBIDDEN, ResultCode.FORBIDDEN);
    }
}
