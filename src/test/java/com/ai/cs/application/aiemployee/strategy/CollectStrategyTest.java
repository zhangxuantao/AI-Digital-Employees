package com.ai.cs.application.aiemployee.strategy;

import com.ai.cs.domain.customer.CustomerProfile;
import com.ai.cs.domain.customer.repository.CustomerProfileRepository;
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
class CollectStrategyTest {

    @Mock
    private AiEmployeeReplyStrategyRepository strategyRepo;

    @Mock
    private CustomerProfileRepository customerRepo;

    private ObjectMapper objectMapper = new ObjectMapper();

    private CollectStrategy strategy;

    private AiEmployee employee;

    @BeforeEach
    void setUp() {
        strategy = new CollectStrategy(strategyRepo, customerRepo, objectMapper);
        employee = new AiEmployee();
        employee.setId(1L);
        employee.setName("test");
    }

    @Test
    void process_shouldAskQuestionWhenFieldMissing() {
        AiEmployeeReplyStrategy config = new AiEmployeeReplyStrategy();
        config.setStrategyType("COLLECT");
        config.setEnabled(true);
        config.setConfigJson("{\"fields\":[{\"name\":\"phone\",\"question\":\"请问您的手机号是多少？\"}]}");

        when(strategyRepo.findByEmployeeIdAndEnabledOrderBySortOrderAsc(1L, true))
                .thenReturn(List.of(config));

        CustomerProfile customer = new CustomerProfile();
        customer.setId(1L);
        customer.setExtraFields(null);

        ConversationContext ctx = ConversationContext.builder()
                .employee(employee)
                .customer(customer)
                .build();

        ReplyBuilder builder = new ReplyBuilder();
        StrategyResult result = strategy.process(ctx, builder);

        assertEquals(StrategyResult.REPLIED, result);
        assertTrue(builder.hasContent());
        assertTrue(builder.build().contains("手机号"));
    }

    @Test
    void process_shouldContinueWhenAllFieldsCollected() {
        AiEmployeeReplyStrategy config = new AiEmployeeReplyStrategy();
        config.setStrategyType("COLLECT");
        config.setEnabled(true);
        config.setConfigJson("{\"fields\":[{\"name\":\"phone\",\"question\":\"请问您的手机号是多少？\"}]}");

        when(strategyRepo.findByEmployeeIdAndEnabledOrderBySortOrderAsc(1L, true))
                .thenReturn(List.of(config));

        CustomerProfile customer = new CustomerProfile();
        customer.setId(1L);
        customer.setExtraFields("{\"phone\":\"13800138000\"}");

        ConversationContext ctx = ConversationContext.builder()
                .employee(employee)
                .customer(customer)
                .build();

        ReplyBuilder builder = new ReplyBuilder();
        StrategyResult result = strategy.process(ctx, builder);

        assertEquals(StrategyResult.CONTINUE, result);
        assertFalse(builder.hasContent());
    }

    @Test
    void process_shouldContinueWhenNoCollectConfig() {
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
    void process_shouldContinueWhenCustomerIsNull() {
        AiEmployeeReplyStrategy config = new AiEmployeeReplyStrategy();
        config.setStrategyType("COLLECT");
        config.setEnabled(true);
        config.setConfigJson("{\"fields\":[{\"name\":\"phone\",\"question\":\"请问您的手机号是多少？\"}]}");

        when(strategyRepo.findByEmployeeIdAndEnabledOrderBySortOrderAsc(1L, true))
                .thenReturn(List.of(config));

        ConversationContext ctx = ConversationContext.builder()
                .employee(employee)
                .customer(null)
                .build();

        ReplyBuilder builder = new ReplyBuilder();
        StrategyResult result = strategy.process(ctx, builder);

        assertEquals(StrategyResult.CONTINUE, result);
    }

    @Test
    void getOrder_shouldReturn60() {
        assertEquals(60, strategy.getOrder());
    }

    @Test
    void process_shouldAskOneQuestionAtATime() {
        AiEmployeeReplyStrategy config = new AiEmployeeReplyStrategy();
        config.setStrategyType("COLLECT");
        config.setEnabled(true);
        config.setConfigJson("{\"fields\":[{\"name\":\"phone\",\"question\":\"请问您的手机号是多少？\"},{\"name\":\"email\",\"question\":\"请问您的邮箱是什么？\"}]}");

        when(strategyRepo.findByEmployeeIdAndEnabledOrderBySortOrderAsc(1L, true))
                .thenReturn(List.of(config));

        CustomerProfile customer = new CustomerProfile();
        customer.setId(1L);
        customer.setExtraFields(null);

        ConversationContext ctx = ConversationContext.builder()
                .employee(employee)
                .customer(customer)
                .build();

        ReplyBuilder builder = new ReplyBuilder();
        StrategyResult result = strategy.process(ctx, builder);

        assertEquals(StrategyResult.REPLIED, result);
        String reply = builder.build();
        assertTrue(reply.contains("手机号"));
        assertFalse(reply.contains("邮箱"));
    }

    @Test
    void process_shouldContinueWhenFieldsArrayEmpty() {
        AiEmployeeReplyStrategy config = new AiEmployeeReplyStrategy();
        config.setStrategyType("COLLECT");
        config.setEnabled(true);
        config.setConfigJson("{\"fields\":[]}");

        when(strategyRepo.findByEmployeeIdAndEnabledOrderBySortOrderAsc(1L, true))
                .thenReturn(List.of(config));

        CustomerProfile customer = new CustomerProfile();
        customer.setId(1L);

        ConversationContext ctx = ConversationContext.builder()
                .employee(employee)
                .customer(customer)
                .build();

        ReplyBuilder builder = new ReplyBuilder();
        StrategyResult result = strategy.process(ctx, builder);

        assertEquals(StrategyResult.CONTINUE, result);
    }
}
