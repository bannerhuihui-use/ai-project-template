package com.example.template.auth.security;

import com.example.template.common.Result;
import com.example.template.common.ResultCode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * 在过滤器 / 安全异常处理中写出统一 {@link Result} JSON。
 */
@Component
public class SecurityResponseWriter {

    private final ObjectMapper objectMapper;

    public SecurityResponseWriter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * 写出错误响应。
     *
     * @param response   响应
     * @param httpStatus HTTP 状态码
     * @param resultCode 业务错误码
     * @throws IOException 写出失败
     */
    public void writeError(HttpServletResponse response, int httpStatus, ResultCode resultCode) throws IOException {
        response.setStatus(httpStatus);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        Result<Void> body = Result.error(resultCode);
        response.getWriter().write(objectMapper.writeValueAsString(body));
    }
}
