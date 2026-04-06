package com.iot.backend_iot.dto;

import lombok.Data;

@Data
public class ThresholdConfigRequest {
    private Double maxVoltage;
    private Double maxCurrent;
    private Double maxPower;
    private Double maxTotalKwh;
}

