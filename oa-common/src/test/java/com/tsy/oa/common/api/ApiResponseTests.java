package com.tsy.oa.common.api;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class ApiResponseTests {

    @Test
    void successResponseUsesStandardValues() {
        ApiResponse<String> response = ApiResponse.success("user-service");

        assertEquals(0, response.code());
        assertEquals("success", response.message());
        assertEquals("user-service", response.data());
    }

    @Test
    void failureResponseUsesProvidedCodeAndMessage() {
        ApiResponse<Void> response = ApiResponse.failure(40001, "invalid request");

        assertEquals(40001, response.code());
        assertEquals("invalid request", response.message());
        assertNull(response.data());
    }
}
