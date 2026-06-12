package com.ai.cs.infrastructure.search;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.aggregations.StringTermsBucket;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.json.JsonData;
import com.ai.cs.domain.conversation.Message;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnBean(ElasticsearchClient.class)
public class ConversationLogIndexService {

    private static final String INDEX_NAME = "conversation_logs";
    private final ElasticsearchClient esClient;

    /** Index conversation messages after conversation closes */
    public void indexConversation(Long convId, List<Message> messages) {
        try {
            for (Message msg : messages) {
                Map<String, Object> doc = new LinkedHashMap<>();
                doc.put("conversation_id", convId);
                doc.put("sender_type", msg.getSenderType());
                doc.put("content", msg.getContent());
                doc.put("msg_type", msg.getMsgType());
                doc.put("send_time", msg.getSendTime() != null ? msg.getSendTime().toString() : null);
                esClient.index(i -> i.index(INDEX_NAME).document(doc));
            }
            log.info("对话日志已索引: convId={}, count={}", convId, messages.size());
        } catch (Exception e) {
            log.error("对话日志索引失败: convId={}", convId, e);
        }
    }

    /** ES aggregation — top N frequent questions */
    public List<Map.Entry<String, Long>> searchTopQuestions(int days, int topN) {
        try {
            SearchResponse<Void> response = esClient.search(s -> s
                    .index(INDEX_NAME)
                    .query(q -> q.range(r -> r.field("send_time").gte(JsonData.of("now-" + days + "d"))))
                    .aggregations("top_questions", agg -> agg
                            .terms(t -> t.field("content.keyword").size(topN))),
                    Void.class);

            List<Map.Entry<String, Long>> results = new ArrayList<>();
            var agg = response.aggregations().get("top_questions");
            if (agg != null && agg.sterms() != null) {
                for (StringTermsBucket bucket : agg.sterms().buckets().array()) {
                    results.add(new AbstractMap.SimpleEntry<>(
                            bucket.key().stringValue(), bucket.docCount()));
                }
            }
            return results;
        } catch (Exception e) {
            log.error("高频问题聚合查询失败", e);
            return List.of();
        }
    }
}
