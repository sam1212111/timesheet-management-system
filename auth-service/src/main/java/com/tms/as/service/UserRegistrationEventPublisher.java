package com.tms.as.service;

import com.tms.as.config.RabbitMQConfig;
import com.tms.as.entity.User;
import com.tms.common.event.UserRegisteredEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class UserRegistrationEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(UserRegistrationEventPublisher.class);

    private final RabbitTemplate rabbitTemplate;

    public UserRegistrationEventPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    @Async
    public void publishUserRegisteredEvent(User user) {
        try {
            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.NOTIFICATION_EXCHANGE,
                    RabbitMQConfig.USER_REGISTERED_ROUTING_KEY,
                    new UserRegisteredEvent(user.getId(), user.getFullName(), user.getEmail())
            );
            log.info("Published UserRegisteredEvent for {}", user.getEmail());
        } catch (Exception ex) {
            log.error("Failed to publish UserRegisteredEvent for {}", user.getEmail(), ex);
        }
    }
}
