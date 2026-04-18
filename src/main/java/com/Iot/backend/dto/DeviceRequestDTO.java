package com.Iot.backend.dto;

public class DeviceRequestDTO {

    private String name;
    private String location;

    public DeviceRequestDTO() {}

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }
}