package com.example.demo.service;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class daocuong {

    private final RestTemplate restTemplate = new RestTemplate();
    private final String URL = "https://znfxhbrkabxenuzrcogd.supabase.co/rest/v1";
    private final String API_KEY = "sb_secret_NXFJy_AuCYhqJmmJadDKNA_I7N91qFu";

    private HttpEntity<String> supabaseEntity() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("apikey", API_KEY);
        headers.set("Authorization", "Bearer " + API_KEY);
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Prefer", "return=representation");
        return new HttpEntity<>(headers);
    }

    private HttpEntity<String> supabaseEntity(String jsonBody) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("apikey", API_KEY);
        headers.set("Authorization", "Bearer " + API_KEY);
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Prefer", "return=representation");
        return new HttpEntity<>(jsonBody, headers);
    }

    /**
     * Task 3: Lấy danh sách device_status.
     */
    public List<Map<String, Object>> getDeviceStatuses() {
        String requestUrl = URL + "/device_status?select=*";
        ResponseEntity<Map<String, Object>[]> response = restTemplate.exchange(
                requestUrl,
                HttpMethod.GET,
                supabaseEntity(),
                (Class<Map<String, Object>[]>) (Class<?>) Map[].class);
        Map<String, Object>[] body = response.getBody();
        List<Map<String, Object>> result = new ArrayList<>();
        if (body != null) {
            for (Map<String, Object> row : body) {
                result.add(row);
            }
        }
        return result;
    }

    /**
     * Task 3: Kiểm tra offline theo phút. Nếu thiết bị quá thời gian không gửi dữ liệu:
     * - cập nhật device_status.is_online = false
     * - tạo alert type="offline" (chỉ khi trạng thái chuyển từ online -> offline)
     */
    public Map<String, Object> checkOfflineDevices(int offlineMinutes) {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        List<Map<String, Object>> statuses = getDeviceStatuses();

        int updated = 0;
        int alertsCreated = 0;
        List<Map<String, Object>> offlineDevices = new ArrayList<>();

        for (Map<String, Object> st : statuses) {
            Integer statusId = asInt(st.get("id"));
            Integer deviceId = asInt(st.get("device_id"));
            String lastSeenStr = st.get("last_seen") != null ? st.get("last_seen").toString() : null;
            boolean wasOnline = asBool(st.get("is_online"));

            OffsetDateTime lastSeen = parseOffsetDateTime(lastSeenStr);
            boolean shouldBeOnline = lastSeen != null && Duration.between(lastSeen, now).toMinutes() <= offlineMinutes;

            if (!shouldBeOnline) {
                Map<String, Object> off = new HashMap<>();
                off.put("device_id", deviceId);
                off.put("status_id", statusId);
                off.put("last_seen", lastSeenStr);
                off.put("minutes_since_last_seen",
                        lastSeen == null ? null : Duration.between(lastSeen, now).toMinutes());
                offlineDevices.add(off);
            }

            if (statusId != null) {
                // cập nhật online/offline theo computed
                if (wasOnline != shouldBeOnline) {
                    boolean ok = patchDeviceStatusOnline(statusId, shouldBeOnline);
                    if (ok) {
                        updated++;
                        if (wasOnline && !shouldBeOnline && deviceId != null) {
                            boolean created = createAlert(deviceId, "offline",
                                    "Thiết bị offline: không gửi dữ liệu quá " + offlineMinutes + " phút");
                            if (created) alertsCreated++;
                        }
                    }
                }
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("offline_minutes", offlineMinutes);
        result.put("checked_at", now.toString());
        result.put("updated_status_rows", updated);
        result.put("alerts_created", alertsCreated);
        result.put("offline_devices", offlineDevices);
        return result;
    }

    private boolean patchDeviceStatusOnline(int statusId, boolean isOnline) {
        String requestUrl = URL + "/device_status?id=eq." + statusId;
        String payload = "{\"is_online\":" + (isOnline ? "true" : "false") + "}";
        try {
            restTemplate.exchange(requestUrl, HttpMethod.PATCH, supabaseEntity(payload), String.class);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Task 4: Check vượt ngưỡng theo DeviceLimit (max_power/max_current) dựa trên bản ghi sensor_data mới nhất.
     * Nếu vượt -> tạo alert type="limit".
     */
    public Map<String, Object> checkThresholdForDevice(int deviceId) {
        Map<String, Object> latestSensor = getLatestSensorData(deviceId);
        Map<String, Object> latestLimit = getLatestDeviceLimit(deviceId);

        Map<String, Object> result = new HashMap<>();
        result.put("device_id", deviceId);
        result.put("sensor_data", latestSensor);
        result.put("device_limit", latestLimit);

        if (latestSensor == null || latestLimit == null) {
            result.put("exceeded", false);
            result.put("reason", "Thiếu sensor_data hoặc device_limit");
            return result;
        }

        Double power = asDouble(latestSensor.get("power"));
        Double current = asDouble(latestSensor.get("current"));
        Double maxPower = asDouble(latestLimit.get("max_power"));
        Double maxCurrent = asDouble(latestLimit.get("max_current"));

        boolean exceedPower = maxPower != null && power != null && power > maxPower;
        boolean exceedCurrent = maxCurrent != null && current != null && current > maxCurrent;
        boolean exceeded = exceedPower || exceedCurrent;

        result.put("exceed_power", exceedPower);
        result.put("exceed_current", exceedCurrent);
        result.put("exceeded", exceeded);

        if (exceeded) {
            StringBuilder msg = new StringBuilder("Vượt ngưỡng: ");
            if (exceedPower) {
                msg.append("power=").append(power).append("W > ").append(maxPower).append("W; ");
            }
            if (exceedCurrent) {
                msg.append("current=").append(current).append("A > ").append(maxCurrent).append("A; ");
            }
            boolean created = createAlertIfNotDuplicateRecently(deviceId, "limit", msg.toString().trim(), 5);
            result.put("alert_created", created);
        } else {
            result.put("alert_created", false);
        }

        return result;
    }

    public List<Map<String, Object>> getAlerts(Integer deviceId, Boolean unreadOnly, Integer limit) {
        StringBuilder requestUrl = new StringBuilder(URL).append("/alerts?select=*");
        if (deviceId != null) requestUrl.append("&device_id=eq.").append(deviceId);
        if (unreadOnly != null && unreadOnly) requestUrl.append("&is_read=eq.false");
        requestUrl.append("&order=id.desc");
        if (limit != null && limit > 0) requestUrl.append("&limit=").append(limit);

        ResponseEntity<Map<String, Object>[]> response = restTemplate.exchange(
                requestUrl.toString(),
                HttpMethod.GET,
                supabaseEntity(),
                (Class<Map<String, Object>[]>) (Class<?>) Map[].class);

        Map<String, Object>[] body = response.getBody();
        List<Map<String, Object>> result = new ArrayList<>();
        if (body != null) {
            for (Map<String, Object> row : body) {
                result.add(row);
            }
        }
        return result;
    }

    private Map<String, Object> getLatestSensorData(int deviceId) {
        String requestUrl = URL + "/sensor_data?select=*&device_id=eq." + deviceId + "&order=created_at.desc&limit=1";
        ResponseEntity<Map<String, Object>[]> response = restTemplate.exchange(
                requestUrl,
                HttpMethod.GET,
                supabaseEntity(),
                (Class<Map<String, Object>[]>) (Class<?>) Map[].class);
        Map<String, Object>[] body = response.getBody();
        return (body != null && body.length > 0) ? body[0] : null;
    }

    private Map<String, Object> getLatestDeviceLimit(int deviceId) {
        String requestUrl = URL + "/device_limits?select=*&device_id=eq." + deviceId + "&order=created_at.desc&limit=1";
        ResponseEntity<Map<String, Object>[]> response = restTemplate.exchange(
                requestUrl,
                HttpMethod.GET,
                supabaseEntity(),
                (Class<Map<String, Object>[]>) (Class<?>) Map[].class);
        Map<String, Object>[] body = response.getBody();
        return (body != null && body.length > 0) ? body[0] : null;
    }

    private boolean createAlert(int deviceId, String type, String message) {
        String requestUrl = URL + "/alerts";
        String payload = "{\"device_id\":" + deviceId + ",\"type\":\"" + escapeJson(type) + "\",\"message\":\""
                + escapeJson(message) + "\",\"is_read\":false}";
        try {
            restTemplate.exchange(requestUrl, HttpMethod.POST, supabaseEntity(payload), String.class);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Tránh spam: nếu alert cùng type gần đây (trong N phút) thì không tạo mới.
     * (Dựa vào created_at nếu bảng alerts có trường này; nếu không có thì fallback tạo mới.)
     */
    private boolean createAlertIfNotDuplicateRecently(int deviceId, String type, String message, int withinMinutes) {
        try {
            String requestUrl = URL + "/alerts?select=id,created_at,type&device_id=eq." + deviceId + "&type=eq." + type
                    + "&order=id.desc&limit=1";
            ResponseEntity<Map<String, Object>[]> response = restTemplate.exchange(
                    requestUrl,
                    HttpMethod.GET,
                    supabaseEntity(),
                    (Class<Map<String, Object>[]>) (Class<?>) Map[].class);
            Map<String, Object>[] body = response.getBody();
            if (body != null && body.length > 0) {
                Object createdAtObj = body[0].get("created_at");
                OffsetDateTime createdAt = createdAtObj != null ? parseOffsetDateTime(createdAtObj.toString()) : null;
                if (createdAt != null) {
                    long mins = Duration.between(createdAt, OffsetDateTime.now(ZoneOffset.UTC)).toMinutes();
                    if (mins >= 0 && mins <= withinMinutes) {
                        return false;
                    }
                }
            }
        } catch (Exception ignored) {
            // ignore, fallback create
        }
        return createAlert(deviceId, type, message);
    }

    private static Integer asInt(Object v) {
        if (v == null) return null;
        if (v instanceof Number) return ((Number) v).intValue();
        try {
            return Integer.parseInt(v.toString());
        } catch (Exception e) {
            return null;
        }
    }

    private static Double asDouble(Object v) {
        if (v == null) return null;
        if (v instanceof Number) return ((Number) v).doubleValue();
        try {
            return Double.parseDouble(v.toString());
        } catch (Exception e) {
            return null;
        }
    }

    private static boolean asBool(Object v) {
        if (v == null) return false;
        if (v instanceof Boolean) return (Boolean) v;
        String s = v.toString().trim().toLowerCase();
        return "true".equals(s) || "1".equals(s) || "yes".equals(s);
    }

    private static OffsetDateTime parseOffsetDateTime(String value) {
        if (value == null || value.isBlank()) return null;
        try {
            return OffsetDateTime.parse(value);
        } catch (Exception e) {
            return null;
        }
    }

    private static String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r");
    }
}