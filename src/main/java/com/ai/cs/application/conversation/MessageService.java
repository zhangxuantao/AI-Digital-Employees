package com.ai.cs.application.conversation;

import com.ai.cs.domain.conversation.Message;
import com.ai.cs.domain.conversation.repository.MessageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MessageService {

    private final MessageRepository messageRepository;

    @Transactional
    public Message saveMessage(Long conversationId, String senderType, Long senderId,
                                String content, String msgType, String metadata) {
        Message msg = new Message();
        msg.setConversationId(conversationId);
        msg.setSenderType(senderType);
        msg.setSenderId(senderId != null ? String.valueOf(senderId) : null);
        msg.setContent(content);
        msg.setMsgType(msgType != null ? msgType : "text");
        msg.setMetadata(metadata);
        msg.setSendTime(LocalDateTime.now());
        return messageRepository.save(msg);
    }

    public List<Message> getRecentMessages(Long conversationId, int limit) {
        return messageRepository.findTop10ByConversationIdOrderBySendTimeDesc(conversationId)
                .stream().limit(limit).toList();
    }

    public List<Message> getConversationMessages(Long conversationId) {
        return messageRepository.findByConversationIdOrderBySendTimeAsc(conversationId);
    }
}
