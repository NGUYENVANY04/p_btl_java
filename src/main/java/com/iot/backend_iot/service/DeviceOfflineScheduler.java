package com.iot.backend_iot.service;

import com.iot.backend_iot.model.Alert;
import com.iot.backend_iot.model.AlertType;
import com.iot.backend_iot.model.Device;
import com.iot.backend_iot.model.DeviceStatus;
import com.iot.backend_iot.repository.AlertRepository;
import com.iot.backend_iot.repository.DeviceRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Component
public class DeviceOfflineScheduler {
    private final DeviceRepository deviceRepository;
    private final AlertRepository alertRepository;

    private final long offlineThresholdSeconds;

    public DeviceOfflineScheduler(
            DeviceRepository deviceRepository,
            AlertRepository alertRepository,
            @Value("${iot.device.offlineThresholdSeconds:60}") long offlineThresholdSeconds
    ) {
        this.deviceRepository = deviceRepository;
        this.alertRepository = alertRepository;
        this.offlineThresholdSeconds = offlineThresholdSeconds;
    }

    @Scheduled(fixedDelayString = "${iot.device.offlineCheckMs:60000}")
    @Transactional
    public void detectOfflineDevices() {
        LocalDateTime cutoff = LocalDateTime.now().minusSeconds(offlineThresholdSeconds);
        List<Device> onlineDevices = deviceRepository.findByStatus(DeviceStatus.ONLINE);
        for (Device d : onlineDevices) {
            LocalDateTime lastSeen = d.getLastSeen();
            if (lastSeen == null || lastSeen.isBefore(cutoff)) {
                d.setStatus(DeviceStatus.OFFLINE);
                deviceRepository.save(d);

                boolean exists = alertRepository.existsByDeviceIdAndTypeAndResolvedFalseAndMetricIsNull(
                        d.getDeviceId(),
                        AlertType.OFFLINE
                );
                if (!exists) {
                    String msg = "Thiet bi offline (khong gui du lieu > " + offlineThresholdSeconds + "s)";
                    alertRepository.save(Alert.offline(d.getDeviceId(), msg));
                }
            }
        }
    }
}

