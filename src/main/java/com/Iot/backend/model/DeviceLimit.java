package com.example.demo.model;

import java.time.OffsetDateTime;

public class DeviceLimit {
    private Integer id;
    private Integer device_id;
    private Float max_power;
    private Float max_current;
    private OffsetDateTime created_at;

    public DeviceLimit() {
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getDevice_id() {
        return device_id;
    }

    public void setDevice_id(Integer device_id) {
        this.device_id = device_id;
    }

    public Float getMax_power() {
        return max_power;
    }

    public void setMax_power(Float max_power) {
        this.max_power = max_power;
    }

    public Float getMax_current() {
        return max_current;
    }

    public void setMax_current(Float max_current) {
        this.max_current = max_current;
    }

    public OffsetDateTime getCreated_at() {
        return created_at;
    }

    public void setCreated_at(OffsetDateTime created_at) {
        this.created_at = created_at;
    }
}
