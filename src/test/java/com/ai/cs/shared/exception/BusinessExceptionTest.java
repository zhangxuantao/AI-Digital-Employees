package com.ai.cs.shared.exception;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class BusinessExceptionTest {

    @Test
    void constructorWithErrorCodeShouldSetCodeAndMessage() {
        BusinessException ex = new BusinessException(ErrorCode.BAD_REQUEST);
        assertEquals(400, ex.getCode());
        assertEquals("请求参数错误", ex.getMessage());
    }

    @Test
    void constructorWithCustomMessageShouldOverrideDefault() {
        BusinessException ex = new BusinessException(ErrorCode.BAD_REQUEST, "自定义错误");
        assertEquals(400, ex.getCode());
        assertEquals("自定义错误", ex.getMessage());
    }
}
