package com.Iot.backend.service;

import org.eclipse.paho.client.mqttv3.*;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import jakarta.annotation.PostConstruct;
import java.util.UUID;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.ParameterizedTypeReference;
import java.util.List;
import java.util.Map;
import java.time.Instant;
import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.scheduling.annotation.Scheduled;
import java.util.ArrayList;

@Service
public class ducthinh {

    // === Cấu hình Supabase ===
    private final String SUPABASE_URL = "https://znfxhbrkabxenuzrcogd.supabase.co/rest/v1/sensor_data";
    private final String API_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InpuZnhoYnJrYWJ4ZW51enJjb2dkIiwicm9sZSI6InNlcnZpY2Vfcm9sZSIsImlhdCI6MTc3NTQ0Mjk4NSwiZXhwIjoyMDkxMDE4OTg1fQ.qNtUEq0HebqEs6tHrWT6Ghj94-UOb5dshIWAvWB8r6Y";

    // === Cấu hình MQTT ===
    private final String MQTT_BROKER = "tcp://broker.hivemq.com:1883";
    private final String CLIENT_ID = "Java_IoT_Full_Service_" + UUID.randomUUID().toString().substring(0, 5);
    private final String DATA_TOPIC = "ptit/test/request";
    private final String CONTROL_TOPIC = "ptit/device/control";

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Map<Integer, Instant> lastSeen = new ConcurrentHashMap<>();
    private MqttClient mqttClient;
    private final EmailService emailService;

    // Constructor injection
    public ducthinh(EmailService emailService) {
        this.emailService = emailService;
    }

    @PostConstruct
    public void init() {
        try {
            mqttClient = new MqttClient(MQTT_BROKER, CLIENT_ID);
            MqttConnectOptions options = new MqttConnectOptions();
            options.setAutomaticReconnect(true);
            options.setCleanSession(true);

            mqttClient.setCallback(new MqttCallback() {
                @Override
                public void messageArrived(String topic, MqttMessage message) {
                    String payload = new String(message.getPayload());
                    System.out.println("📥 NHẬN DỮ LIỆU: " + payload);
                    // Lưu vào DB để giao diện Web hiển thị thông tin
                    saveToDatabase(payload);
                    // Kiểm tra ngưỡng và tạo cảnh báo nếu vượt quá
                    checkThresholds(payload);
                }

                @Override
                public void connectionLost(Throwable cause) {
                }

                @Override
                public void deliveryComplete(IMqttDeliveryToken token) {
                }
            });

            mqttClient.connect(options);
            mqttClient.subscribe(DATA_TOPIC); // Lắng nghe để lấy dữ liệu lên Web
            System.out.println("✅ HỆ THỐNG SẴN SÀNG: Lắng nghe & Điều khiển");
        } catch (MqttException e) {
            System.err.println("❌ Lỗi MQTT: " + e.getMessage());
        }
    }

    // Ghi dữ liệu vào Supabase (Cho mục đích hiển thị trên Web)
    private void saveToDatabase(String jsonPayload) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("apikey", API_KEY);
            headers.set("Authorization", "Bearer " + API_KEY);

