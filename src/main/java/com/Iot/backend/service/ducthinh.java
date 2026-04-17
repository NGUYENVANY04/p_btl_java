package com.Iot.backend.service;

import org.eclipse.paho.client.mqttv3.*;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import jakarta.annotation.PostConstruct;
import java.util.UUID;

@Service
public class ducthinh {

    // URL trỏ thẳng vào bảng sensor_data
    private final String SUPABASE_URL = "https://znfxhbrkabxenuzrcogd.supabase.co/rest/v1/sensor_data";
    private final String API_KEY = "sb_secret_NXFJy_AuCYhqJmmJadDKNA_I7N91qFu";

    // Cấu hình MQTT HiveMQ
    private final String MQTT_BROKER = "tcp://broker.hivemq.com:1883";
    private final String CLIENT_ID = "Java_Server_BTL_" + UUID.randomUUID().toString().substring(0, 5);
    private final String TOPIC = "ptit/test/request";

    private final RestTemplate restTemplate = new RestTemplate();

    @PostConstruct
    public void startBridge() {
        try {
            MqttClient client = new MqttClient(MQTT_BROKER, CLIENT_ID);
            MqttConnectOptions options = new MqttConnectOptions();
            options.setAutomaticReconnect(true);
            options.setCleanSession(true);

            client.setCallback(new MqttCallback() {
                @Override
                public void messageArrived(String topic, MqttMessage message) {
                    String payload = new String(message.getPayload());
                    System.out.println("\n[MQTT RECEIVED]: " + payload);
                    sendToSupabase(payload);
                }

                @Override
                public void connectionLost(Throwable cause) {
                    System.err.println("Mất kết nối MQTT!");
                }

                @Override
                public void deliveryComplete(IMqttDeliveryToken token) {
                }
            });

            client.connect(options);
            client.subscribe(TOPIC);
            System.out.println(">>> BACKEND ĐANG CHẠY - ĐANG ĐỢI DỮ LIỆU TỪ TOPIC: " + TOPIC);

        } catch (MqttException e) {
            System.err.println("Lỗi khởi tạo MQTT: " + e.getMessage());
        }
    }

    private void sendToSupabase(String jsonPayload) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("apikey", API_KEY);
            headers.set("Authorization", "Bearer " + API_KEY);
            headers.set("Prefer", "return=minimal");

            HttpEntity<String> entity = new HttpEntity<>(jsonPayload, headers);
            ResponseEntity<String> response = restTemplate.postForEntity(SUPABASE_URL, entity, String.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                System.out.println("=> [OK] Đã đẩy lên Supabase thành công!");
            }
        } catch (Exception e) {
            System.err.println("=> [LỖI] Supabase từ chối: " + e.getMessage());
            System.err.println("Gợi ý: Kiểm tra bảng 'devices' đã có ID=1 chưa?");
        }
    }
}