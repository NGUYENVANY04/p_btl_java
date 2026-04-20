package com.Iot.backend.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class AlertSchedulerService {

    @Autowired
    private daocuong daocuongService;

    @Autowired
    private EmailService emailService;

    // Check offline devices every 5 minutes
    @Scheduled(fixedRate = 300000) // 5 minutes
    public void checkOfflineDevices() {
        // For all users, but since we need user context, this might need adjustment
        // For now, we'll skip auto-check and rely on manual checks per user
    }

    // Check threshold alerts every 10 minutes
    @Scheduled(fixedRate = 600000) // 10 minutes
    public void checkThresholdAlerts() {
        // Similar issue with user context
    }

    // Send email notification for alerts (can be called after manual checks)
    public void sendAlertNotification(String subject, String body) {
        emailService.sendAlertEmail(subject, body);
    }
}