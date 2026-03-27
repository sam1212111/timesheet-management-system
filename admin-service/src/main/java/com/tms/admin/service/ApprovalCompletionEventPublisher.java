package com.tms.admin.service;

import com.tms.admin.dto.ApprovalCompletedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class ApprovalCompletionEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(ApprovalCompletionEventPublisher.class);

    private final RabbitTemplate rabbitTemplate;

    public ApprovalCompletionEventPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    @Async
    public void publishApprovalCompletedEvent(ApprovalCompletedEvent event) {
        try {
            rabbitTemplate.convertAndSend("admin.exchange", "approval.completed", event);
        } catch (Exception ex) {
            log.error("Failed to publish approval completed event for target {}", event.getTargetId(), ex);
        }
    }
}
