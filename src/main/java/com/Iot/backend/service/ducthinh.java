package com.Iot.backend.service;

import org.eclipse.paho.client.mqttv3.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import jakarta.annotation.PostConstruct;

import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.json.JSONObject;

@Service
public class ducthinh {

    // ================= CONFIG =================
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

    private final String clientId = "Java_IoT_Service_" +
            UUID.randomUUID().toString().substring(0, 8);

    private final RestTemplate restTemplate = new RestTemplate();
    private MqttClient mqttClient;

    // ================= QUEUE =================
    private final ConcurrentLinkedQueue<String> queue = new ConcurrentLinkedQueue<>();

    // ================= INIT =================
    @PostConstruct
    public void init() {
        connectMqtt();
        startWorker();
    }

    // ================= MQTT =================
    private void connectMqtt() {
        try {
            mqttClient = new MqttClient(mqttBroker, clientId);

            MqttConnectOptions options = new MqttConnectOptions();
            options.setAutomaticReconnect(true);
            options.setCleanSession(true);
            options.setConnectionTimeout(10);

            mqttClient.setCallback(new MqttCallback() {

                @Override
                public void messageArrived(String topic, MqttMessage message) {
                    String payload = new String(message.getPayload());
                    System.out.println("📥 MQTT Received: " + payload);

                    queue.add(payload);
                }

                @Override
                public void connectionLost(Throwable cause) {
                    System.err.println("⚠️ MQTT lost: " + cause.getMessage());
                }

                @Override
                public void deliveryComplete(IMqttDeliveryToken token) {
                }
            });

            mqttClient.connect(options);
            mqttClient.subscribe(dataTopic);

            System.out.println("🚀 MQTT connected: " + dataTopic);

        } catch (MqttException e) {
            System.err.println("❌ MQTT error: " + e.getMessage());
        }
    }

    // ================= WORKER =================
    private void startWorker() {
        new Thread(() -> {
            while (true) {
                try {
                    if (!queue.isEmpty()) {
                        String data = queue.poll();
                        processData(data);
                    }
                    Thread.sleep(800);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    // ================= PROCESS =================
    private void processData(String jsonPayload) {
        try {
            JSONObject json = new JSONObject(jsonPayload);
            int deviceId = json.getInt("device_id");

            ensureDeviceExists(deviceId);

            sendToSupabase(jsonPayload);

        } catch (Exception e) {
            System.err.println("❌ JSON error: " + e.getMessage());
        }
    }

    // ================= AUTO CREATE DEVICE =================
    private void ensureDeviceExists(int deviceId) {
        try {
            String deviceUrl = supabaseUrl.replace("sensor_data", "devices")
                    + "?id=eq." + deviceId;

            HttpHeaders headersGet = new HttpHeaders();
            headersGet.set("apikey", apiKey);
            headersGet.set("Authorization", "Bearer " + apiKey);

            HttpEntity<String> entityGet = new HttpEntity<>(headersGet);

            ResponseEntity<String> response = restTemplate.exchange(
                    deviceUrl, HttpMethod.GET, entityGet, String.class);

            if (response.getBody().equals("[]")) {

                String newDevice = String.format(
                        "{\"id\": %d, \"name\": \"Device %d\"}",
                        deviceId, deviceId);

                HttpHeaders headersPost = new HttpHeaders();
                headersPost.set("apikey", apiKey);
                headersPost.set("Authorization", "Bearer " + apiKey);
                headersPost.setContentType(MediaType.APPLICATION_JSON);

                HttpEntity<String> entityPost = new HttpEntity<>(newDevice, headersPost);

                restTemplate.postForEntity(
                        supabaseUrl.replace("sensor_data", "devices"),
                        entityPost,
                        String.class);

                System.out.println("🆕 Device created: " + deviceId);
            }

        } catch (Exception e) {
            System.err.println("❌ Device error: " + e.getMessage());
        }
    }

    // ================= SEND TO SUPABASE (FIXED) =================
    private void sendToSupabase(String jsonPayload) {

        int maxRetry = 5;
        int delay = 1000;

        for (int i = 0; i < maxRetry; i++) {
            try {
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                headers.set("apikey", apiKey);
                headers.set("Authorization", "Bearer " + apiKey);

                // ❌ FIX: KHÔNG UPSERT, KHÔNG ON CONFLICT
                String url = supabaseUrl;

                HttpEntity<String> entity = new HttpEntity<>(jsonPayload, headers);

                ResponseEntity<String> response = restTemplate.postForEntity(
                        url, entity, String.class);

                if (response.getStatusCode().is2xxSuccessful()) {
                    System.out.println("✅ Supabase insert success");
                    return;
                }

            } catch (Exception e) {
                System.err.println("❌ Supabase retry " + (i + 1) + ": " + e.getMessage());

                try {
                    Thread.sleep(delay);
                } catch (InterruptedException ex) {
                    ex.printStackTrace();
                }

                delay *= 2;
            }
        }

        System.err.println("💥 Supabase failed after retry!");
    }

    // ================= CONTROL DEVICE =================
    public String controlDevice(Integer deviceId, String command) {
        try {

            if (mqttClient == null || !mqttClient.isConnected()) {
                connectMqtt();
                if (!mqttClient.isConnected()) {
                    return "MQTT not connected";
                }
            }

            // ❌ NO STATUS ANYMORE
            String payload = String.format(
                    "{\"device_id\": %d, \"command\": \"%s\"}",
                    deviceId, command.toUpperCase());

            MqttMessage message = new MqttMessage(payload.getBytes());
            message.setQos(1);

            mqttClient.publish(controlTopic, message);

            System.out.println("📤 Control sent: " + payload);
            return "Success";

        } catch (MqttException e) {
            System.err.println("❌ MQTT error: " + e.getMessage());
            return "Failed";
        }
    }
}