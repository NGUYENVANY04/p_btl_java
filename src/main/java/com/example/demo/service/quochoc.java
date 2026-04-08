package com.example.demo.service;

import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map; // import java.util.Map
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

@Service
public class quochoc {

    private final RestTemplate restTemplate = new RestTemplate();
    private final String URL = "https://znfxhbrkabxenuzrcogd.supabase.co/rest/v1";
    private final String API_KEY = "sb_secret_NXFJy_AuCYhqJmmJadDKNA_I7N91qFu";

    /**
     * Lấy dữ liệu dòng thứ 4 từ bảng sensor_data
     */
    public Map<String, Object> getFourthSensorData() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("apikey", API_KEY);
        headers.set("Authorization", "Bearer " + API_KEY);
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<String> entity = new HttpEntity<>(headers);

        // nối bảng sensor_data vào URL
        String requestUrl = URL + "/sensor_data?select=*&limit=1&offset=3";

        // Sử dụng Map<String,Object>[] thay vì raw Map[]
        ResponseEntity<Map<String, Object>[]> response = restTemplate.exchange(
                requestUrl,
                HttpMethod.GET,
                entity,
                (Class<Map<String, Object>[]>) (Class<?>) Map[].class);

        Map<String, Object>[] data = response.getBody();
        if (data != null && data.length > 0) {
            return data[0]; // trả về dòng thứ 4
        } else {
            return null;
        }
    }

    public List<Map<String, Object>> getYearlyEnergy() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("apikey", API_KEY);
        headers.set("Authorization", "Bearer " + API_KEY);
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<String> entity = new HttpEntity<>(headers);
        String sensorUrl = URL + "/sensor_data?select=created_at,energy";

        ResponseEntity<Map<String, Object>[]> response = restTemplate.exchange(
                sensorUrl,
                HttpMethod.GET,
                entity,
                (Class<Map<String, Object>[]>) (Class<?>) Map[].class);

        Map<String, Object>[] data = response.getBody();
        Map<Integer, Double> yearlySum = new HashMap<>();

        if (data != null) {
            for (Map<String, Object> row : data) {
                String created = (String) row.get("created_at");
                Double energy = row.get("energy") != null ? ((Number) row.get("energy")).doubleValue() : 0.0;
                int year = OffsetDateTime.parse(created).getYear();
                yearlySum.put(year, yearlySum.getOrDefault(year, 0.0) + energy);
            }
        }

        List<Map<String, Object>> result = new ArrayList<>();
        yearlySum.entrySet().stream()
                .sorted((a, b) -> b.getKey().compareTo(a.getKey()))
                .forEach(e -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("nam", e.getKey());
                    map.put("tong_nang_luong", e.getValue());
                    result.add(map);
                });
        return result;
    }

    // -----------------------------
    // 3️⃣ Tổng năng lượng theo tháng
    // -----------------------------
    public List<Map<String, Object>> getMonthlyEnergy(String monthFilter) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("apikey", API_KEY);
        headers.set("Authorization", "Bearer " + API_KEY);
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<String> entity = new HttpEntity<>(headers);
        String sensorUrl = URL + "/sensor_data?select=created_at,energy";

        ResponseEntity<Map<String, Object>[]> response = restTemplate.exchange(
                sensorUrl,
                HttpMethod.GET,
                entity,
                (Class<Map<String, Object>[]>) (Class<?>) Map[].class);

        Map<String, Object>[] data = response.getBody();
        Map<String, Double> monthlySum = new HashMap<>();

        if (data != null) {
            for (Map<String, Object> row : data) {
                String created = (String) row.get("created_at");
                Double energy = row.get("energy") != null ? ((Number) row.get("energy")).doubleValue() : 0.0;
                String month = created.substring(0, 7); // yyyy-MM
                // Nếu có filter thì check
                if (monthFilter == null || month.equals(monthFilter)) {
                    monthlySum.put(month, monthlySum.getOrDefault(month, 0.0) + energy);
                }
            }
        }

        List<Map<String, Object>> result = new ArrayList<>();
        monthlySum.entrySet().stream()
                .sorted((a, b) -> b.getKey().compareTo(a.getKey())) // sort DESC
                .forEach(e -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("thang", e.getKey());
                    map.put("tong_nang_luong", e.getValue());
                    result.add(map);
                });
        return result;
    }
}