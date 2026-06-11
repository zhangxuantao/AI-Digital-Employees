package com.ai.cs.application.analytics;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TopQuestionServiceTest {

    private TopQuestionService service;

    @BeforeEach
    void setUp() {
        service = new TopQuestionService();
    }

    @Test
    void getTopQuestions_shouldReturnLimitedResults() {
        List<TopQuestionService.QuestionRank> result = service.getTopQuestions(3);
        assertEquals(3, result.size());
    }

    @Test
    void getTopQuestions_shouldReturnAllWhenLimitIsHigh() {
        List<TopQuestionService.QuestionRank> result = service.getTopQuestions(20);
        assertEquals(5, result.size()); // Placeholder has 5 items
    }

    @Test
    void getTopQuestions_shouldReturnEmptyWhenLimitIsZero() {
        List<TopQuestionService.QuestionRank> result = service.getTopQuestions(0);
        assertEquals(0, result.size());
    }

    @Test
    void getTopQuestions_shouldHaveCounts() {
        List<TopQuestionService.QuestionRank> result = service.getTopQuestions(5);
        assertFalse(result.isEmpty());
        assertTrue(result.get(0).getCount() > 0);
        assertNotNull(result.get(0).getQuestion());
    }

    @Test
    void getTopQuestions_shouldBeOrderedByCount() {
        List<TopQuestionService.QuestionRank> result = service.getTopQuestions(5);
        for (int i = 1; i < result.size(); i++) {
            assertTrue(result.get(i - 1).getCount() >= result.get(i).getCount());
        }
    }
}
