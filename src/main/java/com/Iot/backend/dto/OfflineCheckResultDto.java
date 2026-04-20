package com.Iot.backend.dto;

import java.util.ArrayList;
import java.util.List;

public class OfflineCheckResultDto {
    private int threshold_seconds;
    private List<OfflineDeviceDto> offline_devices = new ArrayList<>();
    private int created_alerts;
    private List<String> warnings = new ArrayList<>();

    public int getThreshold_seconds() {
        return threshold_seconds;
    }

    public void setThreshold_seconds(int threshold_seconds) {
        this.threshold_seconds = threshold_seconds;
    }

    public List<OfflineDeviceDto> getOffline_devices() {
        return offline_devices;
    }

    public void setOffline_devices(List<OfflineDeviceDto> offline_devices) {
        this.offline_devices = offline_devices;
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