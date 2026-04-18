package com.Iot.backend.dto;

public class DeviceDTO {

    private Long id;
    private String name;
    private String location;
    private Boolean status;

    public DeviceDTO() {}
    public DeviceDTO(Long id, String name, String location, Boolean status) {
        this.id = id;
        this.name = name;
        this.location = location;
        this.status = status;
    }
    public Long getId() {
        return id;
    }
    public void setId(Long id) {
        this.id = id;
    }
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
    public Boolean getStatus() {
        return status;
    }
    public void setStatus(Boolean status) {
        this.status = status;
    }
}