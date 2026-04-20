package com.example.demo.service;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Service
public class xuandat {

    private final RestTemplate restTemplate = new RestTemplate();
    private final String URL = "https://znfxhbrkabxenuzrcogd.supabase.co/rest/v1";
    private final String API_KEY = "sb_secret_NXFJy_AuCYhqJmmJadDKNA_I7N91qFu";

    // =============================
    // HELPER: Build HttpHeaders
    // =============================
    private HttpHeaders buildHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("apikey", API_KEY);
        headers.set("Authorization", "Bearer " + API_KEY);
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }

    // =============================
    // FETCH: Lấy toàn bộ sensor_data (tất cả cột)
    // =============================
    private List<Map<String, Object>> fetchAllSensorData() {
        try {
            HttpEntity<String> entity = new HttpEntity<>(buildHeaders());
            String url = URL + "/sensor_data?select=*&order=created_at.desc";

            ResponseEntity<List<Map<String, Object>>> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    new ParameterizedTypeReference<List<Map<String, Object>>>() {
                    });

            return response.getBody() != null ? response.getBody() : new ArrayList<>();

        } catch (Exception e) {
            System.out.println("[xuandat] fetchAllSensorData error: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    // =============================
    // FETCH: Lấy data theo device_id
    // =============================
    private List<Map<String, Object>> fetchByDevice(Integer deviceId) {
        try {
            HttpEntity<String> entity = new HttpEntity<>(buildHeaders());
            String url = URL + "/sensor_data?select=*&device_id=eq." + deviceId + "&order=created_at.desc";

            ResponseEntity<List<Map<String, Object>>> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    new ParameterizedTypeReference<List<Map<String, Object>>>() {
                    });

            return response.getBody() != null ? response.getBody() : new ArrayList<>();

        } catch (Exception e) {
            System.out.println("[xuandat] fetchByDevice error: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    // =============================
    // API 1: Lấy dữ liệu realtime mới nhất
    //  GET /api/xuandat/realtime?deviceId=101
    //  Trả về 1 bản ghi mới nhất của thiết bị
    // =============================
    public Map<String, Object> getLatestData(Integer deviceId) {
        try {
            HttpEntity<String> entity = new HttpEntity<>(buildHeaders());

            String url;
            if (deviceId != null) {
                url = URL + "/sensor_data?select=*&device_id=eq." + deviceId
                        + "&order=created_at.desc&limit=1";
            } else {
                url = URL + "/sensor_data?select=*&order=created_at.desc&limit=1";
            }

            ResponseEntity<List<Map<String, Object>>> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    new ParameterizedTypeReference<List<Map<String, Object>>>() {
                    });

            List<Map<String, Object>> body = response.getBody();
            if (body != null && !body.isEmpty()) {
                Map<String, Object> raw = body.get(0);
                Map<String, Object> result = new HashMap<>();

                result.put("time", raw.getOrDefault("created_at", "N/A").toString());
                result.put("voltage", toDouble(raw.get("voltage")));
                result.put("current", toDouble(raw.get("current")));
                result.put("power", toDouble(raw.get("power")));
                result.put("energy", toDouble(raw.get("energy")));
                result.put("device_id", raw.get("device_id"));
                return result;
            }
        } catch (Exception e) {
            System.out.println("[xuandat] getLatestData error: " + e.getMessage());
        }
        return new HashMap<>();
    }

    // =============================
    // API 2: Lấy lịch sử theo ngày (D)
    //  GET /api/xuandat/history/day?day=2026-04-17&deviceId=101
    //  Trả về danh sách các bản ghi trong ngày, kèm tên thiết bị
    // =============================
    public List<Map<String, Object>> getHistoryByDay(String dayFilter, Integer deviceId) {
        List<Map<String, Object>> data = (deviceId != null)
                ? fetchByDevice(deviceId)
                : fetchAllSensorData();

        List<Map<String, Object>> result = new ArrayList<>();

        // Map device_id → tên thiết bị (theo device_id thật trong Supabase)
        Map<Integer, String> deviceNameMap = new HashMap<>();
        deviceNameMap.put(101, "Nguyễn Văn A (Nhà 101)");
        deviceNameMap.put(90,  "Trần Thị B (Nhà 102)");
        deviceNameMap.put(87,  "Lê Văn C (Nhà 103)");

        // Fallback cycle cho device_id khác
        String[] fallbackNames = {
            "Nguyễn Văn A (Nhà 101)",
            "Trần Thị B (Nhà 102)",
            "Lê Văn C (Nhà 103)"
        };

        for (Map<String, Object> row : data) {
            try {
                String created = (String) row.get("created_at");
                if (created == null || created.length() < 10)
                    continue;

                String day = created.substring(0, 10);
                if (dayFilter != null && !day.equals(dayFilter))
                    continue;

                String time = created.length() >= 19 ? created.substring(11, 19) : "00:00:00";
                Integer devId = row.get("device_id") != null
                        ? ((Number) row.get("device_id")).intValue()
                        : 0;

                Map<String, Object> item = new HashMap<>();
                item.put("time", time + " " + formatDate(day));
                String deviceName = deviceNameMap.containsKey(devId)
                        ? deviceNameMap.get(devId)
                        : fallbackNames[result.size() % fallbackNames.length];
                item.put("device", deviceName);
                item.put("voltage", toDouble(row.get("voltage")));
                item.put("current", toDouble(row.get("current")));
                item.put("power", toDouble(row.get("power")));
                item.put("energy", toDouble(row.get("energy")));
                item.put("device_id", devId);

                result.add(item);

            } catch (Exception e) {
                System.out.println("[xuandat] getHistoryByDay row error: " + e.getMessage());
            }
        }

        return result;
    }

    // =============================
    // API 3: Tổng hợp tiêu thụ theo tháng (M)
    //  GET /api/xuandat/history/month?month=2026-04&deviceId=101
    //  Trả về kWh theo từng ngày trong tháng (cho biểu đồ)
    // =============================
    public List<Map<String, Object>> getHistoryByMonth(String monthFilter, Integer deviceId) {
        List<Map<String, Object>> data = (deviceId != null)
                ? fetchByDevice(deviceId)
                : fetchAllSensorData();

        Map<String, Double> energyPerDay = new LinkedHashMap<>();

        for (Map<String, Object> row : data) {
            try {
                String created = (String) row.get("created_at");
                if (created == null || created.length() < 10)
                    continue;

                String month = created.substring(0, 7);
                if (monthFilter != null && !month.equals(monthFilter))
                    continue;

                String day = created.substring(0, 10);
                double energy = toDouble(row.get("energy"));
                energyPerDay.merge(day, energy, Double::sum);

            } catch (Exception e) {
                System.out.println("[xuandat] getHistoryByMonth row error: " + e.getMessage());
            }
        }

        List<Map<String, Object>> result = new ArrayList<>();
        energyPerDay.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(e -> {
                    Map<String, Object> item = new HashMap<>();
                    item.put("date", e.getKey());
                    item.put("energy_kwh", Math.round(e.getValue() * 100.0) / 100.0);
                    result.add(item);
                });

        return result;
    }

    // =============================
    // API 4: Tổng hợp tiêu thụ theo năm (Y)
    //  GET /api/xuandat/history/year?year=2026&deviceId=101
    //  Trả về kWh theo từng tháng trong năm (cho biểu đồ)
    // =============================
    public List<Map<String, Object>> getHistoryByYear(Integer yearFilter, Integer deviceId) {
        List<Map<String, Object>> data = (deviceId != null)
                ? fetchByDevice(deviceId)
                : fetchAllSensorData();

        Map<String, Double> energyPerMonth = new LinkedHashMap<>();

        for (Map<String, Object> row : data) {
            try {
                String created = (String) row.get("created_at");
                if (created == null || created.length() < 7)
                    continue;

                int year = Integer.parseInt(created.substring(0, 4));
                if (yearFilter != null && year != yearFilter)
                    continue;

                String month = created.substring(0, 7);
                double energy = toDouble(row.get("energy"));
                energyPerMonth.merge(month, energy, Double::sum);

            } catch (Exception e) {
                System.out.println("[xuandat] getHistoryByYear row error: " + e.getMessage());
            }
        }

        List<Map<String, Object>> result = new ArrayList<>();
        energyPerMonth.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(e -> {
                    Map<String, Object> item = new HashMap<>();
                    item.put("month", e.getKey());
                    item.put("energy_kwh", Math.round(e.getValue() * 100.0) / 100.0);
                    result.add(item);
                });

        return result;
    }

    // =============================
    // HELPER: Chuyển Object → double an toàn
    // =============================
    private double toDouble(Object val) {
        if (val == null)
            return 0.0;
        try {
            return ((Number) val).doubleValue();
        } catch (Exception e) {
            return 0.0;
        }
    }

    // =============================
    // HELPER: "2026-04-17" → "17/04/2026"
    // =============================
    private String formatDate(String isoDate) {
        if (isoDate == null || isoDate.length() < 10)
            return isoDate;
        String[] parts = isoDate.split("-");
        if (parts.length != 3)
            return isoDate;
        return parts[2] + "/" + parts[1] + "/" + parts[0];
    }
}