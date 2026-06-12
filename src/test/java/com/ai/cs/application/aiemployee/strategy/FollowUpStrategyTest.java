package com.ai.cs.application.aiemployee.strategy;

import com.ai.cs.domain.employee.AiEmployee;
import com.ai.cs.domain.employee.AiEmployeeReplyStrategy;
import com.ai.cs.domain.employee.repository.AiEmployeeReplyStrategyRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FollowUpStrategyTest {

    @Mock
    private AiEmployeeReplyStrategyRepository strategyRepo;

    private ObjectMapper objectMapper = new ObjectMapper();

    private FollowUpStrategy strategy;

    private AiEmployee employee;

    @BeforeEach
    void setUp() {
        strategy = new FollowUpStrategy(strategyRepo, objectMapper);
        employee = new AiEmployee();
        employee.setId(1L);
        employee.setName("test");
    }

    @Test
    void process_shouldAddFixedFollowUpWhenNoReplyContent() {
        AiEmployeeReplyStrategy config = new AiEmployeeReplyStrategy();
        config.setStrategyType("FOLLOWUP");
        config.setEnabled(true);
        config.setConfigJson("{\"intervals\":[{\"fixed\":\"请问您还在吗？\"}]}");

        when(strategyRepo.findByEmployeeIdAndEnabledOrderBySortOrderAsc(1L, true))
                .thenReturn(List.of(config));

        ConversationContext ctx = ConversationContext.builder()
                .employee(employee)
                .build();

        ReplyBuilder builder = new ReplyBuilder();
        StrategyResult result = strategy.process(ctx, builder);

        assertEquals(StrategyResult.CONTINUE, result);
        assertTrue(builder.hasContent());
        assertTrue(builder.build().contains("请问您还在吗？"));
    }

    @Test
    void process_shouldPassThroughWhenReplyAlreadyHasContent() {
        AiEmployeeReplyStrategy config = new AiEmployeeReplyStrategy();
        config.setStrategyType("FOLLOWUP");
        config.setEnabled(true);
        config.setConfigJson("{\"intervals\":[{\"fixed\":\"请问您还在吗？\"}]}");

        when(strategyRepo.findByEmployeeIdAndEnabledOrderBySortOrderAsc(1L, true))
                .thenReturn(List.of(config));

        ConversationContext ctx = ConversationContext.builder()
                .employee(employee)
                .build();

        ReplyBuilder builder = new ReplyBuilder();
        builder.append("已经回复过了");
        StrategyResult result = strategy.process(ctx, builder);

        assertEquals(StrategyResult.CONTINUE, result);
        assertEquals("已经回复过了", builder.build());
        assertFalse(builder.build().contains("请问您还在吗？"));
    }

    @Test
    void process_shouldContinueWhenNoFollowUpConfig() {
        when(strategyRepo.findByEmployeeIdAndEnabledOrderBySortOrderAsc(1L, true))
                .thenReturn(List.of());

        ConversationContext ctx = ConversationContext.builder()
                .employee(employee)
                .build();

        ReplyBuilder builder = new ReplyBuilder();
        StrategyResult result = strategy.process(ctx, builder);

        assertEquals(StrategyResult.CONTINUE, result);
        assertFalse(builder.hasContent());
    }

    @Test
    void process_shouldContinueWhenIntervalsEmpty() {
        AiEmployeeReplyStrategy config = new AiEmployeeReplyStrategy();
        config.setStrategyType("FOLLOWUP");
        config.setEnabled(true);
        config.setConfigJson("{\"intervals\":[]}");

        when(strategyRepo.findByEmployeeIdAndEnabledOrderBySortOrderAsc(1L, true))
                .thenReturn(List.of(config));

        ConversationContext ctx = ConversationContext.builder()
                .employee(employee)
                .build();

        ReplyBuilder builder = new ReplyBuilder();
        StrategyResult result = strategy.process(ctx, builder);

        assertEquals(StrategyResult.CONTINUE, result);
        assertFalse(builder.hasContent());
    }

    @Test
    void getOrder_shouldReturn80() {
        assertEquals(80, strategy.getOrder());
    }
}
