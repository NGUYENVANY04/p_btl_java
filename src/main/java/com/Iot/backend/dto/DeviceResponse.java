package com.Iot.backend.dto;

public record DeviceResponse(
        Integer id,
        String name,
        String location,
        Boolean status) {
}
