package com.Iot.backend.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

// import com.Iot.backend.service.vany;
import com.Iot.backend.service.quochoc;
import com.Iot.backend.service.ducthinh;
// import com.Iot.backend.service.xuandat;
import com.Iot.backend.service.daocuong;

// import java.util.List;
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

    @Autowired
    private ducthinh ducThinhService;

    // @Autowired
    // private xuandat xuanDatService;

    @Autowired
    private daocuong daoCuongService;
    @GetMapping("/quochoc/energy/yearly")
    public ResponseEntity<?> getYear(@RequestParam(required = false) Integer year) {
        return ResponseEntity.ok(quochocService.getYearlyEnergy(year));
    }

    // MONTH → ngày
    @GetMapping("/quochoc/energy/monthly")
    public ResponseEntity<?> getMonth(@RequestParam(required = false) String month) {
        return ResponseEntity.ok(quochocService.getMonthlyEnergy(month));
    }

    // DAY → raw theo giờ
    @GetMapping("/quochoc/data/day")
    public ResponseEntity<?> getDay(@RequestParam String day) {
        return ResponseEntity.ok(quochocService.getDataByDay(day));
    }

    // =========================
    // DAO CUONG - TASK 3 & 4
    // =========================
    @GetMapping("/daocuong/devices/offline/check")
    public ResponseEntity<?> checkOffline(@RequestParam(defaultValue = "5") Integer minutes) {
        return ResponseEntity.ok(daoCuongService.checkOfflineDevices(minutes != null ? minutes : 5));
    }

    @GetMapping("/daocuong/threshold/check")
    public ResponseEntity<?> checkThreshold() {
        return ResponseEntity.ok(daoCuongService.checkThresholdAlerts());
    }

    @GetMapping("/daocuong/alerts")
    public ResponseEntity<?> getAlerts(@RequestParam(defaultValue = "50") Integer limit) {
        return ResponseEntity.ok(daoCuongService.getLatestAlerts(limit != null ? limit : 50));
    }

    // ================= DUCTHINH API =================
    // 🔌 Điều khiển ON/OFF thiết bị
    @GetMapping("/device/control")
    public ResponseEntity<?> controlDevice(
            @RequestParam Integer deviceId,
            @RequestParam String status) {

        String result = ducThinhService.controlDevice(deviceId, status);
        return ResponseEntity.ok().body(Map.of(
                "deviceId", deviceId,
                "status", status.toUpperCase(),
                "message", result
        ));
    }

}
