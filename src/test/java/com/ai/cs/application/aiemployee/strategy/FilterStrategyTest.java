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
class FilterStrategyTest {

    @Mock
    private AiEmployeeReplyStrategyRepository strategyRepo;

    private ObjectMapper objectMapper = new ObjectMapper();

    private FilterStrategy strategy;

    private AiEmployee employee;

    @BeforeEach
    void setUp() {
        strategy = new FilterStrategy(strategyRepo, objectMapper);
        employee = new AiEmployee();
        employee.setId(1L);
        employee.setName("test");
    }

    @Test
    void process_shouldAskQuestionWhenKeywordMatched() {
        AiEmployeeReplyStrategy config = new AiEmployeeReplyStrategy();
        config.setStrategyType("FILTER");
        config.setEnabled(true);
        config.setConfigJson("{\"criteria\":[{\"keyword\":\"价格\",\"action\":\"ASK\",\"question\":\"请问您对价格有什么要求？\"}]}");

        when(strategyRepo.findByEmployeeIdAndEnabledOrderBySortOrderAsc(1L, true))
                .thenReturn(List.of(config));

        ConversationContext ctx = ConversationContext.builder()
                .employee(employee)
                .customerMessage("这个产品的价格是多少")
                .build();

        ReplyBuilder builder = new ReplyBuilder();
        StrategyResult result = strategy.process(ctx, builder);

        assertEquals(StrategyResult.REPLIED, result);
        assertTrue(builder.hasContent());
        assertTrue(builder.build().contains("价格"));
    }

    @Test
    void process_shouldInterruptWhenExcludeAction() {
        AiEmployeeReplyStrategy config = new AiEmployeeReplyStrategy();
        config.setStrategyType("FILTER");
        config.setEnabled(true);
        config.setConfigJson("{\"criteria\":[{\"keyword\":\"违法\",\"action\":\"EXCLUDE\",\"question\":\"\"}]}");

        when(strategyRepo.findByEmployeeIdAndEnabledOrderBySortOrderAsc(1L, true))
                .thenReturn(List.of(config));

        ConversationContext ctx = ConversationContext.builder()
                .employee(employee)
                .customerMessage("我想做违法的事情")
                .build();

        ReplyBuilder builder = new ReplyBuilder();
        StrategyResult result = strategy.process(ctx, builder);

        assertEquals(StrategyResult.INTERRUPT, result);
        assertTrue(builder.hasContent());
        assertTrue(builder.build().contains("抱歉"));
    }

    @Test
    void process_shouldContinueWhenNoMatch() {
        AiEmployeeReplyStrategy config = new AiEmployeeReplyStrategy();
        config.setStrategyType("FILTER");
        config.setEnabled(true);
        config.setConfigJson("{\"criteria\":[{\"keyword\":\"价格\",\"action\":\"ASK\",\"question\":\"请问您对价格有什么要求？\"}]}");

        when(strategyRepo.findByEmployeeIdAndEnabledOrderBySortOrderAsc(1L, true))
                .thenReturn(List.of(config));

        ConversationContext ctx = ConversationContext.builder()
                .employee(employee)
                .customerMessage("我想了解产品功能")
                .build();

        ReplyBuilder builder = new ReplyBuilder();
        StrategyResult result = strategy.process(ctx, builder);

        assertEquals(StrategyResult.CONTINUE, result);
        assertFalse(builder.hasContent());
    }

    @Test
    void process_shouldContinueWhenNoFilterConfig() {
        when(strategyRepo.findByEmployeeIdAndEnabledOrderBySortOrderAsc(1L, true))
                .thenReturn(List.of());

        ConversationContext ctx = ConversationContext.builder()
                .employee(employee)
                .build();

        ReplyBuilder builder = new ReplyBuilder();
        StrategyResult result = strategy.process(ctx, builder);

        assertEquals(StrategyResult.CONTINUE, result);
    }

    @Test
    void process_shouldContinueWhenCriteriaEmpty() {
        AiEmployeeReplyStrategy config = new AiEmployeeReplyStrategy();
        config.setStrategyType("FILTER");
        config.setEnabled(true);
        config.setConfigJson("{\"criteria\":[]}");

        when(strategyRepo.findByEmployeeIdAndEnabledOrderBySortOrderAsc(1L, true))
                .thenReturn(List.of(config));

        ConversationContext ctx = ConversationContext.builder()
                .employee(employee)
                .customerMessage("test")
                .build();

        ReplyBuilder builder = new ReplyBuilder();
        StrategyResult result = strategy.process(ctx, builder);

        assertEquals(StrategyResult.CONTINUE, result);
    }

    @Test
    void getOrder_shouldReturn70() {
        assertEquals(70, strategy.getOrder());
    }

    @Test
    void process_shouldBeCaseInsensitive() {
        AiEmployeeReplyStrategy config = new AiEmployeeReplyStrategy();
        config.setStrategyType("FILTER");
        config.setEnabled(true);
        config.setConfigJson("{\"criteria\":[{\"keyword\":\"VIP\",\"action\":\"ASK\",\"question\":\"请问您是VIP客户吗？\"}]}");

        when(strategyRepo.findByEmployeeIdAndEnabledOrderBySortOrderAsc(1L, true))
                .thenReturn(List.of(config));

        ConversationContext ctx = ConversationContext.builder()
                .employee(employee)
                .customerMessage("我是vip用户")
                .build();

        ReplyBuilder builder = new ReplyBuilder();
        StrategyResult result = strategy.process(ctx, builder);

        assertEquals(StrategyResult.REPLIED, result);
        assertTrue(builder.build().contains("VIP"));
    }
}
