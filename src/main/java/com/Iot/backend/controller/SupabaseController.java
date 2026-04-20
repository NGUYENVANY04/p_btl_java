package com.Iot.backend.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.HttpClientErrorException;

import com.Iot.backend.service.quochoc;
import com.Iot.backend.dto.DeviceDTO;
import com.Iot.backend.dto.DeviceRequestDTO;
import com.Iot.backend.service.DeviceService;
import com.Iot.backend.service.UserService;
import com.Iot.backend.service.ducthinh;
import com.Iot.backend.service.daocuong;
import java.util.Map;

@CrossOrigin("*") // Thêm để Frontend (HTML/JS) có thể gọi API mà không bị lỗi CORS
@RestController
@RequestMapping("/api")

public class SupabaseController {

    @Autowired
    private quochoc quochocService;
    @Autowired
    private DeviceService deviceService;
    @Autowired
    private UserService userService;
    @Autowired
    private ducthinh ducthinhService;
    @Autowired
    private daocuong daocuongService;

    // Lấy dữ liệu tổng quan theo năm
    @GetMapping("/quochoc/energy/overview")
    public ResponseEntity<?> getEnergyOverview() {
        return ResponseEntity.ok(quochocService.getEnergyOverview());
    }

    // ================= QUOCHOC API =================
    @GetMapping("/quochoc/energy/yearly")
    public ResponseEntity<?> getYear(@RequestParam(required = false) Integer year) {
        return ResponseEntity.ok(quochocService.getYearlyEnergy(year));
    }

    // Lấy dữ liệu tháng -> trả về các ngày
    @GetMapping("/quochoc/energy/monthly")
    public ResponseEntity<?> getMonth(@RequestParam(required = false) String month) {
        return ResponseEntity.ok(quochocService.getMonthlyEnergy(month));
    }

    // Lấy dữ liệu ngày -> trả về các giờ
    @GetMapping("/quochoc/data/day")
    public ResponseEntity<?> getDay(@RequestParam String day) {
        return ResponseEntity.ok(quochocService.getDataByDay(day));
    }

    // =========================
    // DEVICE CRUD
    // =========================

    // CREATE
    @PostMapping("/devices")
    public DeviceDTO create(@RequestBody DeviceRequestDTO request) {
        return deviceService.createDevice(request);
    }

    @PostMapping("/users")
    public ResponseEntity<?> register(@RequestBody Map<String, Object> info) {
        try {
            Map<String, Object> result = userService.registerAccount(info);
            return ResponseEntity.ok(result);

        } catch (HttpClientErrorException e) {

            String errorBody = e.getResponseBodyAsString();

            if (errorBody.contains("users_email_key")) {
                return ResponseEntity.status(409).body("Email đã tồn tại");
            }

            if (errorBody.contains("users_username_key")) {
                return ResponseEntity.status(409).body("Username đã tồn tại");
            }

            return ResponseEntity.status(500).body("Lỗi server");
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, Object> info) {
        try {
            Map<String, Object> user = userService.login(info);
            return ResponseEntity.ok(user);

        } catch (RuntimeException e) {
            return ResponseEntity.status(401).body(e.getMessage());

        } catch (Exception e) {
            return ResponseEntity.status(500).body("Lỗi server");
        }
    }

    // GET ALL
    @GetMapping("/devices")
    public ResponseEntity<?> getAllDevices() {
        return ResponseEntity.ok(deviceService.getAllDevices());
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

    // UPDATE
    @PutMapping("/devices/{id}")
    public ResponseEntity<?> updateDevice(@PathVariable Long id,
            @RequestBody Map<String, Object> device) {
        return ResponseEntity.ok(deviceService.updateDevice(id, device));
    }

    // DELETE
    @DeleteMapping("/devices/{id}")
    public ResponseEntity<?> deleteDevice(@PathVariable Long id) {
        return ResponseEntity.ok(deviceService.deleteDevice(id));
    }

    // ================= DUCTHINH API =================
    // 🔌 Điều khiển ON/OFF thiết bị
    @GetMapping("/device/control")
    public ResponseEntity<?> controlDevice(
            @RequestParam Integer deviceId,
            @RequestParam String status) {

        String result = ducthinhService.controlDevice(deviceId, status);
        return ResponseEntity.ok().body(Map.of(
                "deviceId", deviceId,
                "status", status.toUpperCase(),
                "message", result));
    }

    // ================= DAOCUONG API =================
    @GetMapping("/daocuong/devices/offline/check")
    public ResponseEntity<?> checkOffline(@RequestParam(defaultValue = "4") Integer seconds, @RequestParam Integer userId) {
        return ResponseEntity.ok(daocuongService.checkOfflineDevicesForUser(seconds, userId));
    }

    @GetMapping("/daocuong/threshold/check")
    public ResponseEntity<?> checkThreshold(@RequestParam Integer userId) {
        return ResponseEntity.ok(daocuongService.checkThresholdAlertsForUser(userId));
    }

    @GetMapping("/daocuong/alerts")
    public ResponseEntity<?> getAlerts(@RequestParam(defaultValue = "50") Integer limit, @RequestParam Integer userId) {
        return ResponseEntity.ok(daocuongService.getLatestAlertsForUser(limit, userId));
    }

    @GetMapping("/quochoc/alerts")
    public ResponseEntity<?> getAllAlert() {
        return ResponseEntity.ok(quochocService.getAllAlerts());
    }

    // Lấy cảnh báo theo thiết bị
    @GetMapping("/quochoc/alerts/device")
    public ResponseEntity<?> getAlertByDevice(@RequestParam(required = false) Integer deviceId) {
        return ResponseEntity.ok(quochocService.getAlertsByDevice(deviceId));
    }

    // Lấy các cảnh báo chưa đọc
    @GetMapping("/quochoc/alerts/unread")
    public ResponseEntity<?> getUnreadAlert() {
        return ResponseEntity.ok(quochocService.getUnreadAlerts());
    }

    // Lấy cảnh báo theo ngày
    @GetMapping("/quochoc/alerts/day")
    public ResponseEntity<?> getAlertByDay(@RequestParam String day) {
        return ResponseEntity.ok(quochocService.getAlertsByDay(day));
    }

} // Các API khác của quochoc... (giữ nguyên như file cũ của bạn)
