package com.example.demo.service;

import com.example.demo.dto.AlertResponse;
import com.example.demo.dto.DeviceResponse;
import com.example.demo.dto.MonitorFiltersResponse;
import com.example.demo.dto.MonitorHistoryResponse;
import com.example.demo.dto.MonitorHistoryRowResponse;
import com.example.demo.dto.MonitorSummaryResponse;
import com.example.demo.model.Alert;
import com.example.demo.model.Device;
import com.example.demo.model.DeviceLimit;
import com.example.demo.model.SensorData;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
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

@Service
public class xuandat {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final SupabaseGatewayService supabaseGatewayService;

    public xuandat(SupabaseGatewayService supabaseGatewayService) {
        this.supabaseGatewayService = supabaseGatewayService;
    }

    public List<DeviceResponse> getDevices() {
        MultiValueMap<String, String> query = new LinkedMultiValueMap<>();
        query.add("select", "id,name,location,status");
        query.add("order", "name.asc");

        return supabaseGatewayService.getList("devices", query, Device[].class).stream()
                .map(device -> new DeviceResponse(
                        device.getId(),
                        device.getName(),
                        device.getLocation(),
                        device.getStatus()))
                .toList();
    }

    public MonitorHistoryResponse getHistory(Integer deviceId, LocalDateTime from, LocalDateTime to, String bucket) {
        MonitorRequest request = normalizeRequest(deviceId, from, to, bucket);
        DashboardSnapshot snapshot = loadSnapshot(request);
        List<MonitorHistoryRowResponse> rows = buildHistoryRows(snapshot, request.bucket());

        int breachRows = (int) rows.stream().filter(MonitorHistoryRowResponse::isThresholdBreached).count();
        String latestTimestamp = rows.isEmpty() ? null : rows.get(rows.size() - 1).createdAt();
        double totalEnergy = rows.stream().mapToDouble(row -> safeNumber(row.energy())).sum();

        return new MonitorHistoryResponse(
                resolveDeviceResponse(request.deviceId(), snapshot.devicesById()),
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

    public List<AlertResponse> getAlerts(Integer deviceId, LocalDateTime from, LocalDateTime to) {
        MonitorRequest request = normalizeRequest(deviceId, from, to, "raw");
        return loadSnapshot(request).alerts().stream()
                .map(this::toAlertResponse)
                .toList();
    }

    public ResponseEntity<byte[]> exportExcel(Integer deviceId, LocalDateTime from, LocalDateTime to, String bucket) {
        MonitorRequest request = normalizeRequest(deviceId, from, to, bucket);
        DashboardSnapshot snapshot = loadSnapshot(request);
        List<MonitorHistoryRowResponse> rows = buildHistoryRows(snapshot, request.bucket());
        byte[] file = buildSpreadsheetXml(rows, snapshot.alerts()).getBytes(StandardCharsets.UTF_8);

        String filename = "xuandat-report-" + request.from().toLocalDate() + "-to-" + request.to().toLocalDate() + ".xls";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType("application/vnd.ms-excel"))
                .body(file);
    }

    private DashboardSnapshot loadSnapshot(MonitorRequest request) {
        List<Device> devices = fetchDevices();
        Map<Integer, Device> devicesById = new HashMap<>();
        for (Device device : devices) {
            devicesById.put(device.getId(), device);
        }

        List<SensorData> sensorRows = fetchSensorData(request);
        sensorRows.sort(Comparator.comparing(SensorData::getCreated_at, Comparator.nullsLast(Comparator.naturalOrder())));

        Map<Integer, DeviceLimit> limitsByDeviceId = fetchLatestLimits(request.deviceId());
        syncThresholdAlerts(sensorRows, limitsByDeviceId, devicesById, request);
        List<Alert> alerts = fetchAlerts(request.deviceId(), request.from(), request.to());

        return new DashboardSnapshot(devicesById, sensorRows, alerts, limitsByDeviceId);
    }

    private List<Device> fetchDevices() {
        MultiValueMap<String, String> query = new LinkedMultiValueMap<>();
        query.add("select", "id,name,location,status");
        return supabaseGatewayService.getList("devices", query, Device[].class);
    }

    private List<SensorData> fetchSensorData(MonitorRequest request) {
        MultiValueMap<String, String> query = new LinkedMultiValueMap<>();
        query.add("select", "id,device_id,voltage,current,power,energy,created_at");
        query.add("order", "created_at.asc");
        query.add("created_at", "gte." + request.from());
        query.add("created_at", "lte." + request.to());
        if (request.deviceId() != null) {
            query.add("device_id", "eq." + request.deviceId());
        }
        return supabaseGatewayService.getList("sensor_data", query, SensorData[].class);
    }

    private Map<Integer, DeviceLimit> fetchLatestLimits(Integer deviceId) {
        MultiValueMap<String, String> query = new LinkedMultiValueMap<>();
        query.add("select", "id,device_id,max_power,max_current,created_at");
        query.add("order", "created_at.desc");
        if (deviceId != null) {
            query.add("device_id", "eq." + deviceId);
        }

        Map<Integer, DeviceLimit> limitsByDeviceId = new HashMap<>();
        for (DeviceLimit limit : supabaseGatewayService.getList("device_limits", query, DeviceLimit[].class)) {
            if (limit.getDevice_id() != null) {
                limitsByDeviceId.putIfAbsent(limit.getDevice_id(), limit);
            }
        }
        return limitsByDeviceId;
    }

    private List<Alert> fetchAlerts(Integer deviceId, OffsetDateTime from, OffsetDateTime to) {
        MultiValueMap<String, String> query = new LinkedMultiValueMap<>();
        query.add("select", "id,device_id,type,message,created_at,is_read");
        query.add("order", "created_at.desc");
        query.add("created_at", "gte." + from);
        query.add("created_at", "lte." + to);
        if (deviceId != null) {
            query.add("device_id", "eq." + deviceId);
        }
        return supabaseGatewayService.getList("alerts", query, Alert[].class);
    }

    private void syncThresholdAlerts(
            List<SensorData> sensorRows,
            Map<Integer, DeviceLimit> limitsByDeviceId,
            Map<Integer, Device> devicesById,
            MonitorRequest request) {
        if (sensorRows.isEmpty()) {
            return;
        }

        List<Alert> existingAlerts = fetchAlerts(request.deviceId(), request.from(), request.to());
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
                supabaseGatewayService.post("alerts", payload);
                alertKeys.add(key);
            }
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
                    : "Thiết bị " + row.getDevice_id();
            List<String> breachTypes = getBreachTypes(row, snapshot.limitsByDeviceId().get(row.getDevice_id()));

            OffsetDateTime bucketTime = truncate(row.getCreated_at(), bucket);
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
                device != null && device.getName() != null ? device.getName() : "Thiết bị " + row.getDevice_id(),
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
            return new DeviceResponse(null, "Tất cả thiết bị", null, null);
        }

