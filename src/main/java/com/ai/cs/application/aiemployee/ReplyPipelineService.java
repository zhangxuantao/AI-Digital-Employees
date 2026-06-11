package com.ai.cs.application.aiemployee;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class ReplyPipelineService {
    public String process(Long conversationId, Long employeeId, String customerMessage) {
        // STUB — will be fully implemented in Task 1.4
        log.info("[STUB] process: conversationId={}, employeeId={}, message={}", conversationId, employeeId, customerMessage);
        return "您好，感谢您的咨询，我们稍后会回复您。";
    }
}
