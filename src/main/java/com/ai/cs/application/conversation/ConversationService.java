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
        LocalDateTime since = LocalDateTime.now().minusDays(30);
        List<Conversation> activeList = conversationRepository.findActiveByCustomerSince(customerId, since);
        Conversation existing = activeList.stream()
                .filter(c -> c.getStatus() != ConversationStatus.CLOSED)
                .findFirst()
                .orElse(null);
        if (existing != null) {
            log.info("找到已有会话: id={}, status={}", existing.getId(), existing.getStatus());
            return existing;
        }
        Conversation conv = new Conversation();
        conv.setCustomerId(customerId);
        conv.setEmployeeId(employeeId);
        conv.setChannel(channel);
        conv.setStatus(ConversationStatus.AI_ACTIVE);
        conv.setStartTime(LocalDateTime.now());
        Conversation saved = conversationRepository.save(conv);
        log.info("创建新会话: id={}", saved.getId());
        return saved;
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
    public void closeConversation(Long conversationId, String reason) {
        Conversation conv = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CONVERSATION_NOT_FOUND));
        stateMachine.transition(conv, ConversationStatus.CLOSED);
        conv.setEndTime(LocalDateTime.now());
        conv.setCloseReason(reason);
        conversationRepository.save(conv);
        log.info("会话 {} 已关闭: reason={}", conversationId, reason);
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
