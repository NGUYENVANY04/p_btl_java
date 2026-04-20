package com.Iot.backend.dto;

public class OfflineDeviceDto {
    private Integer device_id;
    private String last_seen;
    private long seconds_since_last_seen;
    private long minutes_since_last_seen;

    public Integer getDevice_id() {
        return device_id;
    }

    public void setDevice_id(Integer device_id) {
        this.device_id = device_id;
    }

    public String getLast_seen() {
        return last_seen;
    }

    public void setLast_seen(String last_seen) {
        this.last_seen = last_seen;
    }

    public long getSeconds_since_last_seen() {
        return seconds_since_last_seen;
    }

    public void setSeconds_since_last_seen(long seconds_since_last_seen) {
        this.seconds_since_last_seen = seconds_since_last_seen;
    }

    public long getMinutes_since_last_seen() {
        return minutes_since_last_seen;
    }

    public void setMinutes_since_last_seen(long minutes_since_last_seen) {
        this.minutes_since_last_seen = minutes_since_last_seen;
    }
}