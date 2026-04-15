package com.Iot.backend.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.Iot.backend.service.quochoc;
import com.Iot.backend.service.DeviceService;

import java.util.Map;

@RestController
@RequestMapping("/api")
@CrossOrigin("*") // fix CORS
public class SupabaseController {

    // =========================
    // ENERGY SERVICE
    // =========================
    @Autowired
    private quochoc quochocService;

    // =========================
    // DEVICE SERVICE
    // =========================
    @Autowired
    private DeviceService deviceService;

    // =========================
    // ENERGY API
    // =========================

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

    // =========================
    // DEVICE CRUD
    // =========================

    // CREATE
    @PostMapping("/devices")
    public ResponseEntity<?> createDevice(@RequestBody Map<String, Object> device) {
        return ResponseEntity.ok(deviceService.createDevice(device));
    }

    // GET ALL
    @GetMapping("/devices")
    public ResponseEntity<?> getAllDevices() {
        return ResponseEntity.ok(deviceService.getAllDevices());
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
}