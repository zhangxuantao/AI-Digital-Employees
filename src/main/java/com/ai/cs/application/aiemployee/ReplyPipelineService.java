package com.ai.cs.application.aiemployee;

import com.ai.cs.application.aiemployee.strategy.*;
import com.ai.cs.application.conversation.MessageService;
import com.ai.cs.domain.conversation.Conversation;
import com.ai.cs.domain.conversation.repository.ConversationRepository;
import com.ai.cs.domain.customer.CustomerProfile;
import com.ai.cs.domain.customer.repository.CustomerProfileRepository;
import com.ai.cs.domain.employee.AiEmployee;
import com.ai.cs.domain.employee.repository.AiEmployeeRepository;
import com.ai.cs.shared.exception.BusinessException;
import com.ai.cs.shared.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReplyPipelineService {

    private final List<ReplyStrategyNode> strategyNodes;
    private final ConversationRepository conversationRepository;
    private final AiEmployeeRepository employeeRepository;
    private final CustomerProfileRepository customerRepository;
    private final MessageService messageService;

    @Transactional
    public String process(Long conversationId, Long employeeId, String customerMessage) {
        Conversation conv = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CONVERSATION_NOT_FOUND));
        AiEmployee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new BusinessException(ErrorCode.AI_EMPLOYEE_NOT_FOUND));
        CustomerProfile customer = customerRepository.findById(conv.getCustomerId()).orElse(null);

        // Save customer message
        messageService.saveMessage(conversationId, "CUSTOMER", conv.getCustomerId(), customerMessage, "text", null);

        // Get recent history
        var recentMsgs = messageService.getRecentMessages(conversationId, 10);
        List<String> history = recentMsgs.stream()
                .map(m -> "[" + m.getSenderType() + "]:" + m.getContent())
                .toList();

        // Build context
        ConversationContext ctx = ConversationContext.builder()
                .conversationId(conversationId)
                .conversation(conv)
                .employee(employee)
                .customer(customer)
                .customerMessage(customerMessage)
                .recentHistory(history)
                .build();

        // Execute pipeline
        ReplyBuilder builder = new ReplyBuilder();
        List<ReplyStrategyNode> sortedNodes = strategyNodes.stream()
                .sorted(Comparator.comparingInt(ReplyStrategyNode::getOrder))
                .toList();

        for (ReplyStrategyNode node : sortedNodes) {
            StrategyResult result = node.process(ctx, builder);
            if (result == StrategyResult.INTERRUPT) {
                log.info("策略节点 {} 中断流水线", node.getClass().getSimpleName());
                break;
            }
            if (result == StrategyResult.REPLIED && builder.hasContent()) {
                break;
            }
        }

        // Save AI reply
        String reply = builder.build();
        if (reply != null && !reply.isEmpty()) {
            messageService.saveMessage(conversationId, "AI", employeeId, reply, "text", null);
        }

        return reply;
    }
}
