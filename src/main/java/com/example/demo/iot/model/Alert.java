package com.example.demo.iot.model;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Table(name = "alert")
@Data
public class Alert {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String deviceId;

    @Enumerated(EnumType.STRING)
    private AlertType type;

    // e.g. "voltage", "power" for threshold alerts; null for offline
    private String metric;

    private Double value;
    private Double threshold;

    private String message;

    private boolean resolved = false;

    private LocalDateTime createdAt = LocalDateTime.now();

    public static Alert offline(String deviceId, String message) {
        Alert a = new Alert();
        a.deviceId = deviceId;
        a.type = AlertType.OFFLINE;
        a.message = message;
        return a;
    }

    public static Alert thresholdExceeded(String deviceId, String metric, Double value, Double threshold, String message) {
        Alert a = new Alert();
        a.deviceId = deviceId;
        a.type = AlertType.THRESHOLD_EXCEEDED;
        a.metric = metric;
        a.value = value;
        a.threshold = threshold;
        a.message = message;
        return a;
    }
}

