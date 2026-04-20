package com.Iot.backend.dto;

import com.Iot.backend.model.DeviceLimit;
import com.Iot.backend.model.SensorData;

public class OverLimitDeviceDto {
    private Integer device_id;
    private SensorData latest;
    private DeviceLimit limit;
    private boolean power_over;
    private boolean current_over;

    public Integer getDevice_id() {
        return device_id;
    }

    public void setDevice_id(Integer device_id) {
        this.device_id = device_id;
    }

    public SensorData getLatest() {
        return latest;
    }

    public void setLatest(SensorData latest) {
        this.latest = latest;
    }

    public DeviceLimit getLimit() {
        return limit;
    }

    public void setLimit(DeviceLimit limit) {
        this.limit = limit;
    }

    public boolean isPower_over() {
        return power_over;
    }

    public void setPower_over(boolean power_over) {
        this.power_over = power_over;
    }

    public boolean isCurrent_over() {
        return current_over;
    }

    public void setCurrent_over(boolean current_over) {
        this.current_over = current_over;
    }
}