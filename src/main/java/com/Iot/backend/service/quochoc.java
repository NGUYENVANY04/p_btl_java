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
    // FETCH ENERGY (cho YEAR + MONTH)
    // =============================
    private List<Map<String, Object>> fetchSensorData() {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("apikey", API_KEY);
            headers.set("Authorization", "Bearer " + API_KEY);
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<String> entity = new HttpEntity<>(headers);

            String url = URL + "/sensor_data?select=created_at,energy";

            ResponseEntity<List<Map<String, Object>>> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    new ParameterizedTypeReference<List<Map<String, Object>>>() {
                    });

            return response.getBody() != null ? response.getBody() : new ArrayList<>();

        } catch (Exception e) {
            System.out.println("Fetch error: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    // =============================
    // FETCH FULL (cho DAY)
    // =============================
    private List<Map<String, Object>> fetchFullSensorData() {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("apikey", API_KEY);
            headers.set("Authorization", "Bearer " + API_KEY);
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<String> entity = new HttpEntity<>(headers);

            String url = URL + "/sensor_data?select=*";

            ResponseEntity<List<Map<String, Object>>> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    new ParameterizedTypeReference<List<Map<String, Object>>>() {
                    });

            return response.getBody() != null ? response.getBody() : new ArrayList<>();

        } catch (Exception e) {
            System.out.println("Fetch FULL error: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    // =============================
    // YEAR → GROUP BY MONTH
    // =============================
    public List<Map<String, Object>> getYearlyEnergy(Integer yearFilter) {

        List<Map<String, Object>> data = fetchSensorData();
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
                System.out.println("YEAR error: " + e.getMessage());
            }
        }

        List<Map<String, Object>> result = new ArrayList<>();
        map.entrySet().stream()
                .sorted((a, b) -> b.getKey().compareTo(a.getKey()))
                .forEach(e -> result.add(Map.of(
                        "thang", e.getKey(),
                        "tong_nang_luong", e.getValue())));

        return result;
    }

    // =============================
    // MONTH → GROUP BY DAY
    // =============================
    public List<Map<String, Object>> getMonthlyEnergy(String monthFilter) {

        List<Map<String, Object>> data = fetchSensorData();
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
                System.out.println("MONTH error: " + e.getMessage());
            }
        }

        List<Map<String, Object>> result = new ArrayList<>();
        map.entrySet().stream()
                .sorted((a, b) -> b.getKey().compareTo(a.getKey()))
                .forEach(e -> result.add(Map.of(
                        "ngay", e.getKey(),
                        "tong_nang_luong", e.getValue())));

        return result;
    }

    // =============================
    // DAY → RAW DATA THEO GIỜ
    // =============================
    public List<Map<String, Object>> getDataByDay(String dayFilter) {

        List<Map<String, Object>> data = fetchFullSensorData();
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
                System.out.println("DAY error: " + e.getMessage());
            }
        }

        return result;
    }
}