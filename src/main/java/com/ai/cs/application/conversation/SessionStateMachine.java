package com.ai.cs.application.conversation;

import com.ai.cs.domain.conversation.Conversation;
import com.ai.cs.domain.conversation.ConversationStatus;
import com.ai.cs.shared.exception.BusinessException;
import com.ai.cs.shared.exception.ErrorCode;
import org.springframework.stereotype.Component;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

@Component
public class SessionStateMachine {

    private static final Map<ConversationStatus, Set<ConversationStatus>> ALLOWED = Map.of(
        ConversationStatus.AI_ACTIVE, EnumSet.of(ConversationStatus.WAITING, ConversationStatus.CLOSED),
        ConversationStatus.WAITING, EnumSet.of(ConversationStatus.HUMAN, ConversationStatus.QUEUED, ConversationStatus.CLOSED),
        ConversationStatus.HUMAN, EnumSet.of(ConversationStatus.CLOSED),
        ConversationStatus.QUEUED, EnumSet.of(ConversationStatus.HUMAN, ConversationStatus.CLOSED),
        ConversationStatus.CLOSED, EnumSet.noneOf(ConversationStatus.class)
    );

    public void transition(Conversation conv, ConversationStatus target) {
        Set<ConversationStatus> allowed = ALLOWED.get(conv.getStatus());
        if (allowed == null || !allowed.contains(target)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST,
                    "不允许从 " + conv.getStatus() + " 转换到 " + target);
        }
        conv.setStatus(target);
    }
}
