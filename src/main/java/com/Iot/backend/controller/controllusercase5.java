package com.Iot.backend.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.Iot.backend.service.quochoc;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@CrossOrigin("*")
@RestController
@RequestMapping("/api")
public class controllusercase5 {

    @Autowired
    private quochoc quochocService;

    @GetMapping("/device")
    public ResponseEntity<?> getDeviceByUser(@RequestParam Integer userId) {
        List<Integer> deviceIds = quochocService.getDeviceIdsByUser(userId);
        if (deviceIds == null || deviceIds.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "message", "Không tìm thấy thiết bị nào cho user này",
                    "userId", userId));
        }
        return ResponseEntity.ok(Map.of("deviceIds", deviceIds));
    }

    @GetMapping("/energy/overview")
    public ResponseEntity<?> overview(@RequestParam String deviceIds) {
        List<Integer> devIdsList = Arrays.stream(deviceIds.split(",")).map(String::trim).map(Integer::parseInt)
                .collect(Collectors.toList());
        return ResponseEntity.ok(quochocService.getEnergyOverview(devIdsList));
    }

    @GetMapping("/energy/yearly")
    public ResponseEntity<?> yearly(@RequestParam(required = false) Integer year, @RequestParam String deviceIds) {
        List<Integer> devIdsList = Arrays.stream(deviceIds.split(",")).map(String::trim).map(Integer::parseInt)
                .collect(Collectors.toList());
        return ResponseEntity.ok(quochocService.getYearlyEnergy(year, devIdsList));
    }

    @GetMapping("/energy/monthly")
    public ResponseEntity<?> monthly(@RequestParam(required = false) String month, @RequestParam String deviceIds) {
        List<Integer> devIdsList = Arrays.stream(deviceIds.split(",")).map(String::trim).map(Integer::parseInt)
                .collect(Collectors.toList());
        return ResponseEntity.ok(quochocService.getMonthlyEnergy(month, devIdsList));
    }

    @GetMapping("/data/day")
    public ResponseEntity<?> byDay(@RequestParam(required = false) String day, @RequestParam String deviceIds) {
        List<Integer> devIdsList = Arrays.stream(deviceIds.split(",")).map(String::trim).map(Integer::parseInt)
                .collect(Collectors.toList());
        return ResponseEntity.ok(quochocService.getDataByDay(day, devIdsList));
    }

    @GetMapping("/alerts")
    public ResponseEntity<?> getAllAlerts() {
        return ResponseEntity.ok(quochocService.getAllAlerts());
    }

    @GetMapping("/bills")
    public ResponseEntity<?> getBills(@RequestParam Integer userId, @RequestParam String deviceIds) {
        List<Integer> devIdsList = Arrays.stream(deviceIds.split(",")).map(String::trim).map(Integer::parseInt)
                .collect(Collectors.toList());
        return ResponseEntity.ok(quochocService.getMonthlyBills(userId, devIdsList));
    }

    @PostMapping("/bills/pay")
    public ResponseEntity<?> payBill(
            @RequestParam Integer userId,
            @RequestParam String deviceIds,
            @RequestParam String month) {

        try {
            List<Integer> devIdsList = Arrays.stream(deviceIds.split(","))
                    .map(String::trim)
                    .map(Integer::parseInt)
                    .collect(Collectors.toList());

            boolean success = quochocService.payBill(userId, devIdsList, month);

            if (success) {
                return ResponseEntity.ok(Map.of("message", "Thanh toán thành công"));
            } else {
                // Nếu thất bại sẽ trả về mã 500 để phân biệt với lỗi tham số 400
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(Map.of("message", "Supabase từ chối cập nhật lệnh PATCH. (Xem Console Java)"));
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", "Lỗi dữ liệu gửi lên: " + e.getMessage()));
        }
    }
}