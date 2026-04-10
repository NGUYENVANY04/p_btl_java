package com.example.demo.iot.service;

import com.example.demo.iot.model.Alert;
import com.example.demo.iot.model.AlertType;
import com.example.demo.iot.model.Device;
import com.example.demo.iot.model.DeviceStatus;
import com.example.demo.iot.model.SensorData;
import com.example.demo.iot.repository.AlertRepository;
import com.example.demo.iot.repository.DeviceRepository;
import com.example.demo.iot.repository.SensorRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Objects;

@Service
public class TelemetryService {
    private final SensorRepository sensorRepository;
    private final DeviceRepository deviceRepository;
    private final AlertRepository alertRepository;

    public TelemetryService(
            SensorRepository sensorRepository,
            DeviceRepository deviceRepository,
            AlertRepository alertRepository
    ) {
        this.sensorRepository = sensorRepository;
        this.deviceRepository = deviceRepository;
        this.alertRepository = alertRepository;
    }

    @Transactional
    public void handleIncomingTelemetry(SensorData data) {
        String deviceId = normalizeDeviceId(data.getDeviceId());
        data.setDeviceId(deviceId);

        Device device = deviceRepository.findById(deviceId).orElseGet(() -> new Device(deviceId));
        device.setLastSeen(LocalDateTime.now());
        device.setStatus(DeviceStatus.ONLINE);
        deviceRepository.save(device);

        sensorRepository.save(data);

        checkThreshold(device, "voltage", data.getVoltage(), device.getMaxVoltage());
        checkThreshold(device, "current", data.getCurrent(), device.getMaxCurrent());
        checkThreshold(device, "power", data.getPower(), device.getMaxPower());
        checkThreshold(device, "totalKwh", data.getTotalKwh(), device.getMaxTotalKwh());
    }

    private void checkThreshold(Device device, String metric, Double value, Double threshold) {
        if (threshold == null || value == null) return;
        if (Double.compare(value, threshold) <= 0) return;

        boolean exists = alertRepository.existsByDeviceIdAndTypeAndMetricAndResolvedFalse(
                device.getDeviceId(),
                AlertType.THRESHOLD_EXCEEDED,
                metric
        );
        if (exists) return;

        String message = "Vuot nguong " + metric + ": " + value + " > " + threshold;
        alertRepository.save(Alert.thresholdExceeded(device.getDeviceId(), metric, value, threshold, message));
    }

    private String normalizeDeviceId(String deviceId) {
        String trimmed = deviceId == null ? null : deviceId.trim();
        if (trimmed == null || trimmed.isBlank()) {
            throw new IllegalArgumentException("deviceId is missing in payload");
        }
        return Objects.requireNonNull(trimmed);
    }
}

