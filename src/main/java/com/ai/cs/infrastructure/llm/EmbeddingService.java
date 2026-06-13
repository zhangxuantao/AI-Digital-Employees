package com.ai.cs.infrastructure.llm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;

/**
 * 向量化服务 — 调用 OpenAI 兼容 Embedding API
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

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    /**
     * 单文本向量化 — 调用 OpenAI 兼容 Embedding API
     */
    public float[] embed(String text) {
        try {
            if (apiKey == null || apiKey.isEmpty()) {
                log.warn("Embedding API key 未配置，返回零向量占位。请设置 DEEPSEEK_API_KEY 或 QWEN_API_KEY 环境变量");
                return new float[dimensions];
            }

            // Build request body: {"model": "...", "input": "...", "dimensions": 1024}
            ObjectNode body = objectMapper.createObjectNode();
            body.put("model", model);
            body.put("input", text);
            body.put("dimensions", dimensions);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/v1/embeddings"))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + apiKey)
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body)))
                    .timeout(Duration.ofSeconds(30))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                JsonNode root = objectMapper.readTree(response.body());
                JsonNode embedding = root.path("data").get(0).path("embedding");
                float[] vector = new float[embedding.size()];
                for (int i = 0; i < embedding.size(); i++) {
                    vector[i] = embedding.get(i).floatValue();
                }
                log.debug("向量化完成: dimensions={}", vector.length);
                return vector;
            } else {
                log.error("Embedding API 返回非200: status={}, body={}", response.statusCode(), response.body());
                return new float[dimensions];
            }
        } catch (Exception e) {
            log.error("向量化失败: text length={}", text != null ? text.length() : 0, e);
            return new float[dimensions];
        }
    }

    /**
     * 批量向量化 — 每次 API 调用可能支持批量，这里逐条调用
     */
    public List<float[]> embedBatch(List<String> texts) {
        log.info("批量向量化: count={}", texts.size());
        return texts.stream().map(this::embed).toList();
    }
}
