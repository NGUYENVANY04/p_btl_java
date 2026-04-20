package com.Iot.backend.dto;

public record MonitorFiltersResponse(
        Integer deviceId,
        String from,
        String to,
        String bucket) {
}
