package com.Iot.backend.service;

import com.Iot.backend.dto.AlertResponse;
import com.Iot.backend.dto.DeviceResponse;
import com.Iot.backend.dto.MonitorFiltersResponse;
import com.Iot.backend.dto.MonitorHistoryResponse;
import com.Iot.backend.dto.MonitorHistoryRowResponse;
import com.Iot.backend.dto.MonitorSummaryResponse;
import com.Iot.backend.model.Alert;
import com.Iot.backend.model.Device;
import com.Iot.backend.model.DeviceLimit;
import com.Iot.backend.model.SensorData;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;

@Service
public class xuandat {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final RestTemplate restTemplate;
    private final String baseUrl;
    private final String apiKey;

    public xuandat(
            RestTemplate restTemplate,
            @Value("${supabase.url}") String baseUrl,
            @Value("${supabase.api-key:}") String apiKey) {
        this.restTemplate = restTemplate;
        this.baseUrl = baseUrl;
        this.apiKey = apiKey;
    }

    public List<Map<String, Object>> getUsers(Integer requesterUserId, String requesterRole) {
        ScopeContext scope = resolveScope(requesterUserId, requesterRole, null, null);
        if ("admin".equals(scope.requesterRole())) {
            return fetchUsers(null).stream()
                    .map(this::toUserSummary)
                    .toList();
        }

        if (scope.scopedUserId() == null) {
            return List.of();
        }

        Map<String, Object> currentUser = fetchUser(scope.scopedUserId());
        return currentUser == null ? List.of() : List.of(toUserSummary(currentUser));
    }

    public List<DeviceResponse> getDevices(Integer requesterUserId, String requesterRole, Integer targetUserId) {
        ScopeContext scope = resolveScope(requesterUserId, requesterRole, targetUserId, null);
        return scope.devices().stream()
                .map(device -> new DeviceResponse(
                        device.getId(),
                        device.getName(),
                        device.getLocation(),
                        device.getStatus()))
                .toList();
    }

    public MonitorHistoryResponse getHistory(
            Integer requesterUserId,
            String requesterRole,
            Integer targetUserId,
            Integer deviceId,
            LocalDateTime from,
            LocalDateTime to,
            String bucket) {
        ScopeContext scope = resolveScope(requesterUserId, requesterRole, targetUserId, deviceId);
        MonitorRequest request = normalizeRequest(deviceId, from, to, bucket);
        DashboardSnapshot snapshot = loadSnapshot(request, scope);
        List<MonitorHistoryRowResponse> rows = buildHistoryRows(snapshot, request.bucket());

        int breachRows = (int) rows.stream().filter(MonitorHistoryRowResponse::isThresholdBreached).count();
        String latestTimestamp = rows.isEmpty() ? null : rows.get(rows.size() - 1).createdAt();
        double totalEnergy = rows.stream().mapToDouble(row -> safeNumber(row.energy())).sum();

        return new MonitorHistoryResponse(
                resolveDeviceResponse(request.deviceId(), scope.devicesById()),
                new MonitorFiltersResponse(
                        request.deviceId(),
                        formatTimestamp(request.from()),
                        formatTimestamp(request.to()),
                        request.bucket()),
                rows,
                new MonitorSummaryResponse(
                        rows.size(),
                        snapshot.alerts().size(),
                        breachRows,
                        round(totalEnergy),
                        latestTimestamp));
    }

    public List<AlertResponse> getAlerts(
            Integer requesterUserId,
            String requesterRole,
            Integer targetUserId,
            Integer deviceId,
            LocalDateTime from,
            LocalDateTime to) {
        ScopeContext scope = resolveScope(requesterUserId, requesterRole, targetUserId, deviceId);
        MonitorRequest request = normalizeRequest(deviceId, from, to, "raw");
        return fetchAlerts(request.deviceId(), request.from(), request.to(), scope.accessibleDeviceIds()).stream()
                .map(this::toAlertResponse)
                .toList();
    }

