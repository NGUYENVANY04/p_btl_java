package com.Iot.backend.controller;

import org.springframework.web.bind.annotation.*;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;
import java.util.List;

@RestController
@RequestMapping("/api/energy")
@CrossOrigin(origins = "*") // Cho phép Web gọi API không bị lỗi CORS
public class MessageController {

    private final String SUPABASE_URL = "https://znfxhbrkabxenuzrcogd.supabase.co/rest/v1/energy_logs";
    private final String API_KEY = "sb_secret_NXFJy_AuCYhqJmmJadDKNA_I7N91qFu";
    private final RestTemplate restTemplate = new RestTemplate();

    // 1. API lấy dữ liệu mới nhất (Ví dụ Web cần load nhanh 10 bản ghi đầu)
    @GetMapping("/latest")
    public ResponseEntity<String> getLatestData() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("apikey", API_KEY);
        headers.set("Authorization", "Bearer " + API_KEY);

        HttpEntity<String> entity = new HttpEntity<>(headers);

        // Gọi Supabase để lấy 10 bản ghi mới nhất, sắp xếp theo ID giảm dần
        String url = SUPABASE_URL + "?select=*&order=id.desc&limit=10";

        try {
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
            return ResponseEntity.ok(response.getBody());
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Lỗi lấy dữ liệu: " + e.getMessage());
        }
    }

    // 2. API để Web điều khiển thiết bị (Gửi lệnh ngược lại MQTT nếu cần)
    @PostMapping("/control")
    public ResponseEntity<String> controlDevice(@RequestBody String command) {
        // Bạn có thể inject class 'ducthinh' vào đây để dùng MQTT gửi lệnh xuống ESP32
        System.out.println("Lệnh điều khiển từ Web: " + command);
        return ResponseEntity.ok("Đã nhận lệnh: " + command);
    }
}