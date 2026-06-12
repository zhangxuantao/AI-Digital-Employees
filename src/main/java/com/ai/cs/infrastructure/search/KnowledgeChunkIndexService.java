package com.ai.cs.infrastructure.search;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.mapping.DenseVectorProperty;
import co.elastic.clients.elasticsearch._types.mapping.Property;
import co.elastic.clients.elasticsearch.core.BulkRequest;
import co.elastic.clients.elasticsearch.core.BulkResponse;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import com.ai.cs.domain.knowledge.KnowledgeChunk;
import com.ai.cs.domain.knowledge.repository.KnowledgeChunkRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnBean(ElasticsearchClient.class)
public class KnowledgeChunkIndexService {

    public static final String INDEX_NAME = "knowledge_chunks";

    private final ElasticsearchClient esClient;
    private final KnowledgeChunkRepository chunkRepository;

    /** Bulk index knowledge chunks to ES */
    public void bulkIndex(List<KnowledgeChunk> chunks, List<float[]> embeddings) {
        try {
            ensureIndex();
            BulkRequest.Builder bulkBuilder = new BulkRequest.Builder();
            for (int i = 0; i < chunks.size(); i++) {
                KnowledgeChunk chunk = chunks.get(i);
                float[] vector = (embeddings != null && i < embeddings.size())
                        ? embeddings.get(i) : new float[1024];

                Map<String, Object> doc = new LinkedHashMap<>();
                doc.put("kb_id", chunk.getKbId());
                doc.put("doc_id", chunk.getDocId());
                doc.put("chunk_id", chunk.getId());
                doc.put("content", chunk.getContent());
                doc.put("embedding", vector);
                doc.put("created_at", new Date().toString());

                String esDocId = "chunk_" + chunk.getId();
                bulkBuilder.operations(op -> op.index(idx -> idx
                        .index(INDEX_NAME).id(esDocId).document(doc)));

                chunk.setEsDocId(esDocId);
            }

            BulkResponse response = esClient.bulk(bulkBuilder.build());
            if (response.errors()) {
                log.error("ES批量索引有错误: {}", response.items().stream()
                        .filter(item -> item.error() != null)
                        .map(item -> item.error().reason()).toList());
            } else {
                log.info("ES批量索引完成: count={}, took={}ms", chunks.size(), response.took());
            }
            chunkRepository.saveAll(chunks);
        } catch (Exception e) {
            log.error("ES批量索引失败: count={}", chunks.size(), e);
        }
    }

    /** Delete ES index entries for a document (before re-training) */
    public void deleteByDocId(Long docId) {
        try {
            esClient.deleteByQuery(d -> d.index(INDEX_NAME)
                    .query(q -> q.term(t -> t.field("doc_id").value(docId))));
            log.info("ES索引已删除: docId={}", docId);
        } catch (Exception e) {
            log.error("ES删除索引失败: docId={}", docId, e);
        }
    }

    /** BM25 text search */
    public List<String> searchByKb(Long kbId, String query, int topK) {
        try {
            SearchResponse<Map> response = esClient.search(s -> s
                    .index(INDEX_NAME)
                    .query(q -> q.bool(b -> b
                            .filter(f -> f.term(t -> t.field("kb_id").value(kbId)))
                            .must(m -> m.match(ma -> ma.field("content").query(query)))))
                    .size(topK), Map.class);

            List<String> results = new ArrayList<>();
            for (Hit<Map> hit : response.hits().hits()) {
                Map<String, Object> source = hit.source();
                if (source != null && source.get("content") != null) {
                    results.add((String) source.get("content"));
                }
            }
            return results;
        } catch (Exception e) {
            log.error("ES BM25检索失败: kbId={}, query={}", kbId, query, e);
            return List.of();
        }
    }

    private void ensureIndex() {
        try {
            boolean exists = esClient.indices().exists(e -> e.index(INDEX_NAME)).value();
            if (!exists) {
                esClient.indices().create(c -> c.index(INDEX_NAME).mappings(m -> m
                        .properties("kb_id", Property.of(p -> p.long_(l -> l)))
                        .properties("doc_id", Property.of(p -> p.long_(l -> l)))
                        .properties("chunk_id", Property.of(p -> p.long_(l -> l)))
                        .properties("content", Property.of(p -> p.text(t -> t)))
                        .properties("embedding", Property.of(p -> p.denseVector(
                                DenseVectorProperty.of(d -> d.dims(1024)))))
                        .properties("created_at", Property.of(p -> p.date(d -> d)))));
                log.info("ES索引已创建: {}", INDEX_NAME);
            }
        } catch (Exception e) {
            log.error("ES索引创建失败", e);
        }
    }
}
