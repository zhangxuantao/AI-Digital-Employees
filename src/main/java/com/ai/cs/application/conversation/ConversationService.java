package com.ai.cs.application.conversation;

import com.ai.cs.domain.conversation.Conversation;
import com.ai.cs.domain.conversation.ConversationStatus;
import com.ai.cs.domain.conversation.repository.ConversationRepository;
import com.ai.cs.shared.exception.BusinessException;
import com.ai.cs.shared.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ConversationService {

    private final ConversationRepository conversationRepository;
    private final SessionStateMachine stateMachine;

    @Transactional
    public Conversation findOrCreateConversation(Long customerId, Long employeeId, String channel) {
        // 1. Try to find existing active conversation
        var recent = conversationRepository
                .findActiveByCustomerSince(customerId, LocalDateTime.now().minusDays(30));
        if (!recent.isEmpty()) {
            return recent.get(0);
        }

        // 2. Try to create — with duplicate key retry
        try {
            Conversation conv = new Conversation();
            conv.setCustomerId(customerId);
            conv.setEmployeeId(employeeId);
            conv.setChannel(channel);
            conv.setStatus(ConversationStatus.AI_ACTIVE);
            conv.setStartTime(LocalDateTime.now());
            return conversationRepository.saveAndFlush(conv);
        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            // Race condition: another thread created the conversation first
            // Retry the find
            log.info("并发创建会话冲突，重试查找: customerId={}", customerId);
            var retry = conversationRepository
                    .findActiveByCustomerSince(customerId, LocalDateTime.now().minusDays(30));
            if (!retry.isEmpty()) {
                return retry.get(0);
            }
            throw e; // Should not happen after flush, but safety net
        }
    }

    @Transactional
    public void transferToHuman(Long conversationId, Long agentId) {
        Conversation conv = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CONVERSATION_NOT_FOUND));
        stateMachine.transition(conv, ConversationStatus.HUMAN);
        conv.setHumanAgentId(agentId);
        conv.setOwnerAgentId(agentId);
        conversationRepository.save(conv);
        log.info("会话 {} 转人工客服: agentId={}", conversationId, agentId);
    }

    @Transactional
    public Conversation closeConversation(Long conversationId, String reason) {
        Conversation conv = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CONVERSATION_NOT_FOUND));
        stateMachine.transition(conv, ConversationStatus.CLOSED);
        conv.setEndTime(LocalDateTime.now());
        conv.setCloseReason(reason);
        Conversation saved = conversationRepository.save(conv);
        log.info("会话 {} 已关闭: reason={}", conversationId, reason);
        return saved;
    }

    @Transactional
    public void queueConversation(Long conversationId) {
        Conversation conv = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CONVERSATION_NOT_FOUND));
        stateMachine.transition(conv, ConversationStatus.QUEUED);
        conversationRepository.save(conv);
        log.info("会话 {} 已加入排队", conversationId);
    }

    @Transactional
    public void markWaiting(Long conversationId) {
        Conversation conv = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CONVERSATION_NOT_FOUND));
        stateMachine.transition(conv, ConversationStatus.WAITING);
        conversationRepository.save(conv);
        log.info("会话 {} 标记为等待中", conversationId);
    }
}
