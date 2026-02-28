package com.goaltracker.exception;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import static org.junit.jupiter.api.Assertions.*;

class ErrorCodeTest {

    @Test
    void testAllCodesHaveHttpStatus() {
        for (ErrorCode code : ErrorCode.values()) {
            assertNotNull(code.getHttpStatus(), code.name() + " HTTP status null");
            assertNotNull(code.getDefaultMessage(), code.name() + " default message null");
            assertFalse(code.getDefaultMessage().isBlank(), code.name() + " default message boş");
        }
    }

    @Test
    void testSpecificCodes() {
        assertEquals(HttpStatus.BAD_REQUEST, ErrorCode.VALIDATION_ERROR.getHttpStatus());
        assertEquals(HttpStatus.UNAUTHORIZED, ErrorCode.UNAUTHORIZED.getHttpStatus());
        assertEquals(HttpStatus.FORBIDDEN, ErrorCode.FORBIDDEN.getHttpStatus());
        assertEquals(HttpStatus.NOT_FOUND, ErrorCode.NOT_FOUND.getHttpStatus());
        assertEquals(HttpStatus.CONFLICT, ErrorCode.CONFLICT.getHttpStatus());
        assertEquals(HttpStatus.TOO_MANY_REQUESTS, ErrorCode.RATE_LIMIT_EXCEEDED.getHttpStatus());
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, ErrorCode.INTERNAL_ERROR.getHttpStatus());
    }
}

