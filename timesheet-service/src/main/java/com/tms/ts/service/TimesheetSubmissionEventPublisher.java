package com.tms.ts.service;

import com.tms.ts.config.RabbitMQConfig;
import com.tms.ts.event.TimesheetSubmittedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class TimesheetSubmissionEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(TimesheetSubmissionEventPublisher.class);

    private final RabbitTemplate rabbitTemplate;

    public TimesheetSubmissionEventPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    @Async
    public void publishTimesheetSubmittedEvent(TimesheetSubmittedEvent event) {
        try {
            rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE, RabbitMQConfig.TIMESHEET_ROUTING_KEY, event);
        } catch (Exception ex) {
            log.error("Failed to publish timesheet submitted event for timesheet {}", event.getTimesheetId(), ex);
        }
    }
}
