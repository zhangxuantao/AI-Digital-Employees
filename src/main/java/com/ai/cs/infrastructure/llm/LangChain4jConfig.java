package com.ai.cs.infrastructure.llm;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.ollama.OllamaChatModel;
import dev.langchain4j.model.output.Response;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.List;

@Slf4j
@Configuration
public class LangChain4jConfig {

    @Bean
    public ChatLanguageModel chatLanguageModel() {
        String provider = System.getenv().getOrDefault("LLM_PROVIDER", "deepseek");
        String apiKey = System.getenv().getOrDefault("LLM_API_KEY", "");

        // Return stub model when API key is not configured (e.g., in test environments)
        if (apiKey.isEmpty() && !"ollama".equals(provider)) {
            log.warn("LLM_API_KEY not configured, using stub ChatLanguageModel. " +
                    "Set LLM_PROVIDER=ollama or configure LLM_API_KEY for real LLM calls.");
            return new StubChatLanguageModel();
        }

        if ("ollama".equals(provider)) {
            return OllamaChatModel.builder()
                    .baseUrl("http://localhost:11434")
                    .modelName("qwen2.5:7b")
                    .timeout(Duration.ofSeconds(60))
                    .build();
        }
        String baseUrl = "deepseek".equals(provider)
                ? "https://api.deepseek.com"
                : "https://dashscope.aliyuncs.com/compatible-mode/v1";
        String model = "deepseek".equals(provider) ? "deepseek-chat" : "qwen-plus";

        return OpenAiChatModel.builder()
                .baseUrl(baseUrl)
                .apiKey(apiKey)
                .modelName(model)
                .timeout(Duration.ofSeconds(60))
                .maxRetries(2)
                .build();
    }

    /**
     * Stub ChatLanguageModel that returns a fixed response.
     * Used when no real LLM API key is configured.
     */
    @Slf4j
    static class StubChatLanguageModel implements ChatLanguageModel {
        @Override
        public Response<AiMessage> generate(List<ChatMessage> messages) {
            log.debug("[STUB] ChatLanguageModel.generate called with {} messages", messages.size());
            return Response.from(AiMessage.from("您好，感谢您的咨询！请问有什么可以帮助您的？"));
        }
    }
}