            // Không dùng ON CONFLICT để tránh lỗi 400 nếu bạn chưa chỉnh DB
            // Dữ liệu sẽ được lưu thành các dòng mới (Logs)
            HttpEntity<String> entity = new HttpEntity<>(jsonPayload, headers);
            restTemplate.postForEntity(SUPABASE_URL, entity, String.class);
            System.out.println("✅ Đã lưu dữ liệu cảm biến vào Database.");
        } catch (Exception e) {
            System.err.println("❌ Lỗi lưu DB: " + e.getMessage());
        }
    }

    // Kiểm tra ngưỡng và tạo cảnh báo
    private void checkThresholds(String payload) {
        try {
            Map<String, Object> data = objectMapper.readValue(payload, Map.class);
            if (data.get("device_id") == null || data.get("power") == null || data.get("current") == null) {
                System.err.println("❌ Dữ liệu thiếu: " + payload);
                return;
            }
            Integer deviceId = ((Number) data.get("device_id")).intValue();
            Float power = ((Number) data.get("power")).floatValue();
            Float current = ((Number) data.get("current")).floatValue();

            // Cập nhật thời gian cuối cùng nhận tín hiệu
            lastSeen.put(deviceId, Instant.now());

            Map<String, Object> limit = getDeviceLimit(deviceId);
            if (limit != null) {
                Float maxPower = limit.get("max_power") != null ? ((Number) limit.get("max_power")).floatValue() : Float.MAX_VALUE;
                Float maxCurrent = limit.get("max_current") != null ? ((Number) limit.get("max_current")).floatValue() : Float.MAX_VALUE;

                if (power > maxPower && !hasUnreadAlert(deviceId, "POWER_EXCEEDED")) {
                    createAlert(deviceId, "POWER_EXCEEDED", "Công suất vượt ngưỡng: " + power + " > " + maxPower);
                }
                if (current > maxCurrent && !hasUnreadAlert(deviceId, "CURRENT_EXCEEDED")) {
                    createAlert(deviceId, "CURRENT_EXCEEDED", "Dòng điện vượt ngưỡng: " + current + " > " + maxCurrent);
                }
            }
        } catch (Exception e) {
            System.err.println("❌ Lỗi kiểm tra ngưỡng: " + e.getMessage());
        }
    }

    // Lấy giới hạn thiết bị từ DB
    private Map<String, Object> getDeviceLimit(Integer deviceId) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("apikey", API_KEY);
            headers.set("Authorization", "Bearer " + API_KEY);
            HttpEntity<String> entity = new HttpEntity<>(headers);

            String url = "https://znfxhbrkabxenuzrcogd.supabase.co/rest/v1/device_limits?device_id=eq." + deviceId;
            ResponseEntity<List<Map<String, Object>>> response = restTemplate.exchange(
                url, HttpMethod.GET, entity, new ParameterizedTypeReference<List<Map<String, Object>>>() {}
            );
            List<Map<String, Object>> body = response.getBody();
            if (body != null && !body.isEmpty()) {
                return body.get(0);
            }
        } catch (Exception e) {
            System.err.println("❌ Lỗi lấy giới hạn: " + e.getMessage());
        }
        return null;
    }

    // Tạo cảnh báo
    private void createAlert(Integer deviceId, String type, String message) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("apikey", API_KEY);
            headers.set("Authorization", "Bearer " + API_KEY);

            Map<String, Object> alert = Map.of(
                "device_id", deviceId,
                "type", type,
                "message", message,
                "is_read", false
            );
            String json = objectMapper.writeValueAsString(alert);
            HttpEntity<String> entity = new HttpEntity<>(json, headers);
            restTemplate.postForEntity("https://znfxhbrkabxenuzrcogd.supabase.co/rest/v1/alerts", entity, String.class);
            System.out.println("🚨 Cảnh báo đã tạo: " + message);

            // Gửi email thông báo
            emailService.sendAlertEmail("Cảnh báo IoT: " + type, message);
        } catch (Exception e) {
            System.err.println("❌ Lỗi tạo cảnh báo: " + e.getMessage());
        }
    }

    // Lấy tất cả device IDs từ device_limits
    private List<Integer> getAllDeviceIds() {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("apikey", API_KEY);
            headers.set("Authorization", "Bearer " + API_KEY);
            HttpEntity<String> entity = new HttpEntity<>(headers);

            String url = "https://znfxhbrkabxenuzrcogd.supabase.co/rest/v1/device_limits?select=device_id";
            ResponseEntity<List<Map<String, Object>>> response = restTemplate.exchange(
                url, HttpMethod.GET, entity, new ParameterizedTypeReference<List<Map<String, Object>>>() {}
            );
            List<Map<String, Object>> body = response.getBody();
            List<Integer> ids = new ArrayList<>();
            if (body != null) {
                for (Map<String, Object> item : body) {
                    if (item.get("device_id") != null) {
                        ids.add(((Number) item.get("device_id")).intValue());
                    }
                }
            }
            return ids;
        } catch (Exception e) {
            System.err.println("❌ Lỗi lấy device IDs: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    // Kiểm tra có cảnh báo chưa đọc cùng loại không
    private boolean hasUnreadAlert(Integer deviceId, String type) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("apikey", API_KEY);
            headers.set("Authorization", "Bearer " + API_KEY);
            HttpEntity<String> entity = new HttpEntity<>(headers);

            String url = "https://znfxhbrkabxenuzrcogd.supabase.co/rest/v1/alerts?select=id&device_id=eq." + deviceId + "&type=eq." + type + "&is_read=is.false&limit=1";
            ResponseEntity<List<Map<String, Object>>> response = restTemplate.exchange(
                url, HttpMethod.GET, entity, new ParameterizedTypeReference<List<Map<String, Object>>>() {}
            );
            List<Map<String, Object>> body = response.getBody();
            return body != null && !body.isEmpty();
        } catch (Exception e) {
            System.err.println("❌ Lỗi kiểm tra alert: " + e.getMessage());
            return false;
        }
    }

    // Kiểm tra thiết bị offline mỗi 5 phút
    @Scheduled(fixedRate = 300000) // 5 phút
    private void checkOfflineDevices() {
        Instant now = Instant.now();
        List<Integer> allDevices = getAllDeviceIds();
        for (Integer deviceId : allDevices) {
            Instant last = lastSeen.get(deviceId);
            if (last == null || Duration.between(last, now).toMinutes() > 5) {
                if (!hasUnreadAlert(deviceId, "OFFLINE")) {
                    createAlert(deviceId, "OFFLINE", "Thiết bị mất kết nối (quá 5 phút không gửi tín hiệu).");
                }
            }
        }
    }

    // Gửi lệnh điều khiển qua MQTT
    public String controlDevice(Integer deviceId, String status) {
        try {
            if (mqttClient == null || !mqttClient.isConnected())
                return "MQTT chưa kết nối!";
            String payload = String.format("{\"device_id\": %d, \"status\": \"%s\"}", deviceId, status.toUpperCase());
            mqttClient.publish(CONTROL_TOPIC, new MqttMessage(payload.getBytes()));
            return "Thành công: Gửi lệnh " + status + " tới thiết bị " + deviceId;
        } catch (MqttException e) {
            return "❌ Lỗi: " + e.getMessage();
        }
    }
}