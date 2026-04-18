// package com.Iot.backend.service;

// import org.eclipse.paho.client.mqttv3.*;
// import org.springframework.http.*;
// import org.springframework.stereotype.Service;
// import org.springframework.web.client.RestTemplate;

// import jakarta.annotation.PostConstruct;

// import java.util.UUID;

// @Service
// public class ducthinh {

//     // ================= SUPABASE =================
//     private final String SUPABASE_URL = "https://znfxhbrkabxenuzrcogd.supabase.co/rest/v1/sensor_data";

//     // 🔴 THAY BẰNG service_role key
//     private final String API_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InpuZnhoYnJrYWJ4ZW51enJjb2dkIiwicm9sZSI6InNlcnZpY2Vfcm9sZSIsImlhdCI6MTc3NTQ0Mjk4NSwiZXhwIjoyMDkxMDE4OTg1fQ.qNtUEq0HebqEs6tHrWT6Ghj94-UOb5dshIWAvWB8r6Y";

//     // ================= MQTT =================
//     private final String MQTT_BROKER = "tcp://broker.hivemq.com:1883";
//     private final String CLIENT_ID = "Java_Server_BTL_" + UUID.randomUUID().toString().substring(0, 5);

//     private final String DATA_TOPIC = "ptit/test/request";
//     private final String CONTROL_TOPIC = "ptit/device/control";

//     private final RestTemplate restTemplate = new RestTemplate();
//     private MqttClient mqttClient;

//     // ================= START MQTT =================
//     @PostConstruct
//     public void startBridge() {
//         try {
//             mqttClient = new MqttClient(MQTT_BROKER, CLIENT_ID);

//             MqttConnectOptions options = new MqttConnectOptions();
//             options.setAutomaticReconnect(true);
//             options.setCleanSession(true);

//             mqttClient.setCallback(new MqttCallback() {

//                 @Override
//                 public void messageArrived(String topic, MqttMessage message) {
//                     String payload = new String(message.getPayload());
//                     System.out.println("\n📥 MQTT RECEIVED: " + payload);

//                     sendToSupabase(payload);
//                 }

//                 @Override
//                 public void connectionLost(Throwable cause) {
//                     System.err.println("❌ MQTT mất kết nối!");
//                 }

//                 @Override
//                 public void deliveryComplete(IMqttDeliveryToken token) {
//                 }
//             });

//             mqttClient.connect(options);
//             mqttClient.subscribe(DATA_TOPIC);

//             System.out.println("✅ MQTT CONNECTED");
//             System.out.println("📡 LISTENING: " + DATA_TOPIC);

//         } catch (Exception e) {
//             System.err.println("❌ Lỗi MQTT: " + e.getMessage());
//         }
//     }

//     // ================= UPSERT DATA =================
//     private void sendToSupabase(String jsonPayload) {
//         try {
//             HttpHeaders headers = new HttpHeaders();
//             headers.setContentType(MediaType.APPLICATION_JSON);

//             // 🔑 AUTH
//             headers.set("apikey", API_KEY);
//             headers.set("Authorization", "Bearer " + API_KEY);

//             // 🔥 UPSERT (ghi đè theo device_id)
//             headers.set("Prefer", "resolution=merge-duplicates");

//             // 🔥 QUAN TRỌNG: chỉ định cột conflict
//             String url = SUPABASE_URL + "?on_conflict=device_id";

//             HttpEntity<String> entity = new HttpEntity<>(jsonPayload, headers);

//             ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);

//             if (response.getStatusCode().is2xxSuccessful()) {
//                 System.out.println("✅ UPSERT thành công!");
//             } else {
//                 System.err.println("⚠️ Supabase response: " + response.getStatusCode());
//             }

//         } catch (Exception e) {
//             System.err.println("❌ Lỗi Supabase: " + e.getMessage());
//         }
//     }

//     // ================= CONTROL DEVICE =================
//     public String controlDevice(Integer deviceId, String status) {
//         try {
//             if (mqttClient == null || !mqttClient.isConnected()) {
//                 return "MQTT chưa kết nối!";
//             }

//             // Chuẩn hóa ON/OFF
//             status = status.toUpperCase();
//             if (!status.equals("ON") && !status.equals("OFF")) {
//                 return "Trạng thái không hợp lệ (ON/OFF)";
//             }

//             // JSON gửi xuống thiết bị
//             String message = "{ \"deviceId\": " + deviceId +
//                     ", \"status\": \"" + status + "\" }";

//             mqttClient.publish(CONTROL_TOPIC, new MqttMessage(message.getBytes()));

//             System.out.println("📤 SEND MQTT: " + message);

//             return "Đã gửi lệnh " + status + " tới device " + deviceId;

//         } catch (Exception e) {
//             return "❌ Lỗi điều khiển: " + e.getMessage();
//         }
//     }
// }
package com.Iot.backend.service;

import org.eclipse.paho.client.mqttv3.*;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import jakarta.annotation.PostConstruct;
import java.util.UUID;

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
    private MqttClient mqttClient;

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