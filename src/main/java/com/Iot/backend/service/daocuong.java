package com.Iot.backend.service;

import com.Iot.backend.dto.OfflineCheckResultDto;
import com.Iot.backend.dto.OfflineDeviceDto;
import com.Iot.backend.dto.OverLimitDeviceDto;
import com.Iot.backend.dto.ThresholdCheckResultDto;
import com.Iot.backend.model.Alert;
import com.Iot.backend.model.DeviceLimit;
import com.Iot.backend.model.SensorData;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
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
import java.util.stream.Collectors;

@Service
public class daocuong {

    private final RestTemplate restTemplate = new RestTemplate();
    private final String URL = "https://znfxhbrkabxenuzrcogd.supabase.co/rest/v1";
    private final String API_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InpuZnhoYnJrYWJ4ZW51enJjb2dkIiwicm9sZSI6InNlcnZpY2Vfcm9sZSIsImlhdCI6MTc3NTQ0Mjk4NSwiZXhwIjoyMDkxMDE4OTg1fQ.qNtUEq0HebqEs6tHrWT6Ghj94-UOb5dshIWAvWB8r6Y";

    private static final String TABLE_SENSOR_DATA = "sensor_data";

    // Supabase table names vary by project; we keep defaults and fallback when needed.
    private String tableDeviceLimit = "device_limit";
    private String tableAlerts = "alerts";
    private String tableUserDevices = "user_devices";
    private String tableUsers = "users";

    private final List<String> warnings = new ArrayList<>();

    @Autowired
    @Lazy
    private ducthinh ducthinhService;

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

    private String getUserEmail(Integer userId) {
        if (userId == null) return null;
        String q = String.format("/%s?select=email&id=eq.%d&limit=1", tableUsers, userId);
        List<Map<String, Object>> rows = getList(q);
        if (rows.isEmpty()) return null;
        Object email = rows.get(0).get("email");
        return email != null ? String.valueOf(email) : null;
    }

