package com.example.demo.controller;

import com.example.demo.dto.AlertResponse;
import com.example.demo.dto.DeviceResponse;
import com.example.demo.dto.MonitorHistoryResponse;
import com.example.demo.service.quochoc;
import com.example.demo.service.xuandat;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api")
public class SupabaseController {

    private final quochoc quochocService;
    private final xuandat xuanDatService;

    public SupabaseController(quochoc quochocService, xuandat xuanDatService) {
        this.quochocService = quochocService;
        this.xuanDatService = xuanDatService;
    }

    @GetMapping("/quochoc/energy/yearly")
    public ResponseEntity<?> getYear(@RequestParam(required = false) Integer year) {
        return ResponseEntity.ok(quochocService.getYearlyEnergy(year));
    }

    @GetMapping("/quochoc/energy/monthly")
    public ResponseEntity<?> getMonth(@RequestParam(required = false) String month) {
        return ResponseEntity.ok(quochocService.getMonthlyEnergy(month));
    }

    @GetMapping("/quochoc/data/day")
    public ResponseEntity<?> getDay(@RequestParam String day) {
        return ResponseEntity.ok(quochocService.getDataByDay(day));
    }

    @GetMapping("/xuandat/devices")
    public ResponseEntity<List<DeviceResponse>> getXuandatDevices() {
        return ResponseEntity.ok(xuanDatService.getDevices());
    }

    @GetMapping("/xuandat/history")
    public ResponseEntity<MonitorHistoryResponse> getXuandatHistory(
            @RequestParam(required = false) Integer deviceId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @RequestParam(required = false) String bucket) {
        return ResponseEntity.ok(xuanDatService.getHistory(deviceId, from, to, bucket));
    }

    @GetMapping("/xuandat/alerts")
    public ResponseEntity<List<AlertResponse>> getXuandatAlerts(
            @RequestParam(required = false) Integer deviceId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {
        return ResponseEntity.ok(xuanDatService.getAlerts(deviceId, from, to));
    }

    @GetMapping("/xuandat/export/excel")
    public ResponseEntity<byte[]> exportXuandatExcel(
            @RequestParam(required = false) Integer deviceId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @RequestParam(required = false) String bucket) {
        return xuanDatService.exportExcel(deviceId, from, to, bucket);
    }
}
