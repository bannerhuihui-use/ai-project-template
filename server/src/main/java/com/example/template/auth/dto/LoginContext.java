package com.example.template.auth.dto;

/**
 * 登录上下文：由 Controller 从请求中提取的客户端信息，供登录设备记录使用。
 *
 * @param ip        客户端 IP（取 remoteAddr，避免伪造 X-Forwarded-For）
 * @param userAgent User-Agent（已截断/清洗）
 */
public record LoginContext(String ip, String userAgent) {
}
