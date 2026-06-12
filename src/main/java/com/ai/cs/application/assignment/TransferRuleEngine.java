package com.ai.cs.application.assignment;

import com.ai.cs.domain.assignment.TransferRule;
import com.ai.cs.domain.assignment.repository.TransferRuleRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class TransferRuleEngine {

    private final TransferRuleRepository ruleRepository;
    private final ObjectMapper objectMapper;

    public TransferDecision evaluate(Long employeeId, String customerMessage) {
        List<TransferRule> rules = ruleRepository
                .findByEmployeeIdAndEnabledOrderByPriorityAsc(employeeId, true);

        for (TransferRule rule : rules) {
            boolean triggered = switch (rule.getTriggerType()) {
                case "KEYWORD" -> matchKeyword(rule.getTriggerConfig(), customerMessage);
                case "MANUAL" -> matchManual(rule.getTriggerConfig(), customerMessage);
                case "SENTIMENT" -> false; // Phase 2: integrate sentiment analysis
                case "TIME" -> isOutsideServiceTime();
                default -> false;
            };

            if (triggered) {
                log.info("转人工规则触发: type={}, priority={}", rule.getTriggerType(), rule.getPriority());
                return TransferDecision.transfer(rule);
            }
        }

        return TransferDecision.noTransfer();
    }

    private boolean matchKeyword(String configJson, String message) {
        try {
            JsonNode cfg = objectMapper.readTree(configJson);
            var keywords = cfg.path("keywords");
            if (keywords.isArray()) {
                for (JsonNode kw : keywords) {
                    if (message.contains(kw.asText())) return true;
                }
            }
            String keyword = cfg.path("keyword").asText(null);
            if (keyword != null && message.contains(keyword)) return true;
        } catch (Exception e) {
            log.warn("关键词配置解析失败", e);
        }
        return false;
    }

    private boolean matchManual(String configJson, String message) {
        String[] manualTriggers = {"转人工", "找客服", "人工客服", "不要机器人", "真人"};
        for (String trigger : manualTriggers) {
            if (message.contains(trigger)) return true;
        }
        return false;
    }

    private boolean isOutsideServiceTime() {
        return false;
    }

    @Data
    public static class TransferDecision {
        private final boolean shouldTransfer;
        private final TransferRule matchedRule;

        public static TransferDecision transfer(TransferRule rule) {
            return new TransferDecision(true, rule);
        }

        public static TransferDecision noTransfer() {
            return new TransferDecision(false, null);
        }
    }
}
