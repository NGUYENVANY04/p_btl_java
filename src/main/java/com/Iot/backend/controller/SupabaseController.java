package com.Iot.backend.controller;

import com.Iot.backend.dto.AlertResponse;
import com.Iot.backend.dto.DeviceDTO;
import com.Iot.backend.dto.DeviceRequestDTO;
import com.Iot.backend.dto.DeviceResponse;
import com.Iot.backend.dto.MonitorHistoryResponse;
import com.Iot.backend.service.DeviceService;
import com.Iot.backend.service.UserService;
import com.Iot.backend.service.ducthinh;
import com.Iot.backend.service.quochoc;
import com.Iot.backend.service.xuandat;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class SupabaseController {

    private final DeviceService deviceService;
    private final UserService userService;
    private final ducthinh ducthinhService;
    private final quochoc quochocService;
    private final xuandat xuanDatService;

    public SupabaseController(
            DeviceService deviceService,
            UserService userService,
            ducthinh ducthinhService,
            quochoc quochocService,
            xuandat xuanDatService) {
        this.deviceService = deviceService;
        this.userService = userService;
        this.ducthinhService = ducthinhService;
        this.quochocService = quochocService;
        this.xuanDatService = xuanDatService;
    }

    @PostMapping("/devices")
    public DeviceDTO create(@RequestBody DeviceRequestDTO request) {
        return deviceService.createDevice(request);
    }

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

    @PostMapping("/users")
    public ResponseEntity<?> register(@RequestBody Map<String, Object> info) {
        try {
            return ResponseEntity.ok(userService.registerAccount(info));
        } catch (Exception e) {
            String msg = e.getMessage();
            if (msg.contains("users_email_key")) {
                return ResponseEntity.status(409).body("Email da ton tai");
            }
            if (msg.contains("users_username_key")) {
                return ResponseEntity.status(409).body("Username da ton tai");
            }
            return ResponseEntity.status(500).body("Loi he thong: " + msg);
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

    @GetMapping("/quochoc/energy/yearly")
    public ResponseEntity<?> getYear(@RequestParam(required = false) Integer year) {
        return ResponseEntity.ok(quochocService.getYearlyEnergy(year, null));
    }

    @GetMapping("/quochoc/energy/monthly")
    public ResponseEntity<?> getMonth(@RequestParam(required = false) String month) {
        return ResponseEntity.ok(quochocService.getMonthlyEnergy(month, null));
    }

    @GetMapping("/quochoc/data/day")
    public ResponseEntity<?> getDay(@RequestParam String day) {
        return ResponseEntity.ok(quochocService.getDataByDay(day, null));
    }

    @GetMapping("/xuandat/users")
    public ResponseEntity<?> getXuandatUsers(
            @RequestParam(required = false) Integer requesterUserId,
            @RequestParam(required = false) String requesterRole) {
        return ResponseEntity.ok(xuanDatService.getUsers(requesterUserId, requesterRole));
    }

    @GetMapping("/xuandat/devices")
    public ResponseEntity<List<DeviceResponse>> getXuandatDevices(
            @RequestParam(required = false) Integer requesterUserId,
            @RequestParam(required = false) String requesterRole,
            @RequestParam(required = false) Integer targetUserId) {
        return ResponseEntity.ok(xuanDatService.getDevices(requesterUserId, requesterRole, targetUserId));
    }

    @GetMapping("/xuandat/history")
    public ResponseEntity<MonitorHistoryResponse> getXuandatHistory(
            @RequestParam(required = false) Integer requesterUserId,
            @RequestParam(required = false) String requesterRole,
            @RequestParam(required = false) Integer targetUserId,
            @RequestParam(required = false) Integer deviceId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @RequestParam(required = false) String bucket) {
        return ResponseEntity.ok(xuanDatService.getHistory(requesterUserId, requesterRole, targetUserId, deviceId, from, to, bucket));
    }

    @GetMapping("/xuandat/alerts")
    public ResponseEntity<List<AlertResponse>> getXuandatAlerts(
            @RequestParam(required = false) Integer requesterUserId,
            @RequestParam(required = false) String requesterRole,
            @RequestParam(required = false) Integer targetUserId,
            @RequestParam(required = false) Integer deviceId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {
        return ResponseEntity.ok(xuanDatService.getAlerts(requesterUserId, requesterRole, targetUserId, deviceId, from, to));
    }

    @GetMapping("/xuandat/export/excel")
    public ResponseEntity<byte[]> exportXuandatExcel(
            @RequestParam(required = false) Integer requesterUserId,
            @RequestParam(required = false) String requesterRole,
            @RequestParam(required = false) Integer targetUserId,
            @RequestParam(required = false) Integer deviceId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @RequestParam(required = false) String bucket) {
        return xuanDatService.exportExcel(requesterUserId, requesterRole, targetUserId, deviceId, from, to, bucket);
    }
}
