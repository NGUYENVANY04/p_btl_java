package com.example.demo;

import com.example.demo.model.MqttMessage;
import com.example.demo.repository.MqttMessageRepository;
import com.hivemq.client.mqtt.MqttClientSslConfig;
import com.hivemq.client.mqtt.mqtt5.Mqtt5BlockingClient;
import com.hivemq.client.mqtt.mqtt5.Mqtt5Client;
import org.json.JSONObject;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import java.time.LocalDateTime;
import java.util.UUID;

@SpringBootApplication
public class DemoApplication {
	private final MqttMessageRepository repository;
	private final SimpMessagingTemplate messagingTemplate;

	public DemoApplication(MqttMessageRepository repository, SimpMessagingTemplate messagingTemplate) {
		this.repository = repository;
		this.messagingTemplate = messagingTemplate;
	}

	public static void main(String[] args) {
		SpringApplication.run(DemoApplication.class, args);
	}

	@Bean
	public Mqtt5BlockingClient mqttClient() {
		return Mqtt5Client.builder()
				.identifier("java-server-" + UUID.randomUUID())
				.serverHost("broker.hivemq.com")
				.serverPort(8884)
				.webSocketConfig().serverPath("mqtt").applyWebSocketConfig()
				.sslConfig(MqttClientSslConfig.builder().build())
				.buildBlocking();
	}

	@Bean
	public CommandLineRunner startMqtt(Mqtt5BlockingClient client) {
		return args -> {
			try {
				client.connect();
				System.out.println("✅ SERVER PTIT ĐÃ ONLINE!");
				client.toAsync().subscribeWith()
						.topicFilter("ptit/test/request")
						.callback(publish -> {
							String payload = new String(publish.getPayloadAsBytes());
							try {
								JSONObject json = new JSONObject(payload);
								repository.save(new MqttMessage(
										json.optDouble("p", 0.0),
										json.optDouble("v", 0.0),
										json.optDouble("i", 0.0),
										LocalDateTime.now()));
								messagingTemplate.convertAndSend("/topic/messages", payload);
								System.out.println("📩 Đã nhận: " + payload);
							} catch (Exception e) {
								e.printStackTrace();
							}
						}).send();
			} catch (Exception e) {
				System.err.println("❌ Lỗi: " + e.getMessage());
			}
		};
	}
}