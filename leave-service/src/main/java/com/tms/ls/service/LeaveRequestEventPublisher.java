package com.tms.ls.service;

import com.tms.ls.config.RabbitMQConfig;
import com.tms.ls.dto.LeaveRequestedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class LeaveRequestEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(LeaveRequestEventPublisher.class);

    private final RabbitTemplate rabbitTemplate;

    public LeaveRequestEventPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    @Async
    public void publishLeaveRequestedEvent(LeaveRequestedEvent event) {
        try {
            rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE, RabbitMQConfig.LEAVE_ROUTING_KEY, event);
        } catch (Exception ex) {
            log.error("Failed to publish leave requested event for request {}", event.getRequestId(), ex);
        }
    }
}
