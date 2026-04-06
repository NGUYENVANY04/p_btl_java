package com.iot.backend_iot.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Data
public class SensorData {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String deviceId;    // ID của thiết bị đo (Học gửi)
    private Double voltage;     // Điện áp (V) - thường quanh mức 220V
    private Double current;     // Dòng điện (A)
    private Double power;       // Công suất tức thời (W) = V * A
    private Double totalKwh;    // Tổng điện năng tiêu thụ lũy kế (kWh)
    
    private LocalDateTime timestamp = LocalDateTime.now();
}