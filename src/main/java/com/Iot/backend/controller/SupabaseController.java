package com.Iot.backend.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.HttpClientErrorException;

import com.Iot.backend.dto.DeviceDTO;
import com.Iot.backend.dto.DeviceRequestDTO;
import com.Iot.backend.service.*;
import java.util.Map;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*") // Mở để Frontend gọi được API
public class SupabaseController {

    @Autowired
    private DeviceService deviceService;
    @Autowired
    private UserService userService;
    @Autowired
    private ducthinh ducthinhService;

    @PostMapping("/devices")
    public DeviceDTO create(@RequestBody DeviceRequestDTO request) {
        return deviceService.createDevice(request);
    }

    @PostMapping("/users")
    public ResponseEntity<?> register(@RequestBody Map<String, Object> info) {
        try {
            return ResponseEntity.ok(userService.registerAccount(info));
        } catch (Exception e) {
            // Kiểm tra lỗi trùng lặp một cách an toàn hơn
            String msg = e.getMessage();
            if (msg.contains("users_email_key"))
                return ResponseEntity.status(409).body("Email đã tồn tại");
            if (msg.contains("users_username_key"))
                return ResponseEntity.status(409).body("Username đã tồn tại");
            return ResponseEntity.status(500).body("Lỗi hệ thống: " + msg);
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, Object> info) {
        try {
            return ResponseEntity.ok(userService.login(info));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(e.getMessage());
        }
    }

    // ================= DEVICE MANAGEMENT =================

    @GetMapping("/devices")
    public ResponseEntity<?> getAllDevices() {
        return ResponseEntity.ok(deviceService.getAllDevices());
    }

    @PutMapping("/devices/{id}")
    public ResponseEntity<?> updateDevice(@PathVariable Long id, @RequestBody Map<String, Object> device) {
        return ResponseEntity.ok(deviceService.updateDevice(id, device));
    }

    @DeleteMapping("/devices/{id}")
    public ResponseEntity<?> deleteDevice(@PathVariable Long id) {
        return ResponseEntity.ok(deviceService.deleteDevice(id));
    }

    // ================= DUCTHINH API (Điều khiển) =================

    // Đổi sang POST để đúng chuẩn thay đổi trạng thái
    @PostMapping("/device/control")
    public ResponseEntity<?> controlDevice(
            @RequestParam Integer deviceId,
            @RequestParam String status) {

        String result = ducthinhService.controlDevice(deviceId, status);
        return ResponseEntity.ok(Map.of(
                "deviceId", deviceId,
                "status", status.toUpperCase(),
                "message", result,
                "timestamp", System.currentTimeMillis()));
    }

    @GetMapping("/device_limits")
    public ResponseEntity<?> getAllLimitDevices() {
        return ResponseEntity.ok(deviceService.getAllLimitDevices());
    }

    @PutMapping("/device_limits/{id}")

    public ResponseEntity<?> createLimitDevice(
            @PathVariable Long id,
            @RequestBody Map<String, Object> device) {
        return ResponseEntity.ok(deviceService.createLimitDevice(id, device));
    }

}
