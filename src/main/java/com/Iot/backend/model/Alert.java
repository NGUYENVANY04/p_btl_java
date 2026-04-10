package com.example.demo.model;

public class Alert {

    private Long id; // ✅ thêm dòng này

    private Integer device_id;
    private String type;
    private String message;
    private Boolean is_read;

    public Alert() {
    }

    // ===== ID =====
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    // ===== device_id =====
    public Integer getDevice_id() {
        return device_id;
    }

    public void setDevice_id(Integer device_id) {
        this.device_id = device_id;
    }

    // ===== type =====
    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    // ===== message =====
    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    // ===== is_read =====
    public Boolean getIs_read() {
        return is_read;
    }

    public void setIs_read(Boolean is_read) {
        this.is_read = is_read;
    }
}