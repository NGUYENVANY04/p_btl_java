package com.example.demo.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

// import com.example.demo.service.vany;
import com.example.demo.service.quochoc;
// import com.example.demo.service.ducthinh;
// import com.example.demo.service.xuandat;
import com.example.demo.service.daocuong;

import java.util.List;
import java.util.Map;

/**
 * SupabaseController
 * ------------------
 * Controller chính dùng 5 service:
 * - vany
 * - quochoc
 * - ducthinh
 * - xuandat
 * - daocuong
 *
 * Mỗi service có thể triển khai GET/POST/PUT/DELETE riêng.
 */

@RestController
@RequestMapping("/api")
public class SupabaseController {

    // -----------------------
    // Inject các service
    // -----------------------
    // @Autowired
    // private vany vanyService;

    @Autowired
    private quochoc quochocService;

    // @Autowired
    // private ducthinh ducThinhService;

    // @Autowired
    // private xuandat xuanDatService;

    @Autowired
    private daocuong daoCuongService;

    // -----------------------
    // API demo vany service
    // -----------------------

    @GetMapping("/quochoc/sensordata/fourth")
    public ResponseEntity<Map<String, Object>> getFourthSensorData() {
        Map<String, Object> sensorData = quochocService.getFourthSensorData();
        if (sensorData != null) {
            return ResponseEntity.ok(sensorData);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    // -----------------------------
    // GET tổng năng lượng theo năm
    // -----------------------------
    @GetMapping("/quochoc/energy/yearly")
    public ResponseEntity<List<Map<String, Object>>> getYearlyEnergy() {
        return ResponseEntity.ok(quochocService.getYearlyEnergy());
    }

    // -----------------------------
    // GET tổng năng lượng theo tháng
    // -----------------------------
    @GetMapping("/quochoc/energy/monthly")
    public ResponseEntity<List<Map<String, Object>>> getMonthlyEnergy(@RequestParam(required = false) String month) {
        return ResponseEntity.ok(quochocService.getMonthlyEnergy(month));
    }

    // -----------------------------
    // Task 3: Thiết bị offline
    // -----------------------------
    @GetMapping("/daocuong/device-statuses")
    public ResponseEntity<List<Map<String, Object>>> getDeviceStatuses() {
        return ResponseEntity.ok(daoCuongService.getDeviceStatuses());
    }

    @PostMapping("/daocuong/check-offline")
    public ResponseEntity<Map<String, Object>> checkOffline(@RequestParam(defaultValue = "5") int minutes) {
        return ResponseEntity.ok(daoCuongService.checkOfflineDevices(minutes));
    }

    // -----------------------------
    // Task 4: Vượt ngưỡng
    // -----------------------------
    @PostMapping("/daocuong/check-threshold")
    public ResponseEntity<Map<String, Object>> checkThreshold(@RequestParam int deviceId) {
        return ResponseEntity.ok(daoCuongService.checkThresholdForDevice(deviceId));
    }

    @GetMapping("/daocuong/alerts")
    public ResponseEntity<List<Map<String, Object>>> getAlerts(
            @RequestParam(required = false) Integer deviceId,
            @RequestParam(required = false, defaultValue = "false") boolean unreadOnly,
            @RequestParam(required = false, defaultValue = "20") int limit) {
        return ResponseEntity.ok(daoCuongService.getAlerts(deviceId, unreadOnly, limit));
    }
}
