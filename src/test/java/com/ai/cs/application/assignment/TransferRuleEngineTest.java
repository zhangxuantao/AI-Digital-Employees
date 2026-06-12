package com.ai.cs.application.assignment;

import com.ai.cs.domain.assignment.TransferRule;
import com.ai.cs.domain.assignment.repository.TransferRuleRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TransferRuleEngineTest {

    @Mock
    private TransferRuleRepository ruleRepository;

    private TransferRuleEngine engine;

    @BeforeEach
    void setUp() {
        engine = new TransferRuleEngine(ruleRepository, new ObjectMapper());
    }

    @Test
    void evaluate_keywordTrigger_shouldReturnTransferWhenKeywordMatched() {
        TransferRule rule = new TransferRule();
        rule.setTriggerType("KEYWORD");
        rule.setTriggerConfig("{\"keywords\": [\"退款\", \"投诉\"]}");
        rule.setPriority(1);
        rule.setActionType("TRANSFER");
        rule.setEnabled(true);
        rule.setEmployeeId(1L);

        when(ruleRepository.findByEmployeeIdAndEnabledOrderByPriorityAsc(1L, true))
                .thenReturn(List.of(rule));

        TransferRuleEngine.TransferDecision decision = engine.evaluate(1L, "我要退款");

        assertTrue(decision.isShouldTransfer());
        assertNotNull(decision.getMatchedRule());
        assertEquals("TRANSFER", decision.getMatchedRule().getActionType());
    }

    @Test
    void evaluate_keywordTrigger_shouldReturnNoTransferWhenNoKeywordMatched() {
        TransferRule rule = new TransferRule();
        rule.setTriggerType("KEYWORD");
        rule.setTriggerConfig("{\"keywords\": [\"退款\", \"投诉\"]}");
        rule.setPriority(1);
        rule.setEmployeeId(1L);

        when(ruleRepository.findByEmployeeIdAndEnabledOrderByPriorityAsc(1L, true))
                .thenReturn(List.of(rule));

        TransferRuleEngine.TransferDecision decision = engine.evaluate(1L, "你好，我想咨询一下产品信息");

        assertFalse(decision.isShouldTransfer());
        assertNull(decision.getMatchedRule());
    }

    @Test
    void evaluate_keywordTrigger_singleKeyword_shouldMatch() {
        TransferRule rule = new TransferRule();
        rule.setTriggerType("KEYWORD");
        rule.setTriggerConfig("{\"keyword\": \"人工\"}");
        rule.setPriority(1);
        rule.setEmployeeId(1L);

        when(ruleRepository.findByEmployeeIdAndEnabledOrderByPriorityAsc(1L, true))
                .thenReturn(List.of(rule));

        TransferRuleEngine.TransferDecision decision = engine.evaluate(1L, "我要转人工");

        assertTrue(decision.isShouldTransfer());
    }

    @Test
    void evaluate_manualTrigger_shouldReturnTransfer() {
        TransferRule rule = new TransferRule();
        rule.setTriggerType("MANUAL");
        rule.setTriggerConfig("{}");
        rule.setPriority(1);
        rule.setEmployeeId(1L);

        when(ruleRepository.findByEmployeeIdAndEnabledOrderByPriorityAsc(1L, true))
                .thenReturn(List.of(rule));

        TransferRuleEngine.TransferDecision decision = engine.evaluate(1L, "转人工");

        assertTrue(decision.isShouldTransfer());
    }

    @Test
    void evaluate_manualTrigger_variousPhrases_shouldAllMatch() {
        TransferRule rule = new TransferRule();
        rule.setTriggerType("MANUAL");
        rule.setTriggerConfig("{}");
        rule.setPriority(1);
        rule.setEmployeeId(1L);

        when(ruleRepository.findByEmployeeIdAndEnabledOrderByPriorityAsc(1L, true))
                .thenReturn(List.of(rule));

        String[] phrases = {"转人工", "找客服", "人工客服", "不要机器人", "真人"};
        for (String phrase : phrases) {
            TransferRuleEngine.TransferDecision decision = engine.evaluate(1L, phrase);
            assertTrue(decision.isShouldTransfer(), "Phrase should trigger: " + phrase);
        }
    }

    @Test
    void evaluate_manualTrigger_normalMessage_shouldNotTransfer() {
        TransferRule rule = new TransferRule();
        rule.setTriggerType("MANUAL");
        rule.setTriggerConfig("{}");
        rule.setPriority(1);
        rule.setEmployeeId(1L);

        when(ruleRepository.findByEmployeeIdAndEnabledOrderByPriorityAsc(1L, true))
                .thenReturn(List.of(rule));

        TransferRuleEngine.TransferDecision decision = engine.evaluate(1L, "你好，请问有什么可以帮您的");

        assertFalse(decision.isShouldTransfer());
    }

    @Test
    void evaluate_multipleRules_shouldRespectPriorityOrder() {
        TransferRule lowPriority = new TransferRule();
        lowPriority.setTriggerType("KEYWORD");
        lowPriority.setTriggerConfig("{\"keywords\": [\"退款\"]}");
        lowPriority.setPriority(2);
        lowPriority.setEmployeeId(1L);

        TransferRule highPriority = new TransferRule();
        highPriority.setTriggerType("MANUAL");
        highPriority.setTriggerConfig("{}");
        highPriority.setPriority(1);
        highPriority.setEmployeeId(1L);

        when(ruleRepository.findByEmployeeIdAndEnabledOrderByPriorityAsc(1L, true))
                .thenReturn(List.of(highPriority, lowPriority));

        // Both "转人工" and "退款" are in the message, but MANUAL has higher priority
        TransferRuleEngine.TransferDecision decision = engine.evaluate(1L, "转人工，我要退款");

        assertTrue(decision.isShouldTransfer());
        assertEquals("MANUAL", decision.getMatchedRule().getTriggerType());
    }

    @Test
    void evaluate_noRules_shouldReturnNoTransfer() {
        when(ruleRepository.findByEmployeeIdAndEnabledOrderByPriorityAsc(999L, true))
                .thenReturn(List.of());

        TransferRuleEngine.TransferDecision decision = engine.evaluate(999L, "任何消息");

        assertFalse(decision.isShouldTransfer());
        assertNull(decision.getMatchedRule());
    }

    @Test
    void evaluate_sentimentTrigger_shouldReturnNoTransferInPhase1() {
        TransferRule rule = new TransferRule();
        rule.setTriggerType("SENTIMENT");
        rule.setTriggerConfig("{\"threshold\": \"negative\"}");
        rule.setPriority(1);
        rule.setEmployeeId(1L);

        when(ruleRepository.findByEmployeeIdAndEnabledOrderByPriorityAsc(1L, true))
                .thenReturn(List.of(rule));

        TransferRuleEngine.TransferDecision decision = engine.evaluate(1L, "你们太差劲了");

        assertFalse(decision.isShouldTransfer(), "SENTIMENT trigger should return false in Phase 1");
    }

    @Test
    void evaluate_timeTrigger_shouldReturnNoTransferInPhase1() {
        TransferRule rule = new TransferRule();
        rule.setTriggerType("TIME");
        rule.setTriggerConfig("{\"start\": \"09:00\", \"end\": \"18:00\"}");
        rule.setPriority(1);
        rule.setEmployeeId(1L);

        when(ruleRepository.findByEmployeeIdAndEnabledOrderByPriorityAsc(1L, true))
                .thenReturn(List.of(rule));

        TransferRuleEngine.TransferDecision decision = engine.evaluate(1L, "你好");

        assertFalse(decision.isShouldTransfer(), "TIME trigger should return false in Phase 1");
    }

    @Test
    void evaluate_unknownTriggerType_shouldNotMatch() {
        TransferRule rule = new TransferRule();
        rule.setTriggerType("UNKNOWN");
        rule.setTriggerConfig("{}");
        rule.setPriority(1);
        rule.setEmployeeId(1L);

        when(ruleRepository.findByEmployeeIdAndEnabledOrderByPriorityAsc(1L, true))
                .thenReturn(List.of(rule));

        TransferRuleEngine.TransferDecision decision = engine.evaluate(1L, "任何消息");

        assertFalse(decision.isShouldTransfer());
    }

    @Test
    void transferDecision_transfer_shouldCreateCorrectDecision() {
        TransferRule rule = new TransferRule();
        rule.setTriggerType("KEYWORD");
        rule.setPriority(1);

        TransferRuleEngine.TransferDecision decision = TransferRuleEngine.TransferDecision.transfer(rule);

        assertTrue(decision.isShouldTransfer());
        assertEquals(rule, decision.getMatchedRule());
    }

    @Test
    void transferDecision_noTransfer_shouldCreateCorrectDecision() {
        TransferRuleEngine.TransferDecision decision = TransferRuleEngine.TransferDecision.noTransfer();

        assertFalse(decision.isShouldTransfer());
        assertNull(decision.getMatchedRule());
    }
}
