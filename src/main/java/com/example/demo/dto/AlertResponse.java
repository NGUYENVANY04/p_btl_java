package com.example.demo.dto;

public record AlertResponse(
        Long id,
        Integer deviceId,
        String type,
        String message,
        String createdAt,
        Boolean isRead) {
}
