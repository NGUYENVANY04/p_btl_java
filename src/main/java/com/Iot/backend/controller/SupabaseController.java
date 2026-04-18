package com.Iot.backend.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.Iot.backend.service.quochoc;

@CrossOrigin("*") // Thêm để Frontend (HTML/JS) có thể gọi API mà không bị lỗi CORS
@RestController
@RequestMapping("/api")
public class SupabaseController {

    @Autowired
    private quochoc quochocService;

    // Lấy dữ liệu tổng quan theo năm
    @GetMapping("/quochoc/energy/overview")
    public ResponseEntity<?> getEnergyOverview() {
        return ResponseEntity.ok(quochocService.getEnergyOverview());
    }

    // Lấy dữ liệu năm -> trả về các tháng
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

    // Lấy tất cả cảnh báo
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
}