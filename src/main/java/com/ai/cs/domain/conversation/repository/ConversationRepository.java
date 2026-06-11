package com.ai.cs.domain.conversation.repository;

import com.ai.cs.domain.conversation.Conversation;
import com.ai.cs.domain.conversation.ConversationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ConversationRepository extends JpaRepository<Conversation, Long> {

    List<Conversation> findByEmployeeIdAndStatusNot(Long employeeId, ConversationStatus status);

    @Query("SELECT c FROM Conversation c WHERE c.customerId = :customerId AND c.startTime >= :since")
    List<Conversation> findActiveByCustomerSince(@Param("customerId") Long customerId, @Param("since") java.time.LocalDateTime since);

    List<Conversation> findByOwnerAgentId(Long ownerAgentId);

    List<Conversation> findByOwnerAgentIdAndStatus(Long ownerAgentId, ConversationStatus status);

    Optional<Conversation> findByIdAndOwnerAgentId(Long id, Long ownerAgentId);
}
