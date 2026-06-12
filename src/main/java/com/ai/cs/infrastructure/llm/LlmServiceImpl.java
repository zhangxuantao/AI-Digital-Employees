package com.ai.cs.infrastructure.llm;

import dev.langchain4j.model.chat.ChatLanguageModel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class LlmServiceImpl implements LlmService {
    private final ChatLanguageModel chatModel;

    @Override
    public String chat(String systemPrompt, String userMessage) {
        try {
            String fullPrompt = systemPrompt + "\n客户最新消息：" + userMessage + "\n请直接回复客户：";
            return chatModel.generate(fullPrompt);
        } catch (Exception e) {
            log.error("LLM调用失败", e);
            return "抱歉，我暂时无法处理您的问题，请稍后再试或联系人工客服。";
        }
    }
}
