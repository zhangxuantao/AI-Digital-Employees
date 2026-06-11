package com.ai.cs.infrastructure.llm;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.output.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;

class LlmServiceImplTest {

    private LlmServiceImpl llmService;
    private StubChatModel chatModel;

    @BeforeEach
    void setUp() {
        chatModel = new StubChatModel();
        llmService = new LlmServiceImpl(chatModel);
    }

    @Test
    void chatShouldReturnResponseFromModel() {
        chatModel.setResponse("AI回复内容");

        String result = llmService.chat("你是客服助手", "你好");

        assertEquals("AI回复内容", result);
    }

    @Test
    void chatShouldReturnFallbackMessageOnException() {
        chatModel.setException(new RuntimeException("API超时"));

        String result = llmService.chat("system", "hello");

        assertEquals("抱歉，我暂时无法处理您的问题，请稍后再试或联系人工客服。", result);
    }

    @Test
    void chatShouldIncludeSystemPromptAndUserMessageInPrompt() {
        AtomicReference<List<ChatMessage>> captured = new AtomicReference<>();
        chatModel.setInterceptor(captured::set);

        llmService.chat("系统提示词", "用户消息内容");

        List<ChatMessage> messages = captured.get();
        assertNotNull(messages);
        assertEquals(1, messages.size());
        String text = messages.get(0).text();
        assertTrue(text.contains("系统提示词"), "应包含系统提示词");
        assertTrue(text.contains("客户最新消息：用户消息内容"), "应包含用户消息");
        assertTrue(text.contains("请直接回复客户："), "应包含回复指令");
    }

    /**
     * A simple stub implementation of ChatLanguageModel for testing.
     * Default methods are inherited from the interface, so generate(String)
     * will delegate to generate(List<ChatMessage>) which this stub controls.
     */
    private static class StubChatModel implements ChatLanguageModel {
        private String response = "Default response";
        private RuntimeException exception;
        private Consumer<List<ChatMessage>> interceptor;

        void setResponse(String response) { this.response = response; this.exception = null; }

        void setException(RuntimeException exception) { this.exception = exception; this.response = null; }

        void setInterceptor(Consumer<List<ChatMessage>> interceptor) { this.interceptor = interceptor; }

        @Override
        public Response<AiMessage> generate(List<ChatMessage> messages) {
            if (interceptor != null) interceptor.accept(messages);
            if (exception != null) throw exception;
            return Response.from(AiMessage.from(response));
        }
    }
}
