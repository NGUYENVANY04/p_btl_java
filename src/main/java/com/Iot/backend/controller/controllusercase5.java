package com.Iot.backend.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.Iot.backend.service.quochoc;

import java.util.List;
import java.util.Map;

@CrossOrigin("*")
@RestController
@RequestMapping("/api")
public class controllusercase5 {

    @Autowired
    private quochoc quochocService;

    // =========================
    // LẤY LIST DEVICE ID TỪ USER ID
    // =========================
    @GetMapping("/device")
    public ResponseEntity<?> getDeviceByUser(@RequestParam Integer userId) {
        List<Integer> deviceIds = quochocService.getDeviceIdsByUser(userId);

        if (deviceIds == null || deviceIds.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "message", "Không tìm thấy thiết bị nào cho user này",
                    "userId", userId));
        }

        // Trả về một mảng: { "deviceIds": [1, 2, 3] }
        return ResponseEntity.ok(Map.of("deviceIds", deviceIds));
    }

    // =========================
    // THỐNG KÊ ĐIỆN NĂNG
    // Nhận chuỗi ID cách nhau bằng dấu phẩy (vd: ?deviceIds=1,2,3)
    // =========================
    @GetMapping("/energy/overview")
    public ResponseEntity<?> overview(@RequestParam List<Integer> deviceIds) {
        return ResponseEntity.ok(quochocService.getEnergyOverview(deviceIds));
    }

    @GetMapping("/energy/yearly")
    public ResponseEntity<?> yearly(
            @RequestParam(required = false) Integer year,
            @RequestParam List<Integer> deviceIds) {
        return ResponseEntity.ok(quochocService.getYearlyEnergy(year, deviceIds));
    }

    @GetMapping("/energy/monthly")
    public ResponseEntity<?> monthly(
            @RequestParam(required = false) String month,
            @RequestParam List<Integer> deviceIds) {
        return ResponseEntity.ok(quochocService.getMonthlyEnergy(month, deviceIds));
    }

    @GetMapping("/data/day")
    public ResponseEntity<?> byDay(
            @RequestParam(required = false) String day,
            @RequestParam List<Integer> deviceIds) {
        return ResponseEntity.ok(quochocService.getDataByDay(day, deviceIds));
    }

    @GetMapping("/alerts")
    public ResponseEntity<?> getAllAlerts() {
        return ResponseEntity.ok(quochocService.getAllAlerts());
    }
}