package com.Iot.backend.dto;

import java.util.ArrayList;
import java.util.List;

public class ThresholdCheckResultDto {
    private List<OverLimitDeviceDto> over_limit_devices = new ArrayList<>();
    private int created_alerts;
    private List<String> warnings = new ArrayList<>();

    public List<OverLimitDeviceDto> getOver_limit_devices() {
        return over_limit_devices;
    }

    public void setOver_limit_devices(List<OverLimitDeviceDto> over_limit_devices) {
        this.over_limit_devices = over_limit_devices;
    }

    public int getCreated_alerts() {
        return created_alerts;
    }

    public void setCreated_alerts(int created_alerts) {
        this.created_alerts = created_alerts;
    }

    public List<String> getWarnings() {
        return warnings;
    }

    public void setWarnings(List<String> warnings) {
        this.warnings = warnings;
    }
}
