package com.example.demo.dto;

public record DeviceResponse(
        Integer id,
        String name,
        String location,
        Boolean status) {
}
