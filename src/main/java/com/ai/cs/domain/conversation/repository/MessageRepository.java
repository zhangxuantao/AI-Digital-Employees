package com.ai.cs.domain.conversation.repository;

import com.ai.cs.domain.conversation.Message;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {

    List<Message> findByConversationIdOrderBySendTimeAsc(Long conversationId);

    List<Message> findTop10ByConversationIdOrderBySendTimeDesc(Long conversationId);
}
