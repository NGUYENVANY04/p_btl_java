package com.Iot.backend.service;

/*
 * ===== TEMPLATE NHÓM (GIỮ LẠI) =====
 *
 * // import com.Iot.backend.model.SensorData;
 * // import com.Iot.backend.model.Alert;
 * // import com.Iot.backend.model.Device;
 * // import com.Iot.backend.model.DeviceStatus;
 * // import com.Iot.backend.model.DeviceLimit;
 * // dùng cái nào thì xóa cmt cái đấy
 *
 * // import org.springframework.http.*;
 * // import org.springframework.stereotype.Service;
 * // import org.springframework.web.client.RestTemplate;
 *
 * // @Service
 * // public class daocuong {
 *
 * // private final RestTemplate restTemplate = new RestTemplate();
 * // private final String URL =
 * // "https://znfxhbrkabxenuzrcogd.supabase.co/rest/v1";
 * // private final String API_KEY = "sb_secret_NXFJy_AuCYhqJmmJadDKNA_I7N91qFu";
 *
 * // // thêm các chức năng get post del update tại đây
 *
 * // }
 */

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.HttpStatusCodeException;

import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class daocuong {

    private final RestTemplate restTemplate = new RestTemplate();
    private final String URL = "https://znfxhbrkabxenuzrcogd.supabase.co/rest/v1";
    private final String API_KEY = "sb_secret_NXFJy_AuCYhqJmmJadDKNA_I7N91qFu";

    private static final String TABLE_SENSOR_DATA = "sensor_data";

    // Supabase table names vary by project; we keep defaults and fallback when needed.
    private String tableDeviceStatus = "device_status";
    private String tableDeviceLimit = "device_limit";
    private String tableAlerts = "alerts";

    private final List<String> warnings = new ArrayList<>();

    private HttpHeaders supabaseHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("apikey", API_KEY);
        headers.set("Authorization", "Bearer " + API_KEY);
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }

    private List<Map<String, Object>> getList(String pathAndQuery) {
        try {
            HttpEntity<String> entity = new HttpEntity<>(supabaseHeaders());
            ResponseEntity<List<Map<String, Object>>> response = restTemplate.exchange(
                    URL + pathAndQuery,
                    HttpMethod.GET,
                    entity,
                    new ParameterizedTypeReference<List<Map<String, Object>>>() {
                    });
            return response.getBody() != null ? response.getBody() : new ArrayList<>();
        } catch (HttpStatusCodeException e) {
            warnings.add("Supabase GET failed: " + e.getStatusCode() + " " + e.getResponseBodyAsString());
            return new ArrayList<>();
        } catch (Exception e) {
            warnings.add("Supabase GET failed: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    private Map<String, Object> postReturningFirst(String path, Map<String, Object> body) {
        HttpHeaders headers = supabaseHeaders();
        headers.set("Prefer", "return=representation");
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
        ResponseEntity<List<Map<String, Object>>> response = restTemplate.exchange(
                URL + path,
                HttpMethod.POST,
                entity,
                new ParameterizedTypeReference<List<Map<String, Object>>>() {
                });
        List<Map<String, Object>> list = response.getBody();
        return (list != null && !list.isEmpty()) ? list.get(0) : new HashMap<>();
    }

    private Instant parseSupabaseTimeToInstant(Object createdAtRaw) {
        if (createdAtRaw == null) return null;
        String s = String.valueOf(createdAtRaw).trim();
        if (s.isEmpty()) return null;
        try {
            return OffsetDateTime.parse(s).toInstant();
        } catch (Exception ignored) {
            try {
                // Sometimes Supabase returns without offset; treat as UTC
                return OffsetDateTime.parse(s + "Z").toInstant();
            } catch (Exception ignored2) {
                return null;
            }
        }
    }

    private boolean hasUnreadAlert(Integer deviceId, String type) {
        if (deviceId == null) return false;
        String q = String.format(
                "/%s?select=id&device_id=eq.%d&type=eq.%s&is_read=is.false&limit=1",
                tableAlerts, deviceId, type
        );
        List<Map<String, Object>> r = getList(q);
        return r != null && !r.isEmpty();
    }

    private Map<String, Object> createAlert(Integer deviceId, String type, String message) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("device_id", deviceId);
        payload.put("type", type);
        payload.put("message", message);
        payload.put("is_read", false);
        return postReturningFirst("/" + tableAlerts, payload);
    }

    private List<Map<String, Object>> getListWithFallback(String currentTable, String[] fallbacks, String selectQuery) {
        // try current first
        List<Map<String, Object>> r = getList("/" + currentTable + selectQuery);
        if (!r.isEmpty() || warnings.isEmpty()) return r;

        // if current failed (warnings appended), try fallbacks until one works (even empty but no new warning)
        int warningsBefore = warnings.size();
        for (String t : fallbacks) {
            if (t.equals(currentTable)) continue;
            int w0 = warnings.size();
            List<Map<String, Object>> rr = getList("/" + t + selectQuery);
            boolean addedWarning = warnings.size() > w0;
            if (!addedWarning) return rr; // success (even if empty)
        }

        // nothing worked; keep original result (empty) and warnings
        if (warnings.size() > warningsBefore) {
            warnings.add("Tried fallbacks for table " + currentTable + " but all failed.");
        }
        return r;
    }

    /**
     * TASK 3: Phát hiện thiết bị offline.
     * - Lấy danh sách device_status
     * - Nếu last_seen quá thời gian (minutes) => offline
     * - Nếu chưa có alert OFFLINE chưa đọc => tạo alert
     */
    public Map<String, Object> checkOfflineDevices(int minutes) {
        int thresholdMinutes = Math.max(minutes, 1);
        Instant now = Instant.now();

        warnings.clear();
        List<Map<String, Object>> statuses = getListWithFallback(
                tableDeviceStatus,
                new String[] { "device_statuses" },
                "?select=device_id,last_seen,is_online"
        );
        List<Map<String, Object>> offline = new ArrayList<>();
        int createdAlerts = 0;

        for (Map<String, Object> row : statuses) {
            Integer deviceId = row.get("device_id") instanceof Number ? ((Number) row.get("device_id")).intValue() : null;
            Instant lastSeen = parseSupabaseTimeToInstant(row.get("last_seen"));
            if (deviceId == null || lastSeen == null) continue;

            long diffMinutes = Duration.between(lastSeen, now).toMinutes();
            boolean offlineByTime = diffMinutes >= thresholdMinutes;
            boolean offlineByFlag = row.get("is_online") instanceof Boolean && !((Boolean) row.get("is_online"));

            if (offlineByTime || offlineByFlag) {
                Map<String, Object> item = new HashMap<>();
                item.put("device_id", deviceId);
                item.put("last_seen", row.get("last_seen"));
                item.put("minutes_since_last_seen", diffMinutes);
                offline.add(item);

                if (!hasUnreadAlert(deviceId, "OFFLINE")) {
                    createAlert(deviceId, "OFFLINE", "Thiết bị offline (quá " + thresholdMinutes + " phút không gửi dữ liệu).");
                    createdAlerts++;
                }
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("threshold_minutes", thresholdMinutes);
        result.put("offline_devices", offline);
        result.put("created_alerts", createdAlerts);
        if (!warnings.isEmpty()) result.put("warnings", new ArrayList<>(warnings));
        return result;
    }

    /**
     * TASK 4: Tạo cảnh báo vượt ngưỡng.
     * - Lấy device_limit (max_power, max_current)
     * - Với mỗi device => lấy bản ghi sensor_data mới nhất
     * - Nếu power/current vượt ngưỡng => tạo alert (POWER_OVER / CURRENT_OVER)
     */
    public Map<String, Object> checkThresholdAlerts() {
        warnings.clear();
        List<Map<String, Object>> limits = getListWithFallback(
                tableDeviceLimit,
                new String[] { "device_limits" },
                "?select=device_id,max_power,max_current"
        );
        List<Map<String, Object>> over = new ArrayList<>();
        int createdAlerts = 0;

        for (Map<String, Object> lim : limits) {
            Integer deviceId = lim.get("device_id") instanceof Number ? ((Number) lim.get("device_id")).intValue() : null;
            if (deviceId == null) continue;

            Double maxPower = lim.get("max_power") instanceof Number ? ((Number) lim.get("max_power")).doubleValue() : null;
            Double maxCurrent = lim.get("max_current") instanceof Number ? ((Number) lim.get("max_current")).doubleValue() : null;

            String q = String.format(
                    "/%s?select=device_id,power,current,created_at&device_id=eq.%d&order=created_at.desc&limit=1",
                    TABLE_SENSOR_DATA, deviceId
            );
            List<Map<String, Object>> latestList = getList(q);
            if (latestList.isEmpty()) continue;

            Map<String, Object> latest = latestList.get(0);
            Double power = latest.get("power") instanceof Number ? ((Number) latest.get("power")).doubleValue() : null;
            Double current = latest.get("current") instanceof Number ? ((Number) latest.get("current")).doubleValue() : null;

            boolean powerOver = (maxPower != null && power != null && power > maxPower);
            boolean currentOver = (maxCurrent != null && current != null && current > maxCurrent);
            if (!powerOver && !currentOver) continue;

            Map<String, Object> item = new HashMap<>();
            item.put("device_id", deviceId);
            item.put("latest", latest);
            item.put("limit", lim);
            item.put("power_over", powerOver);
            item.put("current_over", currentOver);
            over.add(item);

            if (powerOver && !hasUnreadAlert(deviceId, "POWER_OVER")) {
                createAlert(deviceId, "POWER_OVER",
                        "Vượt ngưỡng công suất: " + power + " > " + maxPower);
                createdAlerts++;
            }
            if (currentOver && !hasUnreadAlert(deviceId, "CURRENT_OVER")) {
                createAlert(deviceId, "CURRENT_OVER",
                        "Vượt ngưỡng dòng điện: " + current + " > " + maxCurrent);
                createdAlerts++;
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("over_limit_devices", over);
        result.put("created_alerts", createdAlerts);
        if (!warnings.isEmpty()) result.put("warnings", new ArrayList<>(warnings));
        return result;
    }

    public List<Map<String, Object>> getLatestAlerts(int limit) {
        int safeLimit = Math.min(Math.max(limit, 1), 200);
        warnings.clear();
        String q = String.format("?select=id,device_id,type,message,is_read&order=id.desc&limit=%d", safeLimit);
        List<Map<String, Object>> r = getListWithFallback(
                tableAlerts,
                new String[] { "alert" },
                q
        );
        return r;
    }
}