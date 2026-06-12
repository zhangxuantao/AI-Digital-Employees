package com.ai.cs.application.knowledge;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import com.ai.cs.infrastructure.llm.LlmService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnBean(ElasticsearchClient.class)
public class RagRetrievalService {

    private final ElasticsearchClient esClient;
    private final LlmService llmService;

    /**
     * Hybrid retrieval: kNN vector search + BM25 keyword search, fused with RRF
     */
    public List<String> retrieve(Long kbId, String query, int topK) {
        try {
            // 1. BM25 keyword search
            SearchResponse<Map> keywordResults = esClient.search(s -> s
                    .index("knowledge_chunk_index")
                    .query(q -> q
                            .bool(b -> b
                                    .filter(f -> f.term(t -> t.field("kb_id").value(kbId)))
                                    .must(m -> m.match(ma -> ma.field("content").query(query)))
                            )
                    )
                    .size(10),
                    Map.class);

            // 2. kNN vector search (requires embedding service — for now, fallback to keyword only)
            // When EmbeddingService is available, add kNN query here

            // 3. RRF fusion
            Map<String, Double> rrfScores = new LinkedHashMap<>();
            double k = 60;

            int rank = 1;
            for (Hit<Map> hit : keywordResults.hits().hits()) {
                String chunkId = hit.id();
                rrfScores.merge(chunkId, 1.0 / (k + rank), Double::sum);
                rank++;
            }

            // 4. Sort by RRF score, return top-K content
            List<Map.Entry<String, Double>> sorted = rrfScores.entrySet().stream()
                    .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                    .limit(topK)
                    .toList();

            List<String> results = new ArrayList<>();
            for (var entry : sorted) {
                Hit<Map> hit = keywordResults.hits().hits().stream()
                        .filter(h -> h.id().equals(entry.getKey()))
                        .findFirst().orElse(null);
                if (hit != null && hit.source() != null) {
                    results.add((String) hit.source().get("content"));
                }
            }

            return results;
        } catch (Exception e) {
            log.error("RAG检索失败", e);
            return List.of();
        }
    }
}
