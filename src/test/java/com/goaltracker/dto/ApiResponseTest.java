package com.goaltracker.dto;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ApiResponseTest {

    @Test
    void testOkResponse() {
        ApiResponse<String> response = ApiResponse.ok("test data");

        assertTrue(response.isSuccess());
        assertEquals("test data", response.getData());
        assertNull(response.getErrorCode());
        assertNull(response.getMessage());
        assertNotNull(response.getTimestamp());
    }

    @Test
    void testOkResponseWithMessage() {
        ApiResponse<String> response = ApiResponse.ok("data", "Başarılı");

        assertTrue(response.isSuccess());
        assertEquals("data", response.getData());
        assertEquals("Başarılı", response.getMessage());
    }

    @Test
    void testErrorResponse() {
        ApiResponse<Void> response = ApiResponse.error("VALIDATION_ERROR", "Doğrulama hatası.");

        assertFalse(response.isSuccess());
        assertNull(response.getData());
        assertEquals("VALIDATION_ERROR", response.getErrorCode());
        assertEquals("Doğrulama hatası.", response.getMessage());
        assertNotNull(response.getTimestamp());
    }
}

