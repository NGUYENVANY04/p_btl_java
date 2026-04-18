package com.Iot.backend.service;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class quochoc {

    private final RestTemplate restTemplate = new RestTemplate();
    private final String URL = "https://znfxhbrkabxenuzrcogd.supabase.co/rest/v1";
    private final String API_KEY = "sb_secret_NXFJy_AuCYhqJmmJadDKNA_I7N91qFu";

    // =============================
    // LẤY TOÀN BỘ DATA TỪ VIEW VÀ PHÂN TRANG (Vượt giới hạn 1000 dòng của Supabase)
    // =============================
    private List<Map<String, Object>> fetchAllFromView() {
        List<Map<String, Object>> allData = new ArrayList<>();
        int limit = 1000;
        int offset = 0;

        HttpHeaders headers = new HttpHeaders();
        headers.set("apikey", API_KEY);
        headers.setBearerAuth(API_KEY);
        headers.setContentType(MediaType.APPLICATION_JSON);

        try {
            while (true) {
                // Chỉ lấy created_at và delta_energy từ View để tối ưu tốc độ
                String endpoint = URL + "/sensor_data_processed?select=created_at,delta_energy&limit=" + limit
                        + "&offset=" + offset;

                ResponseEntity<List<Map<String, Object>>> response = restTemplate.exchange(
                        endpoint, HttpMethod.GET, new HttpEntity<>(headers),
                        new ParameterizedTypeReference<List<Map<String, Object>>>() {
                        });

                List<Map<String, Object>> batch = response.getBody();
                if (batch == null || batch.isEmpty())
                    break;

                allData.addAll(batch);
                offset += limit;
            }
        } catch (Exception e) {
            System.err.println("Lỗi API Supabase View: " + e.getMessage());
        }
        return allData;
    }

    private double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    // =============================
    // 1. TỔNG QUAN CÁC NĂM
    // =============================
    public List<Map<String, Object>> getEnergyOverview() {
        List<Map<String, Object>> rawData = fetchAllFromView();

        Map<Integer, Double> map = rawData.stream()
                .filter(r -> r.get("created_at") != null)
                .collect(Collectors.groupingBy(
                        r -> Integer.parseInt(((String) r.get("created_at")).substring(0, 4)),
                        Collectors.summingDouble(r -> ((Number) r.getOrDefault("delta_energy", 0.0)).doubleValue())));

        List<Map<String, Object>> result = new ArrayList<>();
        map.forEach((year, energy) -> result.add(Map.of("nam", year, "tong_nang_luong", round(energy))));
        result.sort((a, b) -> ((Integer) b.get("nam")).compareTo((Integer) a.get("nam"))); // Sắp xếp mới nhất lên đầu
        return result;
    }

    // =============================
    // 2. NĂM TRẢ VỀ THÁNG
    // =============================
    public List<Map<String, Object>> getYearlyEnergy(Integer yearFilter) {
        String filter = yearFilter != null ? String.valueOf(yearFilter) : null;
        List<Map<String, Object>> rawData = fetchAllFromView();

        Map<String, Double> map = rawData.stream()
                .filter(r -> r.get("created_at") != null
                        && (filter == null || ((String) r.get("created_at")).startsWith(filter)))
                .collect(Collectors.groupingBy(
                        r -> ((String) r.get("created_at")).substring(0, 7), // Lấy chuỗi "YYYY-MM"
                        Collectors.summingDouble(r -> ((Number) r.getOrDefault("delta_energy", 0.0)).doubleValue())));

        List<Map<String, Object>> result = new ArrayList<>();
        map.forEach((month, energy) -> result.add(Map.of("thang", month, "tong_nang_luong", round(energy))));
        result.sort((a, b) -> ((String) b.get("thang")).compareTo((String) a.get("thang")));
        return result;
    }

    // =============================
    // 3. THÁNG TRẢ VỀ NGÀY
    // =============================
    public List<Map<String, Object>> getMonthlyEnergy(String monthFilter) {
        List<Map<String, Object>> rawData = fetchAllFromView();

        Map<String, Double> map = rawData.stream()
                .filter(r -> r.get("created_at") != null
                        && (monthFilter == null || ((String) r.get("created_at")).startsWith(monthFilter)))
                .collect(Collectors.groupingBy(
                        r -> ((String) r.get("created_at")).substring(0, 10), // Lấy chuỗi "YYYY-MM-DD"
                        Collectors.summingDouble(r -> ((Number) r.getOrDefault("delta_energy", 0.0)).doubleValue())));

        List<Map<String, Object>> result = new ArrayList<>();
        map.forEach((day, energy) -> result.add(Map.of("ngay", day, "tong_nang_luong", round(energy))));
        result.sort((a, b) -> ((String) b.get("ngay")).compareTo((String) a.get("ngay")));
        return result;
    }

    // =============================
    // 4. NGÀY TRẢ VỀ GIỜ
    // =============================
    public List<Map<String, Object>> getDataByDay(String dayFilter) {
        List<Map<String, Object>> rawData = fetchAllFromView();

        Map<String, Double> map = rawData.stream()
                .filter(r -> r.get("created_at") != null
                        && (dayFilter == null || ((String) r.get("created_at")).startsWith(dayFilter)))
                .collect(Collectors.groupingBy(
                        r -> {
                            String time = (String) r.get("created_at");
                            return time.length() >= 13 ? time.substring(11, 13) + ":00" : "00:00";
                        },
                        Collectors.summingDouble(r -> ((Number) r.getOrDefault("delta_energy", 0.0)).doubleValue())));

        List<Map<String, Object>> result = new ArrayList<>();
        map.forEach((hour, energy) -> result.add(Map.of("time", hour, "tong_nang_luong", round(energy))));
        result.sort((a, b) -> ((String) a.get("time")).compareTo((String) b.get("time"))); // Xếp từ 00:00 -> 23:00
        return result;
    }

    // =============================
    // QUẢN LÝ ALERTS
    // =============================
    private List<Map<String, Object>> fetchAlerts() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("apikey", API_KEY);
        headers.setBearerAuth(API_KEY);
        headers.setContentType(MediaType.APPLICATION_JSON);
        try {
            ResponseEntity<List<Map<String, Object>>> response = restTemplate.exchange(
                    URL + "/alerts?select=*", HttpMethod.GET, new HttpEntity<>(headers),
                    new ParameterizedTypeReference<List<Map<String, Object>>>() {
                    });
            return response.getBody() != null ? response.getBody() : new ArrayList<>();
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    public List<Map<String, Object>> getAllAlerts() {
        return fetchAlerts();
    }

    public List<Map<String, Object>> getAlertsByDevice(Integer deviceId) {
        return fetchAlerts().stream()
                .filter(r -> deviceId == null || Objects.equals(r.get("device_id"), deviceId))
                .collect(Collectors.toList());
    }

    public List<Map<String, Object>> getUnreadAlerts() {
        return fetchAlerts().stream()
                .filter(r -> Boolean.FALSE.equals(r.get("is_read")))
                .collect(Collectors.toList());
    }

    public List<Map<String, Object>> getAlertsByDay(String dayFilter) {
        return fetchAlerts().stream()
                .filter(r -> {
                    String time = (String) r.get("created_at");
                    return time != null && time.length() >= 10
                            && (dayFilter == null || time.substring(0, 10).equals(dayFilter));
                })
                .collect(Collectors.toList());
    }
}