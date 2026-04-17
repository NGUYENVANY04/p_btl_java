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

import com.Iot.backend.dto.OfflineCheckResultDto;
import com.Iot.backend.dto.OfflineDeviceDto;
import com.Iot.backend.dto.OverLimitDeviceDto;
import com.Iot.backend.dto.ThresholdCheckResultDto;
import com.Iot.backend.model.Alert;
import com.Iot.backend.model.DeviceLimit;
import com.Iot.backend.model.DeviceStatus;
import com.Iot.backend.model.SensorData;

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
import java.time.LocalDateTime;
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

    private LocalDateTime parseSupabaseTimeToLocalDateTime(Object createdAtRaw) {
        Instant instant = parseSupabaseTimeToInstant(createdAtRaw);
        return instant != null ? LocalDateTime.ofInstant(instant, ZoneOffset.UTC) : null;
    }

    private Integer toInt(Object value) {
        return value instanceof Number ? ((Number) value).intValue() : null;
    }

    private Float toFloat(Object value) {
        return value instanceof Number ? ((Number) value).floatValue() : null;
    }

    private Long toLong(Object value) {
        return value instanceof Number ? ((Number) value).longValue() : null;
    }

    private DeviceStatus toDeviceStatus(Map<String, Object> row) {
        DeviceStatus status = new DeviceStatus();
        status.setDevice_id(toInt(row.get("device_id")));
        status.setLast_seen(parseSupabaseTimeToLocalDateTime(row.get("last_seen")));
        status.setIs_online(row.get("is_online") instanceof Boolean ? (Boolean) row.get("is_online") : null);
        return status;
    }

    private DeviceLimit toDeviceLimit(Map<String, Object> row) {
        DeviceLimit limit = new DeviceLimit();
        limit.setDevice_id(toInt(row.get("device_id")));
        limit.setMax_power(toFloat(row.get("max_power")));
        limit.setMax_current(toFloat(row.get("max_current")));
        return limit;
    }

    private SensorData toSensorData(Map<String, Object> row) {
        SensorData sensor = new SensorData();
        sensor.setDevice_id(toInt(row.get("device_id")));
        sensor.setPower(toFloat(row.get("power")));
        sensor.setCurrent(toFloat(row.get("current")));
        sensor.setCreated_at(parseSupabaseTimeToLocalDateTime(row.get("created_at")));
        return sensor;
    }

    private Alert toAlert(Map<String, Object> row) {
        Alert alert = new Alert();
        alert.setId(toLong(row.get("id")));
        alert.setDevice_id(toInt(row.get("device_id")));
        alert.setType(row.get("type") != null ? String.valueOf(row.get("type")) : null);
        alert.setMessage(row.get("message") != null ? String.valueOf(row.get("message")) : null);
        alert.setIs_read(row.get("is_read") instanceof Boolean ? (Boolean) row.get("is_read") : null);
        return alert;
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
    public OfflineCheckResultDto checkOfflineDevices(int minutes) {
        int thresholdMinutes = Math.max(minutes, 1);
        Instant now = Instant.now();

        warnings.clear();
        List<Map<String, Object>> rawStatuses = getListWithFallback(
                tableDeviceStatus,
                new String[] { "device_statuses" },
                "?select=device_id,last_seen,is_online"
        );
        List<OfflineDeviceDto> offline = new ArrayList<>();
        int createdAlerts = 0;

        for (Map<String, Object> row : rawStatuses) {
            DeviceStatus status = toDeviceStatus(row);
            Integer deviceId = status.getDevice_id();
            LocalDateTime lastSeenAt = status.getLast_seen();
            if (deviceId == null || lastSeenAt == null) continue;
            Instant lastSeen = lastSeenAt.toInstant(ZoneOffset.UTC);

            long diffMinutes = Duration.between(lastSeen, now).toMinutes();
            boolean offlineByTime = diffMinutes >= thresholdMinutes;
            boolean offlineByFlag = Boolean.FALSE.equals(status.getIs_online());

            if (offlineByTime || offlineByFlag) {
                OfflineDeviceDto item = new OfflineDeviceDto();
                item.setDevice_id(deviceId);
                item.setLast_seen(row.get("last_seen") != null ? String.valueOf(row.get("last_seen")) : null);
                item.setMinutes_since_last_seen(diffMinutes);
                offline.add(item);

                if (!hasUnreadAlert(deviceId, "OFFLINE")) {
                    createAlert(deviceId, "OFFLINE", "Thiết bị offline (quá " + thresholdMinutes + " phút không gửi dữ liệu).");
                    createdAlerts++;
                }
            }
        }

        OfflineCheckResultDto result = new OfflineCheckResultDto();
        result.setThreshold_minutes(thresholdMinutes);
        result.setOffline_devices(offline);
        result.setCreated_alerts(createdAlerts);
        if (!warnings.isEmpty()) result.setWarnings(new ArrayList<>(warnings));
        return result;
    }

    /**
     * TASK 4: Tạo cảnh báo vượt ngưỡng.
     * - Lấy device_limit (max_power, max_current)
     * - Với mỗi device => lấy bản ghi sensor_data mới nhất
     * - Nếu power/current vượt ngưỡng => tạo alert (POWER_OVER / CURRENT_OVER)
     */
    public ThresholdCheckResultDto checkThresholdAlerts() {
        warnings.clear();
        List<Map<String, Object>> rawLimits = getListWithFallback(
                tableDeviceLimit,
                new String[] { "device_limits" },
                "?select=device_id,max_power,max_current"
        );
        List<OverLimitDeviceDto> over = new ArrayList<>();
        int createdAlerts = 0;

        for (Map<String, Object> lim : rawLimits) {
            DeviceLimit limit = toDeviceLimit(lim);
            Integer deviceId = limit.getDevice_id();
            if (deviceId == null) continue;

            Double maxPower = limit.getMax_power() != null ? limit.getMax_power().doubleValue() : null;
            Double maxCurrent = limit.getMax_current() != null ? limit.getMax_current().doubleValue() : null;

            String q = String.format(
                    "/%s?select=device_id,power,current,created_at&device_id=eq.%d&order=created_at.desc&limit=1",
                    TABLE_SENSOR_DATA, deviceId
            );
            List<Map<String, Object>> latestList = getList(q);
            if (latestList.isEmpty()) continue;

            SensorData latest = toSensorData(latestList.get(0));
            Double power = latest.getPower() != null ? latest.getPower().doubleValue() : null;
            Double current = latest.getCurrent() != null ? latest.getCurrent().doubleValue() : null;

            boolean powerOver = (maxPower != null && power != null && power > maxPower);
            boolean currentOver = (maxCurrent != null && current != null && current > maxCurrent);
            if (!powerOver && !currentOver) continue;

            OverLimitDeviceDto item = new OverLimitDeviceDto();
            item.setDevice_id(deviceId);
            item.setLatest(latest);
            item.setLimit(limit);
            item.setPower_over(powerOver);
            item.setCurrent_over(currentOver);
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

        ThresholdCheckResultDto result = new ThresholdCheckResultDto();
        result.setOver_limit_devices(over);
        result.setCreated_alerts(createdAlerts);
        if (!warnings.isEmpty()) result.setWarnings(new ArrayList<>(warnings));
        return result;
    }

    public List<Alert> getLatestAlerts(int limit) {
        int safeLimit = Math.min(Math.max(limit, 1), 200);
        warnings.clear();
        String q = String.format("?select=id,device_id,type,message,is_read&order=id.desc&limit=%d", safeLimit);
        List<Map<String, Object>> rows = getListWithFallback(
                tableAlerts,
                new String[] { "alert" },
                q
        );
        List<Alert> result = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            result.add(toAlert(row));
        }
        return result;
    }
}