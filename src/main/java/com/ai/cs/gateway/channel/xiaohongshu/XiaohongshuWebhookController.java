package com.ai.cs.gateway.channel.xiaohongshu;

import com.ai.cs.application.aiemployee.ReplyPipelineService;
import com.ai.cs.application.conversation.ConversationService;
import com.ai.cs.domain.customer.CustomerProfile;
import com.ai.cs.domain.employee.AiEmployeeAccount;
import com.ai.cs.domain.employee.repository.AiEmployeeAccountRepository;
import com.ai.cs.domain.employee.repository.AiEmployeeRepository;
import com.ai.cs.gateway.channel.spi.Message;
import com.ai.cs.shared.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/webhook/xiaohongshu")
@RequiredArgsConstructor
public class XiaohongshuWebhookController {

    private final XiaohongshuChannelAdapter adapter;
    private final AiEmployeeAccountRepository accountRepo;
    private final AiEmployeeRepository employeeRepo;
    private final ConversationService conversationService;
    private final ReplyPipelineService replyPipelineService;

    @PostMapping("/message")
    public ApiResponse<String> receiveMessage(@RequestBody Map<String, Object> body,
                                               @RequestHeader(value = "X-XHS-Signature", required = false) String signature) {
        // Phase 2: verify webhook signature
        log.info("小红书Webhook收到消息: {}", body);

        try {
            Message msg = adapter.normalize(body);

            AiEmployeeAccount account = accountRepo.findByPlatformAndAccountId("XIAOHONGSHU",
                    msg.getAppId()).orElse(null);
            if (account == null) {
                return ApiResponse.success("no employee configured");
            }

            var employee = employeeRepo.findById(account.getEmployeeId()).orElse(null);
            if (employee == null || !"ENABLED".equals(employee.getStatus())) {
                return ApiResponse.success("employee inactive");
            }

            CustomerProfile customer = adapter.syncCustomer(msg.getSenderOpenid());
            var conv = conversationService.findOrCreateConversation(customer.getId(), employee.getId(), "XIAOHONGSHU");
            replyPipelineService.process(conv.getId(), employee.getId(), msg.getContent());

            return ApiResponse.success("ok");
        } catch (Exception e) {
            log.error("小红书消息处理失败", e);
            return ApiResponse.error(500, "处理失败");
        }
    }

    @GetMapping("/health")
    public ApiResponse<String> health() {
        return ApiResponse.success("ok");
    }
}
