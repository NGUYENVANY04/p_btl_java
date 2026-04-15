package com.example.demo.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "sensor_data")
public class MqttMessage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Double p;
    private Double v;
    private Double i;
    private LocalDateTime timestamp;

    public MqttMessage() {
    }

    public MqttMessage(Double p, Double v, Double i, LocalDateTime timestamp) {
        this.p = p;
        this.v = v;
        this.i = i;
        this.timestamp = timestamp;
    }

    // --- BẮT BUỘC PHẢI CÓ GETTER CHO ID ---
    public Long getId() {
        return id;
    }

    public Double getP() {
        return p;
    }

    public Double getV() {
        return v;
    }

    public Double getI() {
        return i;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    // Nên thêm các hàm Setter để Spring JPA hoạt động ổn định hơn
    public void setId(Long id) {
        this.id = id;
    }

    public void setP(Double p) {
        this.p = p;
    }

    public void setV(Double v) {
        this.v = v;
    }

    public void setI(Double i) {
        this.i = i;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
}