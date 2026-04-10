package com.example.demo.iot.model;

import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Table(name = "device")
@Data
public class Device {
    @Id
    private String deviceId;

    @Enumerated(EnumType.STRING)
    private DeviceStatus status = DeviceStatus.ONLINE;

    private LocalDateTime lastSeen = LocalDateTime.now();

    private Double maxVoltage;
    private Double maxCurrent;
    private Double maxPower;
    private Double maxTotalKwh;

    public Device() {}

    public Device(String deviceId) {
        this.deviceId = deviceId;
    }
}

