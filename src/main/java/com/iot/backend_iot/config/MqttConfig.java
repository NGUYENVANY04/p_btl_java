package com.iot.backend_iot.config;

import com.iot.backend_iot.model.SensorData;
import com.iot.backend_iot.service.TelemetryService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.annotation.PostConstruct;
import org.eclipse.paho.client.mqttv3.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MqttConfig implements MqttCallback {

    @Autowired
    private TelemetryService telemetryService;

    private MqttClient mqttClient; // Đưa ra ngoài để quản lý tốt hơn
    private final String broker = "tcp://broker.emqx.io:1883"; 
    private final String topic = "iot/btl/diennang"; 

    @PostConstruct
    public void connect() {
        try {
            // Khởi tạo mqttClient ở đây
            mqttClient = new MqttClient(broker, MqttClient.generateClientId());
            mqttClient.setCallback(this);
            mqttClient.connect();
            mqttClient.subscribe(topic);
            System.out.println(">>> BACKEND DA KET NOI MQTT VA DANG DOI DIEN NANG...");
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void messageArrived(String topic, MqttMessage message) throws Exception {
        String payload = new String(message.getPayload());
        System.out.println("Nhan tu ESP32: " + payload);
        
        try {
            ObjectMapper mapper = new ObjectMapper();
            mapper.registerModule(new JavaTimeModule());
            
            SensorData data = mapper.readValue(payload, SensorData.class);
            telemetryService.handleIncomingTelemetry(data);
            
            System.out.println(">>> Da luu du lieu dien nang thanh cong!");
        } catch (Exception e) {
            System.out.println("Loi format du lieu: " + e.getMessage());
        }
    }

    @Override
    public void connectionLost(Throwable cause) {
        System.out.println("Mat ket noi MQTT! Dang thu lai...");
    }

    @Override
    public void deliveryComplete(IMqttDeliveryToken token) {
    }
}