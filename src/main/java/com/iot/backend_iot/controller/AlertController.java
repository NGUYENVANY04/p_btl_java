package com.iot.backend_iot.controller;

import com.iot.backend_iot.model.Alert;
import com.iot.backend_iot.model.AlertType;
import com.iot.backend_iot.repository.AlertRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/alerts")
@CrossOrigin(origins = "*")
public class AlertController {

    @Autowired
    private AlertRepository alertRepository;

    @GetMapping
    public List<Alert> getAlerts(
            @RequestParam(required = false) String deviceId,
            @RequestParam(required = false) AlertType type,
            @RequestParam(required = false) Boolean resolved
    ) {
        if (deviceId != null && !deviceId.isBlank() && type != null && resolved != null) {
            return alertRepository.findByDeviceIdAndTypeAndResolvedOrderByCreatedAtDesc(deviceId, type, resolved);
        }
        if (deviceId != null && !deviceId.isBlank() && type != null) {
            return alertRepository.findByDeviceIdAndTypeOrderByCreatedAtDesc(deviceId, type);
        }
        if (deviceId != null && !deviceId.isBlank() && resolved != null) {
            return alertRepository.findByDeviceIdAndResolvedOrderByCreatedAtDesc(deviceId, resolved);
        }
        if (type != null && resolved != null) {
            return alertRepository.findByTypeAndResolvedOrderByCreatedAtDesc(type, resolved);
        }
        if (deviceId != null && !deviceId.isBlank()) {
            return alertRepository.findByDeviceIdOrderByCreatedAtDesc(deviceId);
        }
        if (type != null) {
            return alertRepository.findByTypeOrderByCreatedAtDesc(type);
        }
        if (resolved != null) {
            return alertRepository.findByResolvedOrderByCreatedAtDesc(resolved);
        }
        return alertRepository.findByOrderByCreatedAtDesc();
    }

    @PatchMapping("/{id}/resolve")
    public Alert resolveAlert(@PathVariable Long id) {
        Alert alert = alertRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Alert not found: " + id));
        alert.setResolved(true);
        return alertRepository.save(alert);
    }

    @GetMapping("/summary")
    public Map<String, Object> summary() {
        List<Alert> all = alertRepository.findAll();
        long total = all.size();
        long unresolved = all.stream().filter(a -> !a.isResolved()).count();
        long offline = all.stream().filter(a -> a.getType() == AlertType.OFFLINE).count();
        long threshold = all.stream().filter(a -> a.getType() == AlertType.THRESHOLD_EXCEEDED).count();

        return Map.of(
                "total", total,
                "unresolved", unresolved,
                "offline", offline,
                "thresholdExceeded", threshold
        );
    }
}

