package com.example.template.common;

import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void handleAccessDenied_returns403() {
        Result<Void> result = handler.handleAccessDenied(new AccessDeniedException("denied"));
        assertEquals(ResultCode.FORBIDDEN.getCode(), result.getCode());
    }
}
