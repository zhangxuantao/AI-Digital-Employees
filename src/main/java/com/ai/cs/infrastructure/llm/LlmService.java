package com.ai.cs.infrastructure.llm;

public interface LlmService {
    String chat(String systemPrompt, String userMessage);
}
