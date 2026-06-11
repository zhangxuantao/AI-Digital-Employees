package com.ai.cs.infrastructure.llm;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class StubLlmService implements LlmService {
    @Override
    public String chat(String systemPrompt, String userMessage) {
        log.info("[STUB] LLM called with prompt length={}", systemPrompt.length());
        return "您好，感谢您的咨询！请问有什么可以帮助您的？";
    }
}
