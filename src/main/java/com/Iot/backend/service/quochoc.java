package com.Iot.backend.service;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Service
public class quochoc {

    private final RestTemplate restTemplate = new RestTemplate();
    private final String URL = "https://znfxhbrkabxenuzrcogd.supabase.co/rest/v1";
    private final String API_KEY = "sb_secret_NXFJy_AuCYhqJmmJadDKNA_I7N91qFu";

    // =============================
    // FETCH ALL DATA (AUTO PAGINATION)
    // =============================
    private List<Map<String, Object>> fetchAllSensorData(boolean full) {

        List<Map<String, Object>> allData = new ArrayList<>();

        int limit = 1000;
        int offset = 0;

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("apikey", API_KEY);
            headers.set("Authorization", "Bearer " + API_KEY);
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<String> entity = new HttpEntity<>(headers);

            while (true) {

                String select = full ? "*" : "created_at,energy";

                String url = URL + "/sensor_data?select=" + select +
                        "&limit=" + limit +
                        "&offset=" + offset;

                ResponseEntity<List<Map<String, Object>>> response = restTemplate.exchange(
                        url,
                        HttpMethod.GET,
                        entity,
                        new ParameterizedTypeReference<List<Map<String, Object>>>() {
                        });

                List<Map<String, Object>> batch = response.getBody();

                if (batch == null || batch.isEmpty())
                    break;

                allData.addAll(batch);

                offset += limit;
            }

        } catch (Exception e) {
            System.out.println("FETCH ERROR: " + e.getMessage());
        }

        return allData;
    }

    // =============================
    // YEAR → GROUP BY MONTH
    // =============================
    public List<Map<String, Object>> getYearlyEnergy(Integer yearFilter) {

        List<Map<String, Object>> data = fetchAllSensorData(false);
        Map<String, Double> map = new HashMap<>();

        for (Map<String, Object> row : data) {
            try {
                String created = (String) row.get("created_at");
                if (created == null || created.length() < 7)
                    continue;

                int year = Integer.parseInt(created.substring(0, 4));

                if (yearFilter == null || year == yearFilter) {

                    String month = created.substring(0, 7);

                    Double energy = row.get("energy") != null
                            ? ((Number) row.get("energy")).doubleValue()
                            : 0.0;

                    map.put(month, map.getOrDefault(month, 0.0) + energy);
                }

            } catch (Exception e) {
                System.out.println("YEAR ERROR: " + e.getMessage());
            }
        }

        List<Map<String, Object>> result = new ArrayList<>();

        map.entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByKey().reversed())
                .forEach(e -> result.add(Map.of(
                        "thang", e.getKey(),
                        "tong_nang_luong", e.getValue())));

        return result;
    }

    // =============================
    // MONTH → GROUP BY DAY
    // =============================
    public List<Map<String, Object>> getMonthlyEnergy(String monthFilter) {

        List<Map<String, Object>> data = fetchAllSensorData(false);
        Map<String, Double> map = new HashMap<>();

        for (Map<String, Object> row : data) {
            try {
                String created = (String) row.get("created_at");
                if (created == null || created.length() < 10)
                    continue;

                String month = created.substring(0, 7);

                if (monthFilter == null || month.equals(monthFilter)) {

                    String day = created.substring(0, 10);

                    Double energy = row.get("energy") != null
                            ? ((Number) row.get("energy")).doubleValue()
                            : 0.0;

                    map.put(day, map.getOrDefault(day, 0.0) + energy);
                }

            } catch (Exception e) {
                System.out.println("MONTH ERROR: " + e.getMessage());
            }
        }

        List<Map<String, Object>> result = new ArrayList<>();

        map.entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByKey().reversed())
                .forEach(e -> result.add(Map.of(
                        "ngay", e.getKey(),
                        "tong_nang_luong", e.getValue())));

        return result;
    }

    // =============================
    // DAY → RAW DATA (THEO GIỜ)
    // =============================
    public List<Map<String, Object>> getDataByDay(String dayFilter) {

        List<Map<String, Object>> data = fetchAllSensorData(true);
        List<Map<String, Object>> result = new ArrayList<>();

        for (Map<String, Object> row : data) {
            try {
                String created = (String) row.get("created_at");
                if (created == null || created.length() < 19)
                    continue;

                String day = created.substring(0, 10);

                if (dayFilter == null || day.equals(dayFilter)) {

                    String time = created.substring(11, 19);

                    Map<String, Object> map = new HashMap<>();
                    map.put("time", time);
                    map.put("voltage", row.get("voltage"));
                    map.put("current", row.get("current"));
                    map.put("power", row.get("power"));
                    map.put("energy", row.get("energy"));

                    result.add(map);
                }

            } catch (Exception e) {
                System.out.println("DAY ERROR: " + e.getMessage());
            }
        }

        // sort theo giờ
        result.sort((a, b) -> ((String) a.get("time"))
                .compareTo((String) b.get("time")));

        return result;
    }

    private List<Map<String, Object>> fetchAlerts() {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("apikey", API_KEY);
            headers.set("Authorization", "Bearer " + API_KEY);
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<String> entity = new HttpEntity<>(headers);

            // ⚠️ đổi alert -> alerts
            String url = URL + "/alerts?select=*";

            ResponseEntity<List<Map<String, Object>>> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    new ParameterizedTypeReference<List<Map<String, Object>>>() {
                    });

            return response.getBody() != null ? response.getBody() : new ArrayList<>();

        } catch (Exception e) {
            System.out.println("Fetch ALERT error: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    // =============================
    // GET ALL ALERTS
    // =============================
    public List<Map<String, Object>> getAllAlerts() {
        return fetchAlerts();
    }

    // =============================
    // GET ALERT BY DEVICE
    // =============================
    public List<Map<String, Object>> getAlertsByDevice(Integer deviceId) {

        List<Map<String, Object>> data = fetchAlerts();
        List<Map<String, Object>> result = new ArrayList<>();

        for (Map<String, Object> row : data) {
            try {
                Integer dId = row.get("device_id") != null
                        ? ((Number) row.get("device_id")).intValue()
                        : null;

                if (deviceId == null || (dId != null && dId.equals(deviceId))) {
                    result.add(row);
                }

            } catch (Exception e) {
                System.out.println("FILTER ALERT error: " + e.getMessage());
            }
        }

        return result;
    }

    // =============================
    // GET UNREAD ALERT
    // =============================
    public List<Map<String, Object>> getUnreadAlerts() {

        List<Map<String, Object>> data = fetchAlerts();
        List<Map<String, Object>> result = new ArrayList<>();

        for (Map<String, Object> row : data) {
            try {
                Boolean isRead = (Boolean) row.get("is_read");

                if (isRead != null && !isRead) {
                    result.add(row);
                }

            } catch (Exception e) {
                System.out.println("UNREAD ALERT error: " + e.getMessage());
            }
        }

        return result;
    }

    // =============================
    // GET ALERT THEO NGÀY
    // =============================
    public List<Map<String, Object>> getAlertsByDay(String dayFilter) {

        List<Map<String, Object>> data = fetchAlerts();
        List<Map<String, Object>> result = new ArrayList<>();

        for (Map<String, Object> row : data) {
            try {
                String created = (String) row.get("created_at");

                if (created == null || created.length() < 10)
                    continue;

                String day = created.substring(0, 10);

                if (dayFilter == null || day.equals(dayFilter)) {
                    result.add(row);
                }

            } catch (Exception e) {
                System.out.println("DAY ALERT error: " + e.getMessage());
            }
        }

        return result;
    }
}