package com.ai.cs.shared.exception;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ErrorCodeTest {

    @Test
    void successShouldHaveCode0() {
        assertEquals(0, ErrorCode.SUCCESS.getCode());
        assertEquals("成功", ErrorCode.SUCCESS.getMessage());
    }

    @Test
    void unauthorizedShouldHaveCode401() {
        assertEquals(401, ErrorCode.UNAUTHORIZED.getCode());
        assertEquals("未授权", ErrorCode.UNAUTHORIZED.getMessage());
    }

    @Test
    void aiEmployeeNotFoundShouldHaveCode1001() {
        assertEquals(1001, ErrorCode.AI_EMPLOYEE_NOT_FOUND.getCode());
        assertEquals("AI员工不存在", ErrorCode.AI_EMPLOYEE_NOT_FOUND.getMessage());
    }

    @Test
    void noAvailableAgentShouldHaveCode1009() {
        assertEquals(1009, ErrorCode.NO_AVAILABLE_AGENT.getCode());
        assertEquals("无可用客服", ErrorCode.NO_AVAILABLE_AGENT.getMessage());
    }
}
