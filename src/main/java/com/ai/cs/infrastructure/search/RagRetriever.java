package com.ai.cs.infrastructure.search;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.json.JsonData;
import com.ai.cs.infrastructure.llm.EmbeddingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnBean(ElasticsearchClient.class)
public class RagRetriever {

    private final ElasticsearchClient esClient;
    private final EmbeddingService embeddingService;
    private final KnowledgeChunkIndexService chunkIndexService;

    private static final String INDEX = KnowledgeChunkIndexService.INDEX_NAME;
    private static final double RRF_K = 60.0;

    /**
     * Hybrid search: BM25 + kNN vector search, RRF fusion ranking
     */
    public List<String> hybridSearch(Long kbId, String query, int topK) {
        try {
            // 1. BM25 keyword search
            List<String> keywordResults = chunkIndexService.searchByKb(kbId, query, topK * 2);

            // 2. kNN vector search
            float[] queryVector = embeddingService.embed(query);
            List<String> vectorResults = knnSearch(kbId, queryVector, topK * 2);

            // 3. RRF fusion
            Map<String, Double> rrfScores = new LinkedHashMap<>();
            addRrfScores(rrfScores, keywordResults, RRF_K);
            addRrfScores(rrfScores, vectorResults, RRF_K);

            // 4. Sort by RRF score, take topK
            return rrfScores.entrySet().stream()
                    .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                    .limit(topK)
                    .map(Map.Entry::getKey)
                    .toList();
        } catch (Exception e) {
            log.error("RAG混合检索失败: kbId={}, query={}", kbId, query, e);
            return chunkIndexService.searchByKb(kbId, query, topK);
        }
    }

    private List<String> knnSearch(Long kbId, float[] queryVector, int topK) {
        try {
            SearchRequest request = SearchRequest.of(s -> s
                    .index(INDEX)
                    .query(q -> q.bool(b -> b
                            .filter(f -> f.term(t -> t.field("kb_id").value(kbId)))
                            .must(m -> m.scriptScore(ss -> ss
                                    .query(iq -> iq.matchAll(ma -> ma))
                                    .script(sc -> sc.inline(i -> i
                                            .source("cosineSimilarity(params.query_vector, 'embedding') + 1.0")
                                            .params(Map.of("query_vector", JsonData.of(queryVector)))
                                    ))
                            ))
                    ))
                    .size(topK)
            );

            SearchResponse<Map> response = esClient.search(request, Map.class);

            List<String> results = new ArrayList<>();
            for (Hit<Map> hit : response.hits().hits()) {
                Map<String, Object> source = hit.source();
                if (source != null && source.get("content") != null) {
                    results.add((String) source.get("content"));
                }
            }
            return results;
        } catch (Exception e) {
            log.warn("kNN向量检索失败，降级: kbId={}", kbId, e);
            return List.of();
        }
    }

    private void addRrfScores(Map<String, Double> scores, List<String> results, double k) {
        for (int i = 0; i < results.size(); i++) {
            scores.merge(results.get(i), 1.0 / (k + i + 1), Double::sum);
        }
    }
}
