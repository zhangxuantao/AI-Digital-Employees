package com.ai.cs.shared.exception;

import lombok.Getter;

@Getter
public enum ErrorCode {
    SUCCESS(0, "成功"),
    BAD_REQUEST(400, "请求参数错误"),
    UNAUTHORIZED(401, "未授权"),
    FORBIDDEN(403, "无权限"),
    NOT_FOUND(404, "资源不存在"),
    CONFLICT(409, "资源冲突"),
    INTERNAL_ERROR(500, "服务器内部错误"),

    AI_EMPLOYEE_NOT_FOUND(1001, "AI员工不存在"),
    KNOWLEDGE_BASE_NOT_FOUND(1002, "知识库不存在"),
    CONVERSATION_NOT_FOUND(1003, "会话不存在"),
    AGENT_NOT_FOUND(1004, "客服不存在"),
    AGENT_OFFLINE(1005, "客服不在线"),
    AGENT_OVERLOAD(1006, "客服负载已满"),
    STRATEGY_NOT_FOUND(1007, "策略配置不存在"),
    DOCUMENT_PARSE_FAILED(1008, "文档解析失败"),
    NO_AVAILABLE_AGENT(1009, "无可用客服");

    private final int code;
    private final String message;

    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }
}
