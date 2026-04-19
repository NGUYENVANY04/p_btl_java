package com.Iot.backend.controller;

import org.springframework.web.bind.annotation.*;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;

import com.Iot.backend.service.ducthinh;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*") // THÊM DÒNG NÀY ĐỂ CHẶN LỖI RELOAD DO CORS
public class MessageController {

    @Autowired
    private ducthinh mqttService;

    @PostMapping("/control")
    public ResponseEntity<Map<String, String>> controlDevice(@RequestBody Map<String, Object> info) {
        try {
            Integer deviceId = (Integer) info.get("deviceId");
            String status = (String) info.get("status");

            String result = mqttService.controlDevice(deviceId, status);

            // Kiểm tra xem trong chuỗi trả về có chữ "Thành công" không
            if (result != null && result.contains("Thành công")) {
                return ResponseEntity.ok(Map.of("message", result));
            } else {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(Map.of("message", result));
            }
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("message", "Lỗi Server: " + e.getMessage()));
        }
    }
}