    public ResponseEntity<byte[]> exportExcel(
            Integer requesterUserId,
            String requesterRole,
            Integer targetUserId,
            Integer deviceId,
            LocalDateTime from,
            LocalDateTime to,
            String bucket) {
        ScopeContext scope = resolveScope(requesterUserId, requesterRole, targetUserId, deviceId);
        MonitorRequest request = normalizeRequest(deviceId, from, to, bucket);
        DashboardSnapshot snapshot = loadSnapshot(request, scope);
        List<MonitorHistoryRowResponse> rows = buildHistoryRows(snapshot, request.bucket());
        byte[] file = buildSpreadsheetXml(rows, snapshot.alerts()).getBytes(StandardCharsets.UTF_8);

        String filename = buildExportFilename(scope, request);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType("application/vnd.ms-excel"))
                .body(file);
    }

    private DashboardSnapshot loadSnapshot(MonitorRequest request, ScopeContext scope) {
        Map<Integer, Device> devicesById = new HashMap<>(scope.devicesById());
        if (scope.accessibleDeviceIds().isEmpty()) {
            return new DashboardSnapshot(devicesById, List.of(), List.of(), Map.of());
        }

        List<SensorData> sensorRows = fetchSensorData(request, scope.accessibleDeviceIds());
        sensorRows.sort(Comparator.comparing(SensorData::getCreated_at, Comparator.nullsLast(Comparator.naturalOrder())));

        Map<Integer, DeviceLimit> limitsByDeviceId = fetchLatestLimits(request.deviceId(), scope.accessibleDeviceIds());
        syncThresholdAlerts(sensorRows, limitsByDeviceId, devicesById, request, scope.accessibleDeviceIds());
        List<Alert> alerts = fetchAlerts(request.deviceId(), request.from(), request.to(), scope.accessibleDeviceIds());

        return new DashboardSnapshot(devicesById, sensorRows, alerts, limitsByDeviceId);
    }

    private List<Device> fetchAllDevices() {
        MultiValueMap<String, String> query = new LinkedMultiValueMap<>();
        query.add("select", "id,name,location,status");
        query.add("order", "name.asc");
        return getList("devices", query, Device[].class);
    }

    private List<Device> fetchDevicesByIds(Collection<Integer> deviceIds) {
        if (deviceIds == null || deviceIds.isEmpty()) {
            return List.of();
        }

        MultiValueMap<String, String> query = new LinkedMultiValueMap<>();
        query.add("select", "id,name,location,status");
        query.add("order", "name.asc");
        query.add("id", "in.(" + joinNumbers(deviceIds) + ")");
        return getList("devices", query, Device[].class);
    }

    private List<SensorData> fetchSensorData(MonitorRequest request, Set<Integer> accessibleDeviceIds) {
        if (accessibleDeviceIds.isEmpty()) {
            return List.of();
        }

        MultiValueMap<String, String> query = new LinkedMultiValueMap<>();
        query.add("select", "id,device_id,voltage,current,power,energy,created_at");
        query.add("order", "created_at.asc");
        query.add("created_at", "gte." + formatSupabaseTimestamp(request.from()));
        query.add("created_at", "lte." + formatSupabaseTimestamp(request.to()));
        applyDeviceFilter(query, request.deviceId(), accessibleDeviceIds);
        return getList("sensor_data", query, SensorData[].class);
    }

    private Map<Integer, DeviceLimit> fetchLatestLimits(Integer deviceId, Set<Integer> accessibleDeviceIds) {
        if (accessibleDeviceIds.isEmpty()) {
            return Map.of();
        }

        MultiValueMap<String, String> query = new LinkedMultiValueMap<>();
        query.add("select", "id,device_id,max_power,max_current,created_at");
        query.add("order", "created_at.desc");
        applyDeviceFilter(query, deviceId, accessibleDeviceIds);

        Map<Integer, DeviceLimit> limitsByDeviceId = new HashMap<>();
        for (DeviceLimit limit : getList("device_limits", query, DeviceLimit[].class)) {
            if (limit.getDevice_id() != null) {
                limitsByDeviceId.putIfAbsent(limit.getDevice_id(), limit);
            }
        }
        return limitsByDeviceId;
    }

    private List<Alert> fetchAlerts(Integer deviceId, LocalDateTime from, LocalDateTime to, Set<Integer> accessibleDeviceIds) {
        if (accessibleDeviceIds.isEmpty()) {
            return List.of();
        }

        MultiValueMap<String, String> query = new LinkedMultiValueMap<>();
        query.add("select", "id,device_id,type,message,created_at,is_read");
        query.add("order", "created_at.desc");
        query.add("created_at", "gte." + formatSupabaseTimestamp(from));
        query.add("created_at", "lte." + formatSupabaseTimestamp(to));
        applyDeviceFilter(query, deviceId, accessibleDeviceIds);
        return getList("alerts", query, Alert[].class);
    }

    private void syncThresholdAlerts(
            List<SensorData> sensorRows,
            Map<Integer, DeviceLimit> limitsByDeviceId,
            Map<Integer, Device> devicesById,
            MonitorRequest request,
            Set<Integer> accessibleDeviceIds) {
        if (sensorRows.isEmpty()) {
            return;
        }

        List<Alert> existingAlerts = fetchAlerts(request.deviceId(), request.from(), request.to(), accessibleDeviceIds);
        Set<String> alertKeys = new LinkedHashSet<>();
        for (Alert alert : existingAlerts) {
            if (alert.getDevice_id() == null || alert.getCreated_at() == null || alert.getType() == null) {
                continue;
            }
            alertKeys.add(buildAlertKey(alert.getDevice_id(), alert.getCreated_at(), alert.getType()));
        }

        for (SensorData sensorData : sensorRows) {
            if (sensorData.getDevice_id() == null || sensorData.getCreated_at() == null) {
                continue;
            }

            DeviceLimit limit = limitsByDeviceId.get(sensorData.getDevice_id());
            if (limit == null) {
                continue;
            }

            Device device = devicesById.get(sensorData.getDevice_id());
            for (String breachType : getBreachTypes(sensorData, limit)) {
                String key = buildAlertKey(sensorData.getDevice_id(), sensorData.getCreated_at(), breachType);
                if (alertKeys.contains(key)) {
                    continue;
                }

                Map<String, Object> payload = new LinkedHashMap<>();
                payload.put("device_id", sensorData.getDevice_id());
                payload.put("type", breachType);
                payload.put("message", buildAlertMessage(device, sensorData, limit, breachType));
                payload.put("created_at", sensorData.getCreated_at().toString());
                payload.put("is_read", false);
                post("alerts", payload);
                alertKeys.add(key);
            }
        }
    }

    private <T> List<T> getList(String table, MultiValueMap<String, String> queryParams, Class<T[]> responseType) {
        ensureConfigured();

        URI uri = buildUri(table, queryParams);
        HttpEntity<Void> entity = new HttpEntity<>(buildHeaders());
        ResponseEntity<T[]> response = restTemplate.exchange(uri, HttpMethod.GET, entity, responseType);
        T[] body = response.getBody();
        return body == null ? List.of() : Arrays.asList(body);
    }

    private List<Map<String, Object>> getListOfMaps(String table, MultiValueMap<String, String> queryParams) {
        ensureConfigured();

        URI uri = buildUri(table, queryParams);
        HttpEntity<Void> entity = new HttpEntity<>(buildHeaders());
        ResponseEntity<List<Map<String, Object>>> response = restTemplate.exchange(
                uri,
                HttpMethod.GET,
                entity,
                new ParameterizedTypeReference<List<Map<String, Object>>>() {
                });
        List<Map<String, Object>> body = response.getBody();
        return body == null ? List.of() : body;
    }

    private void post(String table, Object payload) {
        ensureConfigured();

        HttpHeaders headers = buildHeaders();
        headers.set("Prefer", "return=minimal");
        HttpEntity<Object> entity = new HttpEntity<>(payload, headers);
        restTemplate.exchange(buildUri(table, null), HttpMethod.POST, entity, String.class);
    }

    private URI buildUri(String table, MultiValueMap<String, String> queryParams) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(baseUrl).pathSegment(table);
        if (queryParams != null) {
            queryParams.forEach((key, values) -> values.forEach(value -> builder.queryParam(key, value)));
        }
        return builder.build().encode().toUri();
    }

    private HttpHeaders buildHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("apikey", apiKey);
        headers.setBearerAuth(apiKey);
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        return headers;
    }

    private void ensureConfigured() {
        if (apiKey == null || apiKey.isBlank()) {
            throw new ResponseStatusException(
                    INTERNAL_SERVER_ERROR,
                    "Thieu SUPABASE_API_KEY. Hay cau hinh bien moi truong truoc khi chay backend.");
        }
    }

    private List<MonitorHistoryRowResponse> buildHistoryRows(DashboardSnapshot snapshot, String bucket) {
        if ("raw".equals(bucket)) {
            return snapshot.sensorRows().stream()
                    .map(row -> toHistoryRow(row, snapshot.devicesById(), snapshot.limitsByDeviceId()))
                    .toList();
        }

        Map<String, AggregatedHistoryRow> grouped = new LinkedHashMap<>();
        for (SensorData row : snapshot.sensorRows()) {
            if (row.getCreated_at() == null) {
                continue;
            }

            Device device = snapshot.devicesById().get(row.getDevice_id());
            String deviceName = device != null && device.getName() != null
                    ? device.getName()
                    : "Thiet bi " + row.getDevice_id();
            List<String> breachTypes = getBreachTypes(row, snapshot.limitsByDeviceId().get(row.getDevice_id()));

            LocalDateTime bucketTime = truncate(row.getCreated_at(), bucket);
            String groupKey = bucketTime.toString();
            grouped.computeIfAbsent(groupKey, ignored -> new AggregatedHistoryRow(bucketTime))
                    .add(row, deviceName, breachTypes);
        }

        return grouped.values().stream()
                .map(AggregatedHistoryRow::toResponse)
                .toList();
    }

    private MonitorHistoryRowResponse toHistoryRow(
            SensorData row,
            Map<Integer, Device> devicesById,
            Map<Integer, DeviceLimit> limitsByDeviceId) {
        List<String> breachTypes = getBreachTypes(row, limitsByDeviceId.get(row.getDevice_id()));
        Device device = devicesById.get(row.getDevice_id());
        return new MonitorHistoryRowResponse(
                formatTimestamp(row.getCreated_at()),
                row.getDevice_id(),
                device != null && device.getName() != null ? device.getName() : "Thiet bi " + row.getDevice_id(),
                round(row.getVoltage()),
                round(row.getCurrent()),
                round(row.getPower()),
                round(row.getEnergy()),
                !breachTypes.isEmpty(),
                breachTypes);
    }

    private AlertResponse toAlertResponse(Alert alert) {
        return new AlertResponse(
                alert.getId(),
                alert.getDevice_id(),
                alert.getType(),
                alert.getMessage(),
                formatTimestamp(alert.getCreated_at()),
                alert.getIs_read());
    }

    private DeviceResponse resolveDeviceResponse(Integer deviceId, Map<Integer, Device> devicesById) {
        if (deviceId == null) {
            return new DeviceResponse(null, "Tat ca thiet bi", null, null);
        }

        Device device = devicesById.get(deviceId);
        if (device == null) {
            return new DeviceResponse(deviceId, "Thiet bi " + deviceId, null, null);
        }

        return new DeviceResponse(device.getId(), device.getName(), device.getLocation(), device.getStatus());
    }

    private MonitorRequest normalizeRequest(Integer deviceId, LocalDateTime from, LocalDateTime to, String bucket) {
        LocalDateTime resolvedTo = to == null ? LocalDateTime.now() : to;
        LocalDateTime resolvedFrom = from == null ? resolvedTo.minusHours(24) : from;
        if (resolvedFrom.isAfter(resolvedTo)) {
            throw new ResponseStatusException(BAD_REQUEST, "Thoi gian bat dau phai nho hon hoac bang thoi gian ket thuc.");
        }

        String resolvedBucket = bucket == null || bucket.isBlank() ? "raw" : bucket.toLowerCase();
        if (!Set.of("raw", "hour", "day").contains(resolvedBucket)) {
            throw new ResponseStatusException(BAD_REQUEST, "bucket chi ho tro raw, hour hoac day.");
        }

        ZoneId zoneId = ZoneId.systemDefault();
        return new MonitorRequest(
                deviceId,
                resolvedFrom,
                resolvedTo,
                resolvedBucket);
    }

    private LocalDateTime truncate(LocalDateTime timestamp, String bucket) {
        if ("day".equals(bucket)) {
            return timestamp.toLocalDate().atStartOfDay();
        }
        if ("hour".equals(bucket)) {
            return timestamp.withMinute(0).withSecond(0).withNano(0);
        }
        return timestamp;
    }

    private List<String> getBreachTypes(SensorData row, DeviceLimit limit) {
        if (limit == null) {
            return List.of();
        }

        List<String> types = new ArrayList<>();
        if (row.getPower() != null && limit.getMax_power() != null && row.getPower() > limit.getMax_power()) {
            types.add("POWER_LIMIT_EXCEEDED");
        }
        if (row.getCurrent() != null && limit.getMax_current() != null && row.getCurrent() > limit.getMax_current()) {
            types.add("CURRENT_LIMIT_EXCEEDED");
        }
        return types;
    }

    private String buildAlertKey(Integer deviceId, LocalDateTime createdAt, String type) {
        return deviceId + "|" + createdAt + "|" + type;
    }

    private String buildAlertMessage(Device device, SensorData sensorData, DeviceLimit limit, String breachType) {
        String deviceName = device != null && device.getName() != null
                ? device.getName()
                : "Thiet bi " + sensorData.getDevice_id();
        if ("POWER_LIMIT_EXCEEDED".equals(breachType)) {
            return String.format(
                    "%s vuot nguong cong suat: %.2fW / %.2fW",
                    deviceName,
                    safeNumber(sensorData.getPower()),
                    safeNumber(limit.getMax_power()));
        }
        return String.format(
                "%s vuot nguong dong dien: %.2fA / %.2fA",
                deviceName,
                safeNumber(sensorData.getCurrent()),
                safeNumber(limit.getMax_current()));
    }

    private String buildSpreadsheetXml(List<MonitorHistoryRowResponse> historyRows, List<Alert> alerts) {
        StringBuilder xml = new StringBuilder();
        xml.append("<?xml version=\"1.0\"?>")
                .append("<Workbook xmlns=\"urn:schemas-microsoft-com:office:spreadsheet\" ")
                .append("xmlns:ss=\"urn:schemas-microsoft-com:office:spreadsheet\">");

        xml.append("<Worksheet ss:Name=\"History\"><Table>");
        appendHeaderRow(xml, List.of("Thoi gian", "Thiet bi", "Dien ap (V)", "Dong dien (A)", "Cong suat (W)", "Energy", "Vuot nguong", "Loai canh bao"));
        for (MonitorHistoryRowResponse row : historyRows) {
            appendDataRow(xml, List.of(
                    row.createdAt(),
                    row.deviceName(),
                    stringValue(row.voltage()),
                    stringValue(row.current()),
                    stringValue(row.power()),
                    stringValue(row.energy()),
                    row.isThresholdBreached() ? "Co" : "Khong",
                    String.join(", ", row.breachTypes())));
        }
        xml.append("</Table></Worksheet>");

        xml.append("<Worksheet ss:Name=\"Alerts\"><Table>");
        appendHeaderRow(xml, List.of("ID", "Thiet bi", "Loai", "Noi dung", "Thoi gian", "Da doc"));
        for (Alert alert : alerts) {
            appendDataRow(xml, List.of(
                    stringValue(alert.getId()),
                    stringValue(alert.getDevice_id()),
                    alert.getType(),
                    alert.getMessage(),
                    formatTimestamp(alert.getCreated_at()),
                    Boolean.TRUE.equals(alert.getIs_read()) ? "Co" : "Chua"));
        }
        xml.append("</Table></Worksheet>");

        xml.append("</Workbook>");
        return xml.toString();
    }

    private void appendHeaderRow(StringBuilder xml, Collection<String> values) {
        xml.append("<Row>");
        for (String value : values) {
            xml.append("<Cell><Data ss:Type=\"String\">")
                    .append(escapeXml(value))
                    .append("</Data></Cell>");
        }
        xml.append("</Row>");
    }

    private void appendDataRow(StringBuilder xml, Collection<String> values) {
        xml.append("<Row>");
        for (String value : values) {
            xml.append("<Cell><Data ss:Type=\"String\">")
                    .append(escapeXml(value))
                    .append("</Data></Cell>");
        }
        xml.append("</Row>");
    }

    private String escapeXml(String input) {
        if (input == null) {
            return "";
        }
        return input
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }

    private String formatTimestamp(LocalDateTime value) {
        if (value == null) {
            return null;
        }
        return value.format(DATE_TIME_FORMATTER);
    }

    private String formatSupabaseTimestamp(LocalDateTime value) {
        if (value == null) {
            return null;
        }
        return value.toString();
    }

    private double safeNumber(Number value) {
        return value == null ? 0.0 : value.doubleValue();
    }

    private Double round(Number value) {
        return Math.round(safeNumber(value) * 100.0) / 100.0;
    }

    private String stringValue(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private ScopeContext resolveScope(
            Integer requesterUserId,
            String requesterRole,
            Integer targetUserId,
            Integer requestedDeviceId) {
        String normalizedRole = normalizeRole(requesterUserId, requesterRole);
        Integer scopedUserId = resolveScopedUserId(normalizedRole, requesterUserId, targetUserId);
        List<Device> scopedDevices = scopedUserId == null
                ? fetchAllDevices()
                : fetchDevicesByIds(fetchDeviceIdsByUser(scopedUserId));

        Map<Integer, Device> devicesById = new HashMap<>();
        for (Device device : scopedDevices) {
            devicesById.put(device.getId(), device);
        }

        if (requestedDeviceId != null && !devicesById.containsKey(requestedDeviceId)) {
            throw new ResponseStatusException(FORBIDDEN, "Ban khong co quyen xem thiet bi nay.");
        }

        return new ScopeContext(
                normalizedRole,
                requesterUserId,
                scopedUserId,
                scopedDevices,
                devicesById,
                new LinkedHashSet<>(devicesById.keySet()),
                resolveUserDisplayName(scopedUserId));
    }

    private String normalizeRole(Integer requesterUserId, String requesterRole) {
        if (requesterRole == null || requesterRole.isBlank()) {
            return requesterUserId == null ? "admin" : "user";
        }
        return "admin".equalsIgnoreCase(requesterRole.trim()) ? "admin" : "user";
    }

    private Integer resolveScopedUserId(String requesterRole, Integer requesterUserId, Integer targetUserId) {
        if ("admin".equals(requesterRole)) {
            return targetUserId;
        }

        if (requesterUserId == null) {
            throw new ResponseStatusException(BAD_REQUEST, "Thieu requesterUserId cho tai khoan user.");
        }

        if (targetUserId != null && !Objects.equals(targetUserId, requesterUserId)) {
            throw new ResponseStatusException(FORBIDDEN, "Ban khong co quyen xem du lieu cua nguoi dung khac.");
        }

        return requesterUserId;
    }

    private List<Map<String, Object>> fetchUsers(Integer userId) {
        MultiValueMap<String, String> query = new LinkedMultiValueMap<>();
        query.add("select", "id,username,email,role");
        query.add("order", "username.asc");
        if (userId != null) {
            query.add("id", "eq." + userId);
        }
        return getListOfMaps("users", query);
    }

    private Map<String, Object> fetchUser(Integer userId) {
        if (userId == null) {
            return null;
        }
        List<Map<String, Object>> users = fetchUsers(userId);
        return users.isEmpty() ? null : users.get(0);
    }

    private Map<String, Object> toUserSummary(Map<String, Object> user) {
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("id", toInteger(user.get("id")));
        summary.put("username", stringValue(user.get("username")));
        summary.put("email", stringValue(user.get("email")));
        summary.put("role", stringValue(user.get("role")));
        return summary;
    }

    private List<Integer> fetchDeviceIdsByUser(Integer userId) {
        if (userId == null) {
            return List.of();
        }

        MultiValueMap<String, String> query = new LinkedMultiValueMap<>();
        query.add("select", "device_id");
        query.add("user_id", "eq." + userId);

        return getListOfMaps("user_devices", query).stream()
                .map(row -> toInteger(row.get("device_id")))
                .filter(Objects::nonNull)
                .distinct()
                .toList();
    }

    private void applyDeviceFilter(
            MultiValueMap<String, String> query,
            Integer requestedDeviceId,
            Set<Integer> accessibleDeviceIds) {
        if (requestedDeviceId != null) {
            query.add("device_id", "eq." + requestedDeviceId);
            return;
        }

        if (!accessibleDeviceIds.isEmpty()) {
            query.add("device_id", "in.(" + joinNumbers(accessibleDeviceIds) + ")");
        }
    }

    private String joinNumbers(Collection<Integer> values) {
        return values.stream()
                .filter(Objects::nonNull)
                .map(String::valueOf)
                .sorted()
                .reduce((left, right) -> left + "," + right)
                .orElse("");
    }

    private Integer toInteger(Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value == null) {
            return null;
        }
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private String resolveUserDisplayName(Integer userId) {
        if (userId == null) {
            return null;
        }
        Map<String, Object> user = fetchUser(userId);
        if (user == null) {
            return null;
        }
        String username = stringValue(user.get("username"));
        return username.isBlank() ? "user-" + userId : username;
    }

    private String buildExportFilename(ScopeContext scope, MonitorRequest request) {
        StringBuilder filename = new StringBuilder("xuandat-report");
        if (scope.scopedUserId() != null) {
            filename.append("-user-").append(sanitizeFilenamePart(scope.scopedUserName() == null
                    ? String.valueOf(scope.scopedUserId())
                    : scope.scopedUserName()));
        }
        filename.append("-").append(request.from().toLocalDate());
        filename.append("-to-").append(request.to().toLocalDate());
        filename.append(".xls");
        return filename.toString();
    }

    private String sanitizeFilenamePart(String value) {
        return stringValue(value)
                .trim()
                .replaceAll("[^a-zA-Z0-9_-]+", "-")
                .replaceAll("-{2,}", "-")
                .replaceAll("^-|-$", "");
    }

    private record MonitorRequest(
            Integer deviceId,
            LocalDateTime from,
            LocalDateTime to,
            String bucket) {
    }

    private record ScopeContext(
            String requesterRole,
            Integer requesterUserId,
            Integer scopedUserId,
            List<Device> devices,
            Map<Integer, Device> devicesById,
            Set<Integer> accessibleDeviceIds,
            String scopedUserName) {
    }

    private record DashboardSnapshot(
            Map<Integer, Device> devicesById,
            List<SensorData> sensorRows,
            List<Alert> alerts,
            Map<Integer, DeviceLimit> limitsByDeviceId) {
    }

    private static final class AggregatedHistoryRow {
        private final LocalDateTime bucketTime;
        private final Set<String> breachTypes = new LinkedHashSet<>();
        private double voltageSum;
        private double currentSum;
        private double powerSum;
        private double energySum;
        private int count;
        private Integer deviceId;
        private String deviceName;

        private AggregatedHistoryRow(LocalDateTime bucketTime) {
            this.bucketTime = bucketTime;
        }

        private void add(SensorData row, String resolvedDeviceName, List<String> resolvedBreachTypes) {
            voltageSum += row.getVoltage() == null ? 0.0 : row.getVoltage();
            currentSum += row.getCurrent() == null ? 0.0 : row.getCurrent();
            powerSum += row.getPower() == null ? 0.0 : row.getPower();
            energySum += row.getEnergy() == null ? 0.0 : row.getEnergy();

            if (count == 0) {
                deviceId = row.getDevice_id();
                deviceName = resolvedDeviceName;
            } else if (!Objects.equals(deviceId, row.getDevice_id())) {
                deviceId = null;
                deviceName = "Tat ca thiet bi";
            }

            breachTypes.addAll(resolvedBreachTypes);
            count++;
        }

        private MonitorHistoryRowResponse toResponse() {
            return new MonitorHistoryRowResponse(
                    bucketTime.format(DATE_TIME_FORMATTER),
                    deviceId,
                    deviceName == null ? "Tat ca thiet bi" : deviceName,
                    roundAverage(voltageSum, count),
                    roundAverage(currentSum, count),
                    roundAverage(powerSum, count),
                    roundTotal(energySum),
                    !breachTypes.isEmpty(),
                    new ArrayList<>(breachTypes));
        }

        private double roundAverage(double total, int divisor) {
            double value = divisor <= 0 ? 0.0 : total / divisor;
            return Math.round(value * 100.0) / 100.0;
        }

        private double roundTotal(double value) {
            return Math.round(value * 100.0) / 100.0;
        }
    }
}


