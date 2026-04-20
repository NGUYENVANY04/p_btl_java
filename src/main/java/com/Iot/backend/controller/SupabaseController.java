package com.example.demo.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

// import com.example.demo.service.vany;
import com.example.demo.service.quochoc;
// import com.example.demo.service.ducthinh;
import com.example.demo.service.xuandat;
// import com.example.demo.service.daocuong;

// import java.util.List;
// import java.util.Map;

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

@CrossOrigin(origins = "*")
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

    @Autowired
    private xuandat xuanDatService;

    // @Autowired
    // private daocuong daoCuongService;
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

    // =======================================
    // XUANDAT - Usecase 3: Giám sát & Lịch sử
    // =======================================

    // Realtime: lấy bản ghi mới nhất của 1 thiết bị
    // GET /api/xuandat/realtime?deviceId=101
    @GetMapping("/xuandat/realtime")
    public ResponseEntity<?> getLatestData(
            @RequestParam(required = false) Integer deviceId) {
        return ResponseEntity.ok(xuanDatService.getLatestData(deviceId));
    }

    // Lịch sử theo Ngày (D)
    // GET /api/xuandat/history/day?day=2026-04-17&deviceId=101
    @GetMapping("/xuandat/history/day")
    public ResponseEntity<?> getHistoryDay(
            @RequestParam(required = false) String day,
            @RequestParam(required = false) Integer deviceId) {
        return ResponseEntity.ok(xuanDatService.getHistoryByDay(day, deviceId));
    }

    // Lịch sử theo Tháng (M)
    // GET /api/xuandat/history/month?month=2026-04&deviceId=101
    @GetMapping("/xuandat/history/month")
    public ResponseEntity<?> getHistoryMonth(
            @RequestParam(required = false) String month,
            @RequestParam(required = false) Integer deviceId) {
        return ResponseEntity.ok(xuanDatService.getHistoryByMonth(month, deviceId));
    }

    // Lịch sử theo Năm (Y)
    // GET /api/xuandat/history/year?year=2026&deviceId=101
    @GetMapping("/xuandat/history/year")
    public ResponseEntity<?> getHistoryYear(
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer deviceId) {
        return ResponseEntity.ok(xuanDatService.getHistoryByYear(year, deviceId));
    }
}
