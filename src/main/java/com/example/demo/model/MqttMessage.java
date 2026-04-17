package com.example.demo.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "sensor_data")
public class MqttMessage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Double power;
    private Double voltage;
    private Double current;
    private LocalDateTime timestamp;

    public MqttMessage() {
    }

    public MqttMessage(Double power, Double voltage, Double current, LocalDateTime timestamp) {
        this.power = power;
        this.voltage = voltage;
        this.current = current;
        this.timestamp = timestamp;
    }

    // --- BẮT BUỘC PHẢI CÓ GETTER CHO ID ---
    public Long getId() {
        return id;
    }

    public Double getPower() {
        return power;
    }

    public Double getVoltage() {
        return voltage;
    }

    public Double getCurrent() {
        return current;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    // Nên thêm các hàm Setter để Spring JPA hoạt động ổn định hơn
    public void setId(Long id) {
        this.id = id;
    }

    public void setPower(Double power) {
        this.power = power;
    }

    public void setVoltage(Double voltage) {
        this.voltage = voltage;
    }

    public void setCurrent(Double current) {
        this.current = current;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
}