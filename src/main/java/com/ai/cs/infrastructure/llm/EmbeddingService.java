package com.ai.cs.infrastructure.llm;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 向量化服务 — 调用 LLM Embedding API
 * 使用 OpenAI 兼容接口，支持通义千问 / DeepSeek
 */
@Slf4j
@Service
@ConditionalOnProperty(name = "embedding.enabled", havingValue = "true", matchIfMissing = true)
public class EmbeddingService {

    @Value("${embedding.provider:deepseek}")
    private String provider;

    @Value("${embedding.api-key:${DEEPSEEK_API_KEY:}}")
    private String apiKey;

    @Value("${embedding.model:text-embedding-v3}")
    private String model;

    @Value("${embedding.base-url:https://api.deepseek.com}")
    private String baseUrl;

    @Value("${embedding.dimensions:1024}")
    private int dimensions;

    /**
     * 单文本向量化
     */
    public float[] embed(String text) {
        try {
            log.debug("向量化: text length={}", text.length());
            // Phase 1 simplified implementation — generates zero vector placeholder
            // Full implementation: POST {baseUrl}/v1/embeddings with model/apiKey/dimensions
            float[] vector = new float[dimensions];
            log.info("向量化完成(简化): dimensions={}", dimensions);
            return vector;
        } catch (Exception e) {
            log.error("向量化失败", e);
            return new float[dimensions];
        }
    }

    /**
     * 批量向量化
     */
    public List<float[]> embedBatch(List<String> texts) {
        log.info("批量向量化: count={}", texts.size());
        return texts.stream().map(this::embed).toList();
    }
}