        Device device = devicesById.get(deviceId);
        if (device == null) {
            return new DeviceResponse(deviceId, "Thiết bị " + deviceId, null, null);
        }

        return new DeviceResponse(device.getId(), device.getName(), device.getLocation(), device.getStatus());
    }

    private MonitorRequest normalizeRequest(Integer deviceId, LocalDateTime from, LocalDateTime to, String bucket) {
        LocalDateTime resolvedTo = to == null ? LocalDateTime.now() : to;
        LocalDateTime resolvedFrom = from == null ? resolvedTo.minusHours(24) : from;
        if (resolvedFrom.isAfter(resolvedTo)) {
            throw new ResponseStatusException(BAD_REQUEST, "Thời gian bắt đầu phải nhỏ hơn hoặc bằng thời gian kết thúc.");
        }

        String resolvedBucket = bucket == null || bucket.isBlank() ? "raw" : bucket.toLowerCase();
        if (!Set.of("raw", "hour", "day").contains(resolvedBucket)) {
            throw new ResponseStatusException(BAD_REQUEST, "bucket chỉ hỗ trợ raw, hour hoặc day.");
        }

        ZoneId zoneId = ZoneId.systemDefault();
        return new MonitorRequest(
                deviceId,
                resolvedFrom.atZone(zoneId).toOffsetDateTime(),
                resolvedTo.atZone(zoneId).toOffsetDateTime(),
                resolvedBucket);
    }

    private OffsetDateTime truncate(OffsetDateTime timestamp, String bucket) {
        if ("day".equals(bucket)) {
            return timestamp.toLocalDate().atStartOfDay().atOffset(timestamp.getOffset());
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

    private String buildAlertKey(Integer deviceId, OffsetDateTime createdAt, String type) {
        return deviceId + "|" + createdAt + "|" + type;
    }

    private String buildAlertMessage(Device device, SensorData sensorData, DeviceLimit limit, String breachType) {
        String deviceName = device != null && device.getName() != null
                ? device.getName()
                : "Thiết bị " + sensorData.getDevice_id();
        if ("POWER_LIMIT_EXCEEDED".equals(breachType)) {
            return String.format(
                    "%s vượt ngưỡng công suất: %.2fW / %.2fW",
                    deviceName,
                    safeNumber(sensorData.getPower()),
                    safeNumber(limit.getMax_power()));
        }
        return String.format(
                "%s vượt ngưỡng dòng điện: %.2fA / %.2fA",
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
        appendHeaderRow(xml, List.of("Thời gian", "Thiết bị", "Điện áp (V)", "Dòng điện (A)", "Công suất (W)", "Energy", "Vượt ngưỡng", "Loại cảnh báo"));
        for (MonitorHistoryRowResponse row : historyRows) {
            appendDataRow(xml, List.of(
                    row.createdAt(),
                    row.deviceName(),
                    stringValue(row.voltage()),
                    stringValue(row.current()),
                    stringValue(row.power()),
                    stringValue(row.energy()),
                    row.isThresholdBreached() ? "Có" : "Không",
                    String.join(", ", row.breachTypes())));
        }
        xml.append("</Table></Worksheet>");

        xml.append("<Worksheet ss:Name=\"Alerts\"><Table>");
        appendHeaderRow(xml, List.of("ID", "Thiết bị", "Loại", "Nội dung", "Thời gian", "Đã đọc"));
        for (Alert alert : alerts) {
            appendDataRow(xml, List.of(
                    stringValue(alert.getId()),
                    stringValue(alert.getDevice_id()),
                    alert.getType(),
                    alert.getMessage(),
                    formatTimestamp(alert.getCreated_at()),
                    Boolean.TRUE.equals(alert.getIs_read()) ? "Có" : "Chưa"));
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

    private String formatTimestamp(OffsetDateTime value) {
        if (value == null) {
            return null;
        }
        return value.toLocalDateTime().format(DATE_TIME_FORMATTER);
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

    private record MonitorRequest(
            Integer deviceId,
            OffsetDateTime from,
            OffsetDateTime to,
            String bucket) {
    }

    private record DashboardSnapshot(
            Map<Integer, Device> devicesById,
            List<SensorData> sensorRows,
            List<Alert> alerts,
            Map<Integer, DeviceLimit> limitsByDeviceId) {
    }

    private static final class AggregatedHistoryRow {
        private final OffsetDateTime bucketTime;
        private final Set<String> breachTypes = new LinkedHashSet<>();
        private double voltageSum;
        private double currentSum;
        private double powerSum;
        private double energySum;
        private int count;
        private Integer deviceId;
        private String deviceName;

        private AggregatedHistoryRow(OffsetDateTime bucketTime) {
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
                deviceName = "Tất cả thiết bị";
            }

            breachTypes.addAll(resolvedBreachTypes);
            count++;
        }

        private MonitorHistoryRowResponse toResponse() {
            return new MonitorHistoryRowResponse(
                    bucketTime.toLocalDateTime().format(DATE_TIME_FORMATTER),
                    deviceId,
                    deviceName == null ? "Tất cả thiết bị" : deviceName,
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
