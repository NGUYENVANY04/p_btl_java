package com.Iot.backend.service;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

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

    private List<Map<String, Object>> fetchAllSensorData(boolean full, List<Integer> deviceIds) {
        List<Map<String, Object>> allData = new ArrayList<>();
        if (deviceIds == null || deviceIds.isEmpty())
            return allData;

        int limit = 1000;
        int offset = 0;
        String idsString = deviceIds.stream().map(String::valueOf).collect(Collectors.joining(","));

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("apikey", API_KEY);
            headers.set("Authorization", "Bearer " + API_KEY);
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<String> entity = new HttpEntity<>(headers);

            while (true) {
                String select = full ? "*" : "created_at,delta_energy,device_id";
                String url = URL + "/sensor_data_processed?select=" + select + "&device_id=in.(" + idsString
                        + ")&limit=" + limit + "&offset=" + offset;
                ResponseEntity<List<Map<String, Object>>> response = restTemplate.exchange(url, HttpMethod.GET, entity,
                        new ParameterizedTypeReference<List<Map<String, Object>>>() {
                        });
                List<Map<String, Object>> batch = response.getBody();
                if (batch == null || batch.isEmpty())
                    break;
                allData.addAll(batch);
                offset += limit;
            }
        } catch (Exception e) {
            System.out.println("❌ FETCH ERROR: " + e.getMessage());
        }
        return allData;
    }

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
        map.entrySet().stream().sorted(Map.Entry.<Integer, Double>comparingByKey().reversed())
                .forEach(e -> result.add(Map.of("nam", e.getKey(), "tong_nang_luong", round(e.getValue()))));
        return result;
    }

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
        map.entrySet().stream().sorted(Map.Entry.<String, Double>comparingByKey().reversed())
                .forEach(e -> result.add(Map.of("thang", e.getKey(), "tong_nang_luong", round(e.getValue()))));
        return result;
    }

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
        map.entrySet().stream().sorted(Map.Entry.<String, Double>comparingByKey().reversed())
                .forEach(e -> result.add(Map.of("ngay", e.getKey(), "tong_nang_luong", round(e.getValue()))));
        return result;
    }

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
        map.entrySet().stream().sorted(Map.Entry.comparingByKey())
                .forEach(e -> result.add(Map.of("time", e.getKey(), "tong_nang_luong", round(e.getValue()))));
        return result;
    }

    public List<Integer> getDeviceIdsByUser(Integer userId) {
        List<Integer> ids = new ArrayList<>();
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("apikey", API_KEY);
            headers.set("Authorization", "Bearer " + API_KEY);
            HttpEntity<String> entity = new HttpEntity<>(headers);
            String url = URL + "/user_devices?user_id=eq." + userId + "&select=device_id";
            ResponseEntity<List<Map<String, Object>>> response = restTemplate.exchange(url, HttpMethod.GET, entity,
                    new ParameterizedTypeReference<List<Map<String, Object>>>() {
                    });
            List<Map<String, Object>> data = response.getBody();
            if (data != null && !data.isEmpty()) {
                for (Map<String, Object> map : data)
                    ids.add(((Number) map.get("device_id")).intValue());
            }
        } catch (Exception e) {
            System.out.println("❌ DEVICE ERROR: " + e.getMessage());
        }
        return ids;
    }

    public List<Map<String, Object>> getAllAlerts() {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("apikey", API_KEY);
            headers.set("Authorization", "Bearer " + API_KEY);
            HttpEntity<String> entity = new HttpEntity<>(headers);
            ResponseEntity<List<Map<String, Object>>> response = restTemplate.exchange(
                    URL + "/alerts?select=*&order=created_at.desc", HttpMethod.GET, entity,
                    new ParameterizedTypeReference<List<Map<String, Object>>>() {
                    });
            return response.getBody() != null ? response.getBody() : new ArrayList<>();
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    // ============================================
    // 1. LẤY HÓA ĐƠN & KIỂM TRA ĐÃ THANH TOÁN
    // ============================================
    public List<Map<String, Object>> getMonthlyBills(Integer userId, List<Integer> deviceIds) {
        try {
            if (deviceIds == null || deviceIds.isEmpty())
                return new ArrayList<>();
            String idsString = deviceIds.stream().map(String::valueOf).collect(Collectors.joining(","));

            HttpHeaders headers = new HttpHeaders();
            headers.set("apikey", API_KEY);
            headers.set("Authorization", "Bearer " + API_KEY);
            HttpEntity<String> entity = new HttpEntity<>(headers);

            // A. Lấy hóa đơn từ View (Mặc định unpaid)
            String url = URL + "/payments_auto?user_id=eq." + userId + "&device_id=in.(" + idsString + ")";
            ResponseEntity<List<Map<String, Object>>> response = restTemplate.exchange(url, HttpMethod.GET, entity,
                    new ParameterizedTypeReference<List<Map<String, Object>>>() {
                    });
            List<Map<String, Object>> data = response.getBody();
            if (data == null || data.isEmpty())
                return new ArrayList<>();

            // B. Lấy các tháng đã thanh toán từ bảng paid_bills
            String paidUrl = URL + "/paid_bills?user_id=eq." + userId + "&select=month";
            ResponseEntity<List<Map<String, Object>>> paidResponse = restTemplate.exchange(paidUrl, HttpMethod.GET,
                    entity, new ParameterizedTypeReference<List<Map<String, Object>>>() {
                    });
            List<Map<String, Object>> paidData = paidResponse.getBody();

            Set<String> paidMonths = new HashSet<>();
            if (paidData != null) {
                for (Map<String, Object> p : paidData) {
                    paidMonths.add((String) p.get("month"));
                }
            }

            // C. Gộp số liệu các máy trong cùng 1 tháng
            Map<String, Map<String, Object>> grouped = new HashMap<>();
            String currentMonthStr = java.time.YearMonth.now().toString();

            for (Map<String, Object> row : data) {
                String month = (String) row.get("month");
                double energy = ((Number) row.get("total_energy")).doubleValue();
                double amount = ((Number) row.get("total_amount")).doubleValue();
                Integer devId = ((Number) row.get("device_id")).intValue();

                // NẾU THÁNG NÀY CÓ TRONG BẢNG paid_bills -> ĐỔI THÀNH PAID
                String finalStatus = paidMonths.contains(month) ? "paid" : "unpaid";

                grouped.putIfAbsent(month, new HashMap<>(Map.of("month", month, "total_energy", 0.0, "total_amount",
                        0.0, "status", finalStatus, "can_pay", false, "devices", new HashSet<Integer>())));

                Map<String, Object> monthData = grouped.get(month);
                monthData.put("total_energy", round((double) monthData.get("total_energy") + energy));
                monthData.put("total_amount", (double) monthData.get("total_amount") + amount);

                @SuppressWarnings("unchecked")
                Set<Integer> devs = (Set<Integer>) monthData.get("devices");
                devs.add(devId);
            }

            // Chuẩn bị dữ liệu trả về cho Web
            List<Map<String, Object>> result = new ArrayList<>();
            for (Map<String, Object> m : grouped.values()) {
                String mStr = (String) m.get("month");
                // Chặn tháng hiện tại
                if ("unpaid".equals(m.get("status")) && mStr.compareTo(currentMonthStr) < 0)
                    m.put("can_pay", true);
                else
                    m.put("can_pay", false);

                @SuppressWarnings("unchecked")
                Set<Integer> devs = (Set<Integer>) m.get("devices");
                m.put("devices", new ArrayList<>(devs));
                result.add(m);
            }

            // Sắp xếp tháng mới nhất lên đầu
            result.sort((a, b) -> ((String) b.get("month")).compareTo((String) a.get("month")));
            return result;

        } catch (Exception e) {
            System.out.println("❌ BILL ERROR: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    // ============================================
    // 2. LƯU LỊCH SỬ THANH TOÁN (INSERT)
    // ============================================
    public boolean payBill(Integer userId, List<Integer> deviceIds, String month) {
        try {
            // Lưu dữ liệu vào bảng thật paid_bills
            String urlString = URL + "/paid_bills";

            String jsonBody = String.format("{\"user_id\": %d, \"month\": \"%s\"}", userId, month);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(urlString))
                    .header("apikey", API_KEY)
                    .header("Authorization", "Bearer " + API_KEY)
                    .header("Content-Type", "application/json")
                    // Dùng POST để tạo mới (Insert)
                    .method("POST", HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();

            HttpClient client = HttpClient.newHttpClient();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                System.out.println("✅ ĐÃ GHI NHẬN THANH TOÁN CHO THÁNG " + month);
                return true;
            } else {
                System.err.println("❌ LỖI TỪ SUPABASE: " + response.body());
                return false;
            }

        } catch (Exception e) {
            System.err.println("❌ LỖI JAVA KHI GHI NHẬN THANH TOÁN: " + e.getMessage());
            return false;
        }
    }
}