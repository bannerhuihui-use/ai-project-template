package com.example.template.common;

import com.example.template.common.filter.TraceIdFilter;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * 全局异常处理，统一以 {@link Result} 返回。
 *
 * <p>日志约定：参数异常 / 业务异常用 warn（可预期、可恢复），系统异常用 error（需告警排查）。
 * 每条异常日志均显式带上 traceId，便于按 X-Trace-Id 串联整条请求链路。</p>
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler({AccessDeniedException.class, AuthorizationDeniedException.class})
    public Result<Void> handleAccessDenied(Exception e) {
        log.warn("无权限访问: traceId={}, message={}", currentTraceId(), e.getMessage());
        return Result.error(ResultCode.FORBIDDEN);
    }

    @ExceptionHandler(BusinessException.class)
    public Result<Void> handleBusinessException(BusinessException e) {
        log.warn("业务异常: traceId={}, code={}, message={}", currentTraceId(), e.getCode(), e.getMessage());
        return Result.error(e.getCode(), e.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<Void> handleValidationException(MethodArgumentNotValidException e) {
        FieldError fieldError = e.getBindingResult().getFieldError();
        String message = fieldError == null ? ResultCode.PARAM_ERROR.getMessage() : fieldError.getDefaultMessage();
        log.warn("参数校验失败: traceId={}, message={}", currentTraceId(), message);
        return Result.error(ResultCode.PARAM_ERROR.getCode(), message);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public Result<Void> handleConstraintViolation(ConstraintViolationException e) {
        String message = e.getConstraintViolations().stream()
                .map(ConstraintViolation::getMessage)
                .findFirst()
                .orElse(ResultCode.PARAM_ERROR.getMessage());
        log.warn("参数约束校验失败: traceId={}, message={}", currentTraceId(), message);
        return Result.error(ResultCode.PARAM_ERROR.getCode(), message);
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    public Result<Void> handleHandlerMethodValidation(HandlerMethodValidationException e) {
        String message = e.getAllValidationResults().stream()
                .flatMap(result -> result.getResolvableErrors().stream())
                .map(error -> error.getDefaultMessage() == null ? ResultCode.PARAM_ERROR.getMessage()
                        : error.getDefaultMessage())
                .findFirst()
                .orElse(ResultCode.PARAM_ERROR.getMessage());
        log.warn("方法参数校验失败: traceId={}, message={}", currentTraceId(), message);
        return Result.error(ResultCode.PARAM_ERROR.getCode(), message);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public Result<Void> handleNoResourceFound(NoResourceFoundException e) {
        log.warn("资源不存在: traceId={}, path={}", currentTraceId(), e.getResourcePath());
        return Result.error(ResultCode.NOT_FOUND);
    }

    @ExceptionHandler(Exception.class)
    public Result<Void> handleException(Exception e) {
        log.error("系统异常: traceId={}", currentTraceId(), e);
        return Result.error(ResultCode.SYSTEM_ERROR);
    }

    /**
     * 获取当前请求的 traceId，无则返回占位符。
     *
     * @return traceId
     */
    private String currentTraceId() {
        String traceId = MDC.get(TraceIdFilter.MDC_KEY);
        return traceId == null ? "-" : traceId;
    }
}
