package com.Iot.backend.dto;

public record MonitorSummaryResponse(
        int totalRows,
        int alertCount,
        int breachRows,
        Double totalEnergy,
        String latestTimestamp) {
}
