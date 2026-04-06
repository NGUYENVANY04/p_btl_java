package com.iot.backend_iot.controller;

import com.iot.backend_iot.dto.ThresholdConfigRequest;
import com.iot.backend_iot.model.Device;
import com.iot.backend_iot.model.DeviceStatus;
import com.iot.backend_iot.repository.DeviceRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/devices")
@CrossOrigin(origins = "*")
public class DeviceController {

    @Autowired
    private DeviceRepository deviceRepository;

    @GetMapping
    public List<Device> listDevices(
            @RequestParam(required = false) DeviceStatus status
    ) {
        if (status != null) {
            return deviceRepository.findByStatus(status);
        }
        return deviceRepository.findAll();
    }

    @GetMapping("/{deviceId}")
    public Device getDevice(@PathVariable String deviceId) {
        return deviceRepository.findById(deviceId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Device not found: " + deviceId));
    }

    @PostMapping
    public Device createDevice(@RequestBody Device device) {
        if (device.getDeviceId() == null || device.getDeviceId().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "deviceId is required");
        }
        if (device.getStatus() == null) {
            device.setStatus(DeviceStatus.ONLINE);
        }
        if (device.getLastSeen() == null) {
            device.setLastSeen(LocalDateTime.now());
        }
        return deviceRepository.save(device);
    }

    @PutMapping("/{deviceId}")
    public Device updateDevice(
            @PathVariable String deviceId,
            @RequestBody Device payload
    ) {
        Device existing = deviceRepository.findById(deviceId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Device not found: " + deviceId));

        if (payload.getStatus() != null) existing.setStatus(payload.getStatus());
        if (payload.getLastSeen() != null) existing.setLastSeen(payload.getLastSeen());
        existing.setMaxVoltage(payload.getMaxVoltage());
        existing.setMaxCurrent(payload.getMaxCurrent());
        existing.setMaxPower(payload.getMaxPower());
        existing.setMaxTotalKwh(payload.getMaxTotalKwh());

        return deviceRepository.save(existing);
    }

    @PutMapping("/{deviceId}/thresholds")
    public Device updateThresholds(
            @PathVariable String deviceId,
            @RequestBody ThresholdConfigRequest request
    ) {
        Device device = deviceRepository.findById(deviceId)
                .orElseGet(() -> new Device(deviceId));
        device.setMaxVoltage(request.getMaxVoltage());
        device.setMaxCurrent(request.getMaxCurrent());
        device.setMaxPower(request.getMaxPower());
        device.setMaxTotalKwh(request.getMaxTotalKwh());
        return deviceRepository.save(device);
    }

    @DeleteMapping("/{deviceId}")
    public Map<String, Object> deleteDevice(@PathVariable String deviceId) {
        deviceRepository.deleteById(deviceId);
        return Map.of(
                "deleted", true,
                "deviceId", deviceId
        );
    }
}

