package com.tms.admin.messaging;

import com.tms.admin.dto.LeaveRequestedEvent;
import com.tms.admin.dto.TimesheetSubmittedEvent;
import com.tms.admin.entity.ApprovalTask;
import com.tms.admin.entity.TargetType;
import com.tms.admin.repository.ApprovalTaskRepository;
import com.tms.admin.service.WelcomeEmailService;
import com.tms.common.event.UserRegisteredEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Component
public class EventConsumer {

    private static final Logger log = LoggerFactory.getLogger(EventConsumer.class);
    private final ApprovalTaskRepository taskRepository;
    private final WelcomeEmailService welcomeEmailService;

    public EventConsumer(ApprovalTaskRepository taskRepository,
                         WelcomeEmailService welcomeEmailService) {
        this.taskRepository = taskRepository;
        this.welcomeEmailService = welcomeEmailService;
    }

    @RabbitListener(queues = "admin.timesheet.queue")
    @Transactional
    public void handleTimesheetSubmitted(TimesheetSubmittedEvent event) {
        log.info("Received TimesheetSubmittedEvent for timesheet: {}", event.getTimesheetId());
        
        Optional<ApprovalTask> existing = taskRepository.findByTargetTypeAndTargetId(TargetType.TIMESHEET, event.getTimesheetId());
        if (existing.isEmpty()) {
            ApprovalTask task = new ApprovalTask(
                    TargetType.TIMESHEET,
                    event.getTimesheetId(),
                    event.getEmployeeId(),
                    event.getApproverId()
            );
            taskRepository.save(task);
            log.info("Approval task created for timesheet: {}", event.getTimesheetId());
        }
    }

    @RabbitListener(queues = "admin.leave.queue")
    @Transactional
    public void handleLeaveRequested(LeaveRequestedEvent event) {
        log.info("Received LeaveRequestedEvent for request: {}", event.getRequestId());

        Optional<ApprovalTask> existing = taskRepository.findByTargetTypeAndTargetId(TargetType.LEAVE, event.getRequestId());
        if (existing.isEmpty()) {
            ApprovalTask task = new ApprovalTask(
                    TargetType.LEAVE,
                    event.getRequestId(),
                    event.getEmployeeId(),
                    event.getApproverId()
            );
            taskRepository.save(task);
            log.info("Approval task created for leave request: {}", event.getRequestId());
        }
    }

    @RabbitListener(queues = "notification.user.registered.queue")
    public void handleUserRegistered(UserRegisteredEvent event) {
        log.info("Received UserRegisteredEvent for email: {}", event.getEmail());
        try {
            welcomeEmailService.sendWelcomeEmail(event);
        } catch (Exception ex) {
            log.error("Failed to send welcome email to {}", event.getEmail(), ex);
            throw new AmqpRejectAndDontRequeueException("Welcome email handling failed", ex);
        }
    }
}