    private Map<String, Object> createAlert(Integer deviceId, Integer userId, String type, String message) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("device_id", deviceId);
        payload.put("type", type);
        payload.put("message", message);
        payload.put("is_read", false);
        return postReturningFirst("/" + tableAlerts, payload);
    }

    private Integer getUserIdByDeviceId(Integer deviceId) {
        if (deviceId == null) return null;
        String q = String.format("/%s?select=user_id&device_id=eq.%d&limit=1", tableUserDevices, deviceId);
        List<Map<String, Object>> rows = getList(q);
        if (rows.isEmpty()) return null;
        return toInt(rows.get(0).get("user_id"));
    }

    /**
     * Kiểm tra ngưỡng thời gian thực cho một thiết bị khi có dữ liệu mới từ MQTT.
     */
    public void checkRealtimeThreshold(int deviceId, float power, float current) {
        // 1. Lấy ngưỡng của thiết bị
        String qLimit = String.format("/%s?select=max_power,max_current&device_id=eq.%d&limit=1", tableDeviceLimit, deviceId);
        List<Map<String, Object>> limitRows = getList(qLimit);
        if (limitRows.isEmpty()) return;

        Map<String, Object> lim = limitRows.get(0);
        Float maxPower = toFloat(lim.get("max_power"));
        Float maxCurrent = toFloat(lim.get("max_current"));

        boolean powerOver = (maxPower != null && maxPower > 0 && power > maxPower);
        boolean currentOver = (maxCurrent != null && maxCurrent > 0 && current > maxCurrent);

        if (powerOver || currentOver) {
            Integer userId = getUserIdByDeviceId(deviceId);
            String message = "";
            String type = "";

            if (powerOver) {
                type = "POWER_EXCEEDED";
                message = "Vượt ngưỡng công suất: " + power + " > " + maxPower;
            } else {
                type = "CURRENT_EXCEEDED";
                message = "Vượt ngưỡng dòng điện: " + current + " > " + maxCurrent;
            }

            // 2. Tạo alert
            createAlert(deviceId, userId, type, message);

            // 3. Ngắt thiết bị ngay lập tức
            ducthinhService.controlDevice(deviceId, "OFF");

            // 4. Cập nhật trạng thái thiết bị trong DB về false (OFF)
            updateDeviceStatusInDb(deviceId, false);
            
            System.out.println("🚨 REALTIME ALERT: Device " + deviceId + " exceeded threshold! Sent OFF command.");
        }
    }

    private void updateDeviceStatusInDb(int deviceId, boolean status) {
        try {
            String url = "/devices?id=eq." + deviceId;
            Map<String, Object> body = new HashMap<>();
            body.put("status", status);

            HttpHeaders headers = supabaseHeaders();
            headers.set("Prefer", "return=minimal");
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
            
            restTemplate.exchange(URL + url, HttpMethod.PATCH, entity, String.class);
        } catch (Exception e) {
            System.err.println("❌ Failed to update device status in DB: " + e.getMessage());
        }
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

    // Get device IDs for a specific user
    private List<Integer> getUserDeviceIds(Integer userId) {
        if (userId == null) return new ArrayList<>();
        String q = String.format("/%s?select=device_id&user_id=eq.%d", tableUserDevices, userId);
        List<Map<String, Object>> rows = getList(q);
        return rows.stream()
                .map(row -> toInt(row.get("device_id")))
                .filter(id -> id != null)
                .collect(Collectors.toList());
    }

    /**
     * TASK 3: Phát hiện thiết bị offline cho user cụ thể.
     * - Kiểm tra sensor_data mới nhất của từng device
     * - Nếu không có dữ liệu trong N giây gần đây => offline
     * - Nếu chưa có alert OFFLINE chưa đọc => tạo alert
     */
    public OfflineCheckResultDto checkOfflineDevicesForUser(int seconds, Integer userId) {
        int thresholdSeconds = Math.max(seconds, 1);
        Instant now = Instant.now();
        Instant thresholdTime = now.minus(Duration.ofSeconds(thresholdSeconds));

        warnings.clear();
        List<Integer> userDeviceIds = getUserDeviceIds(userId);
        if (userDeviceIds.isEmpty()) {
            OfflineCheckResultDto result = new OfflineCheckResultDto();
            result.setThreshold_seconds(thresholdSeconds);
            result.setOffline_devices(new ArrayList<>());
            result.setCreated_alerts(0);
            return result;
        }

        List<OfflineDeviceDto> offline = new ArrayList<>();
        int createdAlerts = 0;

        for (Integer deviceId : userDeviceIds) {
            // Query sensor_data mới nhất của device này
            String q = String.format(
                    "/%s?select=device_id,created_at&device_id=eq.%d&created_at=gte.%s&order=created_at.desc&limit=1",
                    TABLE_SENSOR_DATA, deviceId, thresholdTime.toString()
            );
            List<Map<String, Object>> recentData = getList(q);

            boolean isOffline = recentData.isEmpty(); // Không có dữ liệu trong khoảng thời gian => offline

            if (isOffline) {
                // Tìm thời điểm gửi dữ liệu cuối cùng (nếu có)
                String lastDataQuery = String.format(
                        "/%s?select=device_id,created_at&device_id=eq.%d&order=created_at.desc&limit=1",
                        TABLE_SENSOR_DATA, deviceId
                );
                List<Map<String, Object>> lastData = getList(lastDataQuery);

                String lastSeen = null;
                long secondsSinceLastSeen = -1;

                if (!lastData.isEmpty()) {
                    Object createdAtRaw = lastData.get(0).get("created_at");
                    if (createdAtRaw != null) {
                        LocalDateTime lastSeenAt = parseSupabaseTimeToLocalDateTime(createdAtRaw);
                        if (lastSeenAt != null) {
                            lastSeen = createdAtRaw.toString();
                            secondsSinceLastSeen = Duration.between(lastSeenAt.toInstant(ZoneOffset.UTC), now).toSeconds();
                        }
                    }
                }

                OfflineDeviceDto item = new OfflineDeviceDto();
                item.setDevice_id(deviceId);
                item.setLast_seen(lastSeen);
                item.setSeconds_since_last_seen(secondsSinceLastSeen);
                item.setMinutes_since_last_seen(secondsSinceLastSeen >= 0 ? secondsSinceLastSeen / 60 : -1);
                offline.add(item);

                if (!hasUnreadAlert(deviceId, "OFFLINE")) {
                    String message = secondsSinceLastSeen >= 0
                        ? "Thiết bị offline (quá " + thresholdSeconds + " giây không gửi dữ liệu, lần cuối " + secondsSinceLastSeen + " giây trước)."
                        : "Thiết bị offline (chưa từng gửi dữ liệu).";
                    createAlert(deviceId, userId, "OFFLINE", message);
                    createdAlerts++;
                }
            }
        }

        OfflineCheckResultDto result = new OfflineCheckResultDto();
        result.setThreshold_seconds(thresholdSeconds);
        result.setOffline_devices(offline);
        result.setCreated_alerts(createdAlerts);
        if (!warnings.isEmpty()) result.setWarnings(new ArrayList<>(warnings));
        return result;
    }

    /**
     * TASK 4: Tạo cảnh báo vượt ngưỡng cho user cụ thể.
     * - Lấy device_limit của user
     * - Với mỗi device => lấy bản ghi sensor_data mới nhất
     * - Nếu power/current vượt ngưỡng => tạo alert (POWER_OVER / CURRENT_OVER)
     */
    public ThresholdCheckResultDto checkThresholdAlertsForUser(Integer userId) {
        warnings.clear();
        List<Integer> userDeviceIds = getUserDeviceIds(userId);
        if (userDeviceIds.isEmpty()) {
            ThresholdCheckResultDto result = new ThresholdCheckResultDto();
            result.setOver_limit_devices(new ArrayList<>());
            result.setCreated_alerts(0);
            return result;
        }

        List<Map<String, Object>> rawLimits = getListWithFallback(
                tableDeviceLimit,
                new String[] { "device_limits" },
                "?select=device_id,max_power,max_current"
        );

        // Filter only user's device limits
        List<Map<String, Object>> userLimits = rawLimits.stream()
                .filter(row -> {
                    Integer deviceId = toInt(row.get("device_id"));
                    return deviceId != null && userDeviceIds.contains(deviceId);
                })
                .collect(Collectors.toList());

        List<OverLimitDeviceDto> over = new ArrayList<>();
        int createdAlerts = 0;

        for (Map<String, Object> lim : userLimits) {
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

            // Tự động ngắt thiết bị khi vượt ngưỡng
            ducthinhService.controlDevice(deviceId, "OFF");

            OverLimitDeviceDto item = new OverLimitDeviceDto();
            item.setDevice_id(deviceId);
            item.setLatest(latest);
            item.setLimit(limit);
            item.setPower_over(powerOver);
            item.setCurrent_over(currentOver);
            over.add(item);

            if (powerOver && !hasUnreadAlert(deviceId, "POWER_EXCEEDED")) {
                createAlert(deviceId, userId, "POWER_EXCEEDED",
                        "Vượt ngưỡng công suất: " + power + " > " + maxPower);
                createdAlerts++;
            }
            if (currentOver && !hasUnreadAlert(deviceId, "CURRENT_EXCEEDED")) {
                createAlert(deviceId, userId, "CURRENT_EXCEEDED",
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

    public List<Alert> getLatestAlertsForUser(int limit, Integer userId) {
        int safeLimit = Math.min(Math.max(limit, 1), 200);
        warnings.clear();
        List<Integer> userDeviceIds = getUserDeviceIds(userId);
        if (userDeviceIds.isEmpty()) return new ArrayList<>();

        // Build query to get alerts for user's devices
        String deviceIds = userDeviceIds.stream()
                .map(String::valueOf)
                .collect(Collectors.joining(","));
        String q = String.format("?select=id,device_id,type,message,is_read&device_id=in.(%s)&order=id.desc&limit=%d",
                deviceIds, safeLimit);
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