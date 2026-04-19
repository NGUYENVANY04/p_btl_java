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

    private double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    // =============================
    // FETCH DATA ĐA THIẾT BỊ
    // =============================
    private List<Map<String, Object>> fetchAllSensorData(boolean full, List<Integer> deviceIds) {
        List<Map<String, Object>> allData = new ArrayList<>();

        if (deviceIds == null || deviceIds.isEmpty()) {
            System.out.println("❌ deviceIds rỗng");
            return allData;
        }

        int limit = 1000;
        int offset = 0;

        // Chuyển List [1, 2, 3] thành chuỗi "1,2,3"
        String idsString = deviceIds.stream().map(String::valueOf).collect(Collectors.joining(","));

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("apikey", API_KEY);
            headers.set("Authorization", "Bearer " + API_KEY);
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<String> entity = new HttpEntity<>(headers);

            while (true) {
                String select = full ? "*" : "created_at,delta_energy,device_id";

                // Sử dụng toán tử in.(id1,id2,id3) để lấy cùng lúc nhiều thiết bị
                String url = URL + "/sensor_data_processed?select=" + select
                        + "&device_id=in.(" + idsString + ")"
                        + "&limit=" + limit
                        + "&offset=" + offset;

                ResponseEntity<List<Map<String, Object>>> response = restTemplate.exchange(
                        url, HttpMethod.GET, entity,
                        new ParameterizedTypeReference<List<Map<String, Object>>>() {
                        });

                List<Map<String, Object>> batch = response.getBody();

                if (batch == null || batch.isEmpty()) {
                    break;
                }

                allData.addAll(batch);
                offset += limit;
            }
        } catch (Exception e) {
            System.out.println("❌ FETCH ERROR: " + e.getMessage());
        }

        return allData;
    }

    // =============================
    // OVERVIEW (TỔNG QUAN NĂM)
    // =============================
    public List<Map<String, Object>> getEnergyOverview(List<Integer> deviceIds) {
        List<Map<String, Object>> data = fetchAllSensorData(false, deviceIds);
        Map<Integer, Double> map = new HashMap<>();

        for (Map<String, Object> row : data) {
            try {
                String created = (String) row.get("created_at");
                if (created == null)
                    continue;

                int year = Integer.parseInt(created.substring(0, 4));
                double energy = row.get("delta_energy") != null ? ((Number) row.get("delta_energy")).doubleValue()
                        : 0.0;

                map.put(year, map.getOrDefault(year, 0.0) + energy);
            } catch (Exception ignored) {
            }
        }

        List<Map<String, Object>> result = new ArrayList<>();
        map.entrySet().stream()
                .sorted(Map.Entry.<Integer, Double>comparingByKey().reversed())
                .forEach(e -> result.add(Map.of("nam", e.getKey(), "tong_nang_luong", round(e.getValue()))));

        return result;
    }

    // =============================
    // YEAR → MONTH
    // =============================
    public List<Map<String, Object>> getYearlyEnergy(Integer year, List<Integer> deviceIds) {
        List<Map<String, Object>> data = fetchAllSensorData(false, deviceIds);
        Map<String, Double> map = new HashMap<>();

        for (Map<String, Object> row : data) {
            try {
                String created = (String) row.get("created_at");
                if (created == null)
                    continue;

                int y = Integer.parseInt(created.substring(0, 4));

                if (year == null || y == year) {
                    String month = created.substring(0, 7);
                    double energy = row.get("delta_energy") != null ? ((Number) row.get("delta_energy")).doubleValue()
                            : 0.0;
                    map.put(month, map.getOrDefault(month, 0.0) + energy);
                }
            } catch (Exception ignored) {
            }
        }

        List<Map<String, Object>> result = new ArrayList<>();
        map.entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByKey().reversed())
                .forEach(e -> result.add(Map.of("thang", e.getKey(), "tong_nang_luong", round(e.getValue()))));

        return result;
    }

    // =============================
    // MONTH → DAY
    // =============================
    public List<Map<String, Object>> getMonthlyEnergy(String month, List<Integer> deviceIds) {
        List<Map<String, Object>> data = fetchAllSensorData(false, deviceIds);
        Map<String, Double> map = new HashMap<>();

        for (Map<String, Object> row : data) {
            try {
                String created = (String) row.get("created_at");
                if (created == null)
                    continue;

                if (month == null || created.startsWith(month)) {
                    String day = created.substring(0, 10);
                    double energy = row.get("delta_energy") != null ? ((Number) row.get("delta_energy")).doubleValue()
                            : 0.0;
                    map.put(day, map.getOrDefault(day, 0.0) + energy);
                }
            } catch (Exception ignored) {
            }
        }

        List<Map<String, Object>> result = new ArrayList<>();
        map.entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByKey().reversed())
                .forEach(e -> result.add(Map.of("ngay", e.getKey(), "tong_nang_luong", round(e.getValue()))));

        return result;
    }

    // =============================
    // DAY → HOUR
    // =============================
    public List<Map<String, Object>> getDataByDay(String day, List<Integer> deviceIds) {
        List<Map<String, Object>> data = fetchAllSensorData(false, deviceIds);
        Map<String, Double> map = new HashMap<>();

        for (Map<String, Object> row : data) {
            try {
                String created = (String) row.get("created_at");
                if (created == null)
                    continue;

                if (day == null || created.startsWith(day)) {
                    String hour = created.substring(11, 13) + ":00";
                    double energy = row.get("delta_energy") != null ? ((Number) row.get("delta_energy")).doubleValue()
                            : 0.0;
                    map.put(hour, map.getOrDefault(hour, 0.0) + energy);
                }
            } catch (Exception ignored) {
            }
        }

        List<Map<String, Object>> result = new ArrayList<>();
        map.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(e -> result.add(Map.of("time", e.getKey(), "tong_nang_luong", round(e.getValue()))));

        return result;
    }

    public List<Map<String, Object>> getAllAlerts() {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("apikey", API_KEY);
            headers.set("Authorization", "Bearer " + API_KEY);
            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<List<Map<String, Object>>> response = restTemplate.exchange(
                    URL + "/alerts?select=*&order=created_at.desc",
                    HttpMethod.GET, entity,
                    new ParameterizedTypeReference<List<Map<String, Object>>>() {
                    });
            return response.getBody() != null ? response.getBody() : new ArrayList<>();
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    // =============================
    // TRẢ VỀ TẤT CẢ DEVICE CỦA USER
    // =============================
    public List<Integer> getDeviceIdsByUser(Integer userId) {
        List<Integer> ids = new ArrayList<>();
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("apikey", API_KEY);
            headers.set("Authorization", "Bearer " + API_KEY);
            HttpEntity<String> entity = new HttpEntity<>(headers);

            String url = URL + "/user_devices?user_id=eq." + userId + "&select=device_id";

            ResponseEntity<List<Map<String, Object>>> response = restTemplate.exchange(
                    url, HttpMethod.GET, entity,
                    new ParameterizedTypeReference<List<Map<String, Object>>>() {
                    });

            List<Map<String, Object>> data = response.getBody();
            if (data != null && !data.isEmpty()) {
                // Duyệt vòng lặp lấy TẤT CẢ các thiết bị
                for (Map<String, Object> map : data) {
                    ids.add(((Number) map.get("device_id")).intValue());
                }
            }
        } catch (Exception e) {
            System.out.println("❌ DEVICE ERROR: " + e.getMessage());
        }
        return ids;
    }
}