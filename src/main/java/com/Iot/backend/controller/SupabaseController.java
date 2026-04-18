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
    @Autowired
    private UserService userService;
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
}