package com.ai.cs.application.analytics;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TopQuestionService {

    /**
     * Phase 1 simplified: returns placeholder.
     * Phase 2: use ES aggregation on conversation_log_index for real clustering.
     */
    public List<QuestionRank> getTopQuestions(int limit) {
        // Placeholder — real implementation uses ES aggregation
        return List.of(
                new QuestionRank("产品价格咨询", 156),
                new QuestionRank("售后服务流程", 98),
                new QuestionRank("物流配送时效", 72),
                new QuestionRank("退换货政策", 45),
                new QuestionRank("会员权益说明", 31)
        ).stream().limit(limit).toList();
    }

    @Data
    public static class QuestionRank {
        private final String question;
        private final long count;
        private int rank;

        public QuestionRank(String question, long count) {
            this.question = question;
            this.count = count;
        }
    }
}
