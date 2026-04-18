package com.example.demo.dto;

public record MonitorFiltersResponse(
        Integer deviceId,
        String from,
        String to,
        String bucket) {
}
