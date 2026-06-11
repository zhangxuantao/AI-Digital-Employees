package com.ai.cs.shared.dto;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ApiResponseTest {

    @Test
    void successWithDataShouldReturnCorrectResponse() {
        ApiResponse<String> response = ApiResponse.success("hello");
        assertEquals(0, response.getCode());
        assertEquals("success", response.getMessage());
        assertEquals("hello", response.getData());
    }

    @Test
    void successWithoutDataShouldReturnNullData() {
        ApiResponse<String> response = ApiResponse.success();
        assertEquals(0, response.getCode());
        assertEquals("success", response.getMessage());
        assertNull(response.getData());
    }

    @Test
    void errorShouldReturnCorrectCodeAndMessage() {
        ApiResponse<String> response = ApiResponse.error(404, "not found");
        assertEquals(404, response.getCode());
        assertEquals("not found", response.getMessage());
        assertNull(response.getData());
    }
}
