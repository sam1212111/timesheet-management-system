package com.tms.admin.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {
    private static final String TIMESHEET_ROUTING_KEY = "timesheet.submitted";
    private static final String LEAVE_ROUTING_KEY = "leave.requested";
    private static final String USER_REGISTERED_ROUTING_KEY = "user.registered";
    private static final String ADMIN_TIMESHEET_QUEUE = "admin.timesheet.queue";
    private static final String ADMIN_LEAVE_QUEUE = "admin.leave.queue";
    private static final String USER_REGISTERED_QUEUE = "notification.user.registered.queue";
    private static final String ADMIN_TIMESHEET_DLX = "admin.timesheet.dlx";
    private static final String ADMIN_LEAVE_DLX = "admin.leave.dlx";
    private static final String USER_REGISTERED_DLX = "notification.user.registered.dlx";
    private static final String ADMIN_TIMESHEET_DLQ = "admin.timesheet.queue.dlq";
    private static final String ADMIN_LEAVE_DLQ = "admin.leave.queue.dlq";
    private static final String USER_REGISTERED_DLQ = "notification.user.registered.queue.dlq";

    // Exchanges
    @Bean
    public TopicExchange timesheetExchange() {
        return new TopicExchange("timesheet.exchange");
    }

    @Bean
    public TopicExchange leaveExchange() {
        return new TopicExchange("leave.exchange");
    }
    
    @Bean
    public TopicExchange adminExchange() {
        return new TopicExchange("admin.exchange");
    }

    @Bean
    public TopicExchange notificationExchange() {
        return new TopicExchange("notification.exchange");
    }

    @Bean
    public TopicExchange adminTimesheetDlx() {
        return new TopicExchange(ADMIN_TIMESHEET_DLX);
    }

    @Bean
    public TopicExchange adminLeaveDlx() {
        return new TopicExchange(ADMIN_LEAVE_DLX);
    }

    @Bean
    public TopicExchange userRegisteredDlx() {
        return new TopicExchange(USER_REGISTERED_DLX);
    }

    // Queues
    @Bean
    public Queue adminTimesheetQueue() {
        return QueueBuilder.durable(ADMIN_TIMESHEET_QUEUE)
                .withArgument("x-dead-letter-exchange", ADMIN_TIMESHEET_DLX)
                .withArgument("x-dead-letter-routing-key", ADMIN_TIMESHEET_DLQ)
                .build();
    }

    @Bean
    public Queue adminLeaveQueue() {
        return QueueBuilder.durable(ADMIN_LEAVE_QUEUE)
                .withArgument("x-dead-letter-exchange", ADMIN_LEAVE_DLX)
                .withArgument("x-dead-letter-routing-key", ADMIN_LEAVE_DLQ)
                .build();
    }

    @Bean
    public Queue userRegisteredQueue() {
        return QueueBuilder.durable(USER_REGISTERED_QUEUE)
                .withArgument("x-dead-letter-exchange", USER_REGISTERED_DLX)
                .withArgument("x-dead-letter-routing-key", USER_REGISTERED_DLQ)
                .build();
    }

    @Bean
    public Queue adminTimesheetDlq() {
        return QueueBuilder.durable(ADMIN_TIMESHEET_DLQ).build();
    }

    @Bean
    public Queue adminLeaveDlq() {
        return QueueBuilder.durable(ADMIN_LEAVE_DLQ).build();
    }

    @Bean
    public Queue userRegisteredDlq() {
        return QueueBuilder.durable(USER_REGISTERED_DLQ).build();
    }

    // Bindings
    @Bean
    public Binding bindingTimesheetToAdmin(
            @org.springframework.beans.factory.annotation.Qualifier("adminTimesheetQueue") Queue adminTimesheetQueue, 
            @org.springframework.beans.factory.annotation.Qualifier("timesheetExchange") TopicExchange timesheetExchange) {
        return BindingBuilder.bind(adminTimesheetQueue).to(timesheetExchange).with(TIMESHEET_ROUTING_KEY);
    }

    @Bean
    public Binding bindingLeaveToAdmin(
            @org.springframework.beans.factory.annotation.Qualifier("adminLeaveQueue") Queue adminLeaveQueue, 
            @org.springframework.beans.factory.annotation.Qualifier("leaveExchange") TopicExchange leaveExchange) {
        return BindingBuilder.bind(adminLeaveQueue).to(leaveExchange).with(LEAVE_ROUTING_KEY);
    }

    @Bean
    public Binding bindingUserRegisteredToNotification(
            @org.springframework.beans.factory.annotation.Qualifier("userRegisteredQueue") Queue userRegisteredQueue,
            @org.springframework.beans.factory.annotation.Qualifier("notificationExchange") TopicExchange notificationExchange) {
        return BindingBuilder.bind(userRegisteredQueue).to(notificationExchange).with(USER_REGISTERED_ROUTING_KEY);
    }

    @Bean
    public Binding bindAdminTimesheetDlq(
            @org.springframework.beans.factory.annotation.Qualifier("adminTimesheetDlq") Queue adminTimesheetDlq,
            @org.springframework.beans.factory.annotation.Qualifier("adminTimesheetDlx") TopicExchange adminTimesheetDlx) {
        return BindingBuilder.bind(adminTimesheetDlq).to(adminTimesheetDlx).with(ADMIN_TIMESHEET_DLQ);
    }

    @Bean
    public Binding bindAdminLeaveDlq(
            @org.springframework.beans.factory.annotation.Qualifier("adminLeaveDlq") Queue adminLeaveDlq,
            @org.springframework.beans.factory.annotation.Qualifier("adminLeaveDlx") TopicExchange adminLeaveDlx) {
        return BindingBuilder.bind(adminLeaveDlq).to(adminLeaveDlx).with(ADMIN_LEAVE_DLQ);
    }

    @Bean
    public Binding bindUserRegisteredDlq(
            @org.springframework.beans.factory.annotation.Qualifier("userRegisteredDlq") Queue userRegisteredDlq,
            @org.springframework.beans.factory.annotation.Qualifier("userRegisteredDlx") TopicExchange userRegisteredDlx) {
        return BindingBuilder.bind(userRegisteredDlq).to(userRegisteredDlx).with(USER_REGISTERED_DLQ);
    }

    // Converters
    @Bean
    public Jackson2JsonMessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(messageConverter());
        return template;
    }
}
