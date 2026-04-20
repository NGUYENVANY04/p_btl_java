package com.Iot.backend.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    @Value("${alert.email.to}")
    private String defaultToEmail;

    @Value("${alert.email.from:${spring.mail.username}}")
    private String fromEmail;

    public void sendAlertEmail(String subject, String body) {
        sendAlertEmail(defaultToEmail, subject, body);
    }

    public void sendAlertEmail(String toEmail, String subject, String body) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(toEmail);
            message.setSubject(subject);
            message.setText(body);
            mailSender.send(message);
            System.out.println("📧 Email sent: " + subject + " to " + toEmail);
        } catch (Exception e) {
            System.err.println("❌ Lỗi gửi email: " + e.getMessage());
        }
    }
}