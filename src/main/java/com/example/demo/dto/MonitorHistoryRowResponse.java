package com.example.demo.dto;

import java.util.List;

public record MonitorHistoryRowResponse(
        String createdAt,
        Integer deviceId,
        String deviceName,
        Double voltage,
        Double current,
        Double power,
        Double energy,
        boolean isThresholdBreached,
        List<String> breachTypes) {
}
