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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import jakarta.annotation.PostConstruct;
import java.util.UUID;

@Service
public class ducthinh {

    @Value("${supabase.url}")
    private String supabaseUrl;

    @Value("${supabase.key}")
    private String apiKey;

    @Value("${mqtt.broker}")
    private String mqttBroker;

    @Value("${mqtt.topic.data}")
    private String dataTopic;

    @Value("${mqtt.topic.control}")
    private String controlTopic;

    private final String clientId = "Java_IoT_Service_" + UUID.randomUUID().toString().substring(0, 8);
    private final RestTemplate restTemplate = new RestTemplate();
    private MqttClient mqttClient;

    @PostConstruct
    public void init() {
        connectMqtt();
    }

    /**
     * Khởi tạo kết nối tới MQTT Broker
     */
    private void connectMqtt() {
        try {
            mqttClient = new MqttClient(mqttBroker, clientId);
            MqttConnectOptions options = new MqttConnectOptions();
            options.setAutomaticReconnect(true);
            options.setCleanSession(true);
            options.setConnectionTimeout(10);

            // mqttClient.setCallback(new MqttCallback() {
            // @Override
            // public void messageArrived(String topic, MqttMessage message) {
            // String payload = new String(message.getPayload());
            // System.out.println("📥 [MQTT] Nhận dữ liệu: " + payload);
            // // saveToDatabase(payload);
            // }

            // @Override
            // public void connectionLost(Throwable cause) {
            // System.err.println("⚠️ [MQTT] Mất kết nối: " + cause.getMessage());
            // }

            // @Override
            // public void deliveryComplete(IMqttDeliveryToken token) {
            // }
            // });

            mqttClient.connect(options);
            mqttClient.subscribe(dataTopic);
            System.out.println("🚀 [MQTT] Đã kết nối & lắng nghe topic: " + dataTopic);
        } catch (MqttException e) {
            System.err.println("❌ [MQTT] Lỗi kết nối: " + e.getMessage());
        }
    }

    /**
     * Ghi dữ liệu cảm biến vào Supabase
     */
    private void saveToDatabase(String jsonPayload) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("apikey", apiKey);
            headers.set("Authorization", "Bearer " + apiKey);

            HttpEntity<String> entity = new HttpEntity<>(jsonPayload, headers);
            ResponseEntity<String> response = restTemplate.postForEntity(supabaseUrl, entity, String.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                System.out.println("✅ [Supabase] Lưu dữ liệu thành công.");
            }
        } catch (Exception e) {
            System.err.println("❌ [Supabase] Lỗi lưu dữ liệu: " + e.getMessage());
        }
    }

    /**
     * Gửi lệnh điều khiển thiết bị qua MQTT
     */
    public String controlDevice(Integer deviceId, String status) {
        // TẠO LUỒNG RIÊNG: Để Java trả về "Thành công" ngay lập tức cho Web
        new Thread(() -> {
            try {
                if (mqttClient == null || !mqttClient.isConnected()) {
                    connectMqtt();
                }
                String payload = String.format("{\"device_id\": %d, \"status\": \"%s\"}", deviceId,
                        status.toUpperCase());
                MqttMessage message = new MqttMessage(payload.getBytes());
                message.setQos(0);

                // Việc gửi này có thể mất 2 giây, nhưng nó chạy ở luồng khác, không làm treo
                // Web
                mqttClient.publish("ptit/test/request", message);
                System.out.println("📤 MQTT Sent: " + payload);
            } catch (MqttException e) {
                e.printStackTrace();
            }
        }).start();

        // Dòng này chạy ngay lập tức (~1ms), Web sẽ nhận được phản hồi cực nhanh ->
        // KHÔNG RELOAD
        return "Thành công";
    }
}