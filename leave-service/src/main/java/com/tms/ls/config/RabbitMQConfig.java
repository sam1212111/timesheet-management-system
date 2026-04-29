package com.tms.ls.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String EXCHANGE = "leave.exchange";
    public static final String LEAVE_QUEUE = "leave.requested";
    public static final String LEAVE_ROUTING_KEY = "leave.requested";
    public static final String ADMIN_EXCHANGE = "admin.exchange";
    public static final String APPROVAL_COMPLETED_QUEUE = "leave.approval.completed";
    public static final String APPROVAL_COMPLETED_ROUTING_KEY = "approval.completed";
    public static final String APPROVAL_COMPLETED_DLX = "leave.approval.dlx";
    public static final String APPROVAL_COMPLETED_DLQ = "leave.approval.completed.dlq";

    @Bean
    public TopicExchange leaveExchange() {
        return new TopicExchange(EXCHANGE);
    }

    @Bean
    public TopicExchange adminExchange() {
        return new TopicExchange(ADMIN_EXCHANGE);
    }

    @Bean
    public TopicExchange approvalCompletedDlx() {
        return new TopicExchange(APPROVAL_COMPLETED_DLX);
    }

    @Bean
    public Queue leaveQueue() {
        return new Queue(LEAVE_QUEUE, true);
    }

    @Bean
    public Queue approvalCompletedQueue() {
        return QueueBuilder.durable(APPROVAL_COMPLETED_QUEUE)
                .withArgument("x-dead-letter-exchange", APPROVAL_COMPLETED_DLX)
                .withArgument("x-dead-letter-routing-key", APPROVAL_COMPLETED_DLQ)
                .build();
    }

    @Bean
    public Queue approvalCompletedDlq() {
        return QueueBuilder.durable(APPROVAL_COMPLETED_DLQ).build();
    }

    @Bean
    public Binding leaveBinding() {
        return BindingBuilder
                .bind(leaveQueue())
                .to(leaveExchange())
                .with(LEAVE_ROUTING_KEY);
    }

    @Bean
    public Binding approvalCompletedBinding() {
        return BindingBuilder
                .bind(approvalCompletedQueue())
                .to(adminExchange())
                .with(APPROVAL_COMPLETED_ROUTING_KEY);
    }

    @Bean
    public Binding approvalCompletedDlqBinding() {
        return BindingBuilder
                .bind(approvalCompletedDlq())
                .to(approvalCompletedDlx())
                .with(APPROVAL_COMPLETED_DLQ);
    }

    @Bean
    public Jackson2JsonMessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(messageConverter());
        return rabbitTemplate;
    }
}
