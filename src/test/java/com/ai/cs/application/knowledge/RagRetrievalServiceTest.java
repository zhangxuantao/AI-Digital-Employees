package com.ai.cs.application.knowledge;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.elasticsearch.core.search.HitsMetadata;
import com.ai.cs.infrastructure.llm.LlmService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import co.elastic.clients.elasticsearch.core.search.TotalHitsRelation;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;

@ExtendWith(MockitoExtension.class)
class RagRetrievalServiceTest {

    @Mock
    private ElasticsearchClient esClient;

    @Mock
    private LlmService llmService;

    private RagRetrievalService ragRetrievalService;

    @BeforeEach
    void setUp() {
        ragRetrievalService = new RagRetrievalService(esClient, llmService);
    }

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    void retrieve_shouldReturnResultsFromKeywordSearch() throws Exception {
        // Mock search response with hits
        Hit<Map> hit1 = Hit.<Map>of(h -> h.id("1").index("knowledge_chunk_index")
                .source(Map.of("content", "知识库内容1", "kb_id", 1L)));
        Hit<Map> hit2 = Hit.<Map>of(h -> h.id("2").index("knowledge_chunk_index")
                .source(Map.of("content", "知识库内容2", "kb_id", 1L)));

        HitsMetadata<Map> hitsMetadata = HitsMetadata.<Map>of(hm -> hm
                .hits(List.of(hit1, hit2))
                .total(t -> t.value(2).relation(TotalHitsRelation.Eq)));

        SearchResponse<Map> searchResponse = SearchResponse.<Map>of(sr -> sr
                .hits(hitsMetadata)
                .took(5)
                .timedOut(false)
                .shards(sh -> sh.failures(List.of()).total(1).successful(1).failed(0)));

        doReturn(searchResponse).when(esClient).search(any(Function.class), any(Class.class));

        List<String> results = ragRetrievalService.retrieve(1L, "测试查询", 5);

        assertNotNull(results);
        assertFalse(results.isEmpty());
        assertEquals(2, results.size());
        assertTrue(results.contains("知识库内容1"));
        assertTrue(results.contains("知识库内容2"));
    }

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    void retrieve_shouldReturnEmptyListOnException() throws Exception {
        doThrow(new RuntimeException("ES connection failed"))
                .when(esClient).search(any(Function.class), any(Class.class));

        List<String> results = ragRetrievalService.retrieve(1L, "test", 5);

        assertNotNull(results);
        assertTrue(results.isEmpty());
    }

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    void retrieve_shouldRespectTopK() throws Exception {
        Hit<Map> hit1 = Hit.<Map>of(h -> h.id("1").index("knowledge_chunk_index")
                .source(Map.of("content", "内容1", "kb_id", 1L)));
        Hit<Map> hit2 = Hit.<Map>of(h -> h.id("2").index("knowledge_chunk_index")
                .source(Map.of("content", "内容2", "kb_id", 1L)));

        HitsMetadata<Map> hitsMetadata = HitsMetadata.<Map>of(hm -> hm
                .hits(List.of(hit1, hit2))
                .total(t -> t.value(2).relation(TotalHitsRelation.Eq)));

        SearchResponse<Map> searchResponse = SearchResponse.<Map>of(sr -> sr
                .hits(hitsMetadata)
                .took(5)
                .timedOut(false)
                .shards(sh -> sh.failures(List.of()).total(1).successful(1).failed(0)));

        doReturn(searchResponse).when(esClient).search(any(Function.class), any(Class.class));

        List<String> results = ragRetrievalService.retrieve(1L, "test", 1);

        assertEquals(1, results.size());
    }
}
