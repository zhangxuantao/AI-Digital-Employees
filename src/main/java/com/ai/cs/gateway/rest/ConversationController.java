package com.ai.cs.gateway.rest;

import com.ai.cs.application.conversation.ConversationService;
import com.ai.cs.application.conversation.MessageService;
import com.ai.cs.domain.conversation.Conversation;
import com.ai.cs.domain.conversation.Message;
import com.ai.cs.domain.conversation.repository.ConversationRepository;
import com.ai.cs.shared.dto.ApiResponse;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/conversations")
@RequiredArgsConstructor
public class ConversationController {

    private final ConversationRepository conversationRepository;
    private final ConversationService conversationService;
    private final MessageService messageService;

    @GetMapping
    @PreAuthorize("hasAuthority('im:access')")
    public ApiResponse<List<Conversation>> listConversations() {
        return ApiResponse.success(conversationRepository.findAll());
    }

    @GetMapping("/{id}/messages")
    @PreAuthorize("hasAuthority('im:access')")
    public ApiResponse<List<Message>> getMessages(@PathVariable Long id) {
        return ApiResponse.success(messageService.getConversationMessages(id));
    }

    @PostMapping("/{id}/close")
    @PreAuthorize("hasAuthority('im:access')")
    public ApiResponse<Conversation> closeConversation(@PathVariable Long id,
                                                        @RequestBody(required = false) CloseRequest request) {
        String reason = (request != null && request.getReason() != null)
                ? request.getReason() : "客服主动关闭";
        return ApiResponse.success(conversationService.closeConversation(id, reason));
    }

    @Data
    public static class CloseRequest {
        private String reason;
    }
}
