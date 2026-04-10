package com.example.demo.iot.controller;

import com.example.demo.iot.model.SensorData;
import com.example.demo.iot.repository.SensorRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/power")
@CrossOrigin(origins = "*")
public class PowerController {

    @Autowired
    private SensorRepository sensorRepository;

    @GetMapping("/history")
    public List<SensorData> getAll(
            @RequestParam(required = false) String deviceId,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to
    ) {
        LocalDateTime fromTime = parseDateTimeStart(from);
        LocalDateTime toTime = parseDateTimeEnd(to);

        if (deviceId != null && !deviceId.isBlank()) {
            if (fromTime != null && toTime != null) {
                return sensorRepository.findByDeviceIdAndTimestampBetweenOrderByTimestampAsc(deviceId, fromTime, toTime);
            }
            if (fromTime != null) {
                return sensorRepository.findByDeviceIdAndTimestampGreaterThanEqualOrderByTimestampAsc(deviceId, fromTime);
            }
            if (toTime != null) {
                return sensorRepository.findByDeviceIdAndTimestampLessThanEqualOrderByTimestampAsc(deviceId, toTime);
            }
            return sensorRepository.findByDeviceIdOrderByTimestampAsc(deviceId);
        }

        if (fromTime != null && toTime != null) {
            return sensorRepository.findByTimestampBetweenOrderByTimestampAsc(fromTime, toTime);
        }
        if (fromTime != null) {
            return sensorRepository.findByTimestampGreaterThanEqualOrderByTimestampAsc(fromTime);
        }
        if (toTime != null) {
            return sensorRepository.findByTimestampLessThanEqualOrderByTimestampAsc(toTime);
        }
        return sensorRepository.findAll();
    }

    @GetMapping("/latest")
    public SensorData getLatest() {
        return sensorRepository.findTopByOrderByTimestampDesc();
    }

    @GetMapping("/report/daily")
    public Map<String, Object> dailyReport(
            @RequestParam String date,
            @RequestParam(required = false) String deviceId
    ) {
        LocalDate day = LocalDate.parse(date);
        LocalDateTime from = day.atStartOfDay();
        LocalDateTime to = day.plusDays(1).atStartOfDay().minusNanos(1);
        List<SensorData> data = fetchHistory(deviceId, from, to);
        return buildReport("DAILY", day.toString(), deviceId, data);
    }

    @GetMapping("/report/monthly")
    public Map<String, Object> monthlyReport(
            @RequestParam String month,
            @RequestParam(required = false) String deviceId
    ) {
        YearMonth ym = YearMonth.parse(month);
        LocalDateTime from = ym.atDay(1).atStartOfDay();
        LocalDateTime to = ym.atEndOfMonth().atTime(23, 59, 59, 999_999_999);
        List<SensorData> data = fetchHistory(deviceId, from, to);
        return buildReport("MONTHLY", ym.toString(), deviceId, data);
    }

    private List<SensorData> fetchHistory(String deviceId, LocalDateTime from, LocalDateTime to) {
        if (deviceId != null && !deviceId.isBlank()) {
            return sensorRepository.findByDeviceIdAndTimestampBetweenOrderByTimestampAsc(deviceId, from, to);
        }
        return sensorRepository.findByTimestampBetweenOrderByTimestampAsc(from, to);
    }

    private Map<String, Object> buildReport(String type, String period, String deviceId, List<SensorData> data) {
        double sumPower = 0.0;
        double sumVoltage = 0.0;
        double sumCurrent = 0.0;
        int powerCount = 0;
        int voltageCount = 0;
        int currentCount = 0;
        Double latestTotalKwh = null;

        for (SensorData d : data) {
            if (d.getPower() != null) {
                sumPower += d.getPower();
                powerCount++;
            }
            if (d.getVoltage() != null) {
                sumVoltage += d.getVoltage();
                voltageCount++;
            }
            if (d.getCurrent() != null) {
                sumCurrent += d.getCurrent();
                currentCount++;
            }
            if (d.getTotalKwh() != null) {
                latestTotalKwh = d.getTotalKwh();
            }
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("reportType", type);
        result.put("period", period);
        result.put("deviceId", deviceId);
        result.put("samples", data.size());
        result.put("avgPower", powerCount == 0 ? null : sumPower / powerCount);
        result.put("avgVoltage", voltageCount == 0 ? null : sumVoltage / voltageCount);
        result.put("avgCurrent", currentCount == 0 ? null : sumCurrent / currentCount);
        result.put("latestTotalKwh", latestTotalKwh);
        result.put("data", data);
        return result;
    }

    private LocalDateTime parseDateTimeStart(String value) {
        if (value == null || value.isBlank()) return null;
        if (value.length() == 10) {
            return LocalDate.parse(value).atStartOfDay();
        }
        return LocalDateTime.parse(value);
    }

    private LocalDateTime parseDateTimeEnd(String value) {
        if (value == null || value.isBlank()) return null;
        if (value.length() == 10) {
            return LocalDate.parse(value).plusDays(1).atStartOfDay().minusNanos(1);
        }
        return LocalDateTime.parse(value);
    }
}

