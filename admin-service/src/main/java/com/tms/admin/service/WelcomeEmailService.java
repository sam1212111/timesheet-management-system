package com.tms.admin.service;

import com.tms.common.event.UserRegisteredEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class WelcomeEmailService {

    private static final Logger log = LoggerFactory.getLogger(WelcomeEmailService.class);

    private final JavaMailSender mailSender;
    private final String fromEmail;

    public WelcomeEmailService(JavaMailSender mailSender,
                               @Value("${app.mail.from:no-reply@tms.local}") String fromEmail) {
        this.mailSender = mailSender;
        this.fromEmail = fromEmail;
    }

    public void sendWelcomeEmail(UserRegisteredEvent event) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(event.getEmail());
        message.setSubject("Welcome to TimeSheet & Leave Management System");
        message.setText(buildBody(event));

        mailSender.send(message);
        log.info("Welcome email sent to {}", event.getEmail());
    }

    private String buildBody(UserRegisteredEvent event) {
        return "Hello " + event.getFullName() + ",\n\n"
                + "Welcome to the TimeSheet & Leave Management System.\n"
                + "Your account has been created successfully.\n\n"
                + "Employee/User ID: " + event.getUserId() + "\n"
                + "Email: " + event.getEmail() + "\n\n"
                + "You can now log in and start using the system.\n\n"
                + "Regards,\n"
                + "TMS Team";
    }
}
