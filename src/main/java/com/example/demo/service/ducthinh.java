// package com.example.demo.service;

// // import com.example.demo.model.SensorData;
// // import com.example.demo.model.Alert;
// // import com.example.demo.model.Device;
// // import com.example.demo.model.DeviceStatus;
// // import com.example.demo.model.DeviceLimit;
// // dùng cái nào thì xóa cmt cái đấy

// import org.springframework.http.*;
// import org.springframework.stereotype.Service;
// import org.springframework.web.client.RestTemplate;

// @Service
// public class ducthinh {

// private final RestTemplate restTemplate = new RestTemplate();
// private final String URL =
// "https://znfxhbrkabxenuzrcogd.supabase.co/rest/v1";
// private final String API_KEY = "sb_secret_NXFJy_AuCYhqJmmJadDKNA_I7N91qFu";

// // thêm các chức năng get post del update tại đây

// }
package com.example.demo.service;

import com.example.demo.model.MqttMessage;
import com.example.demo.repository.MqttMessageRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;

@Service
public class ducthinh {

    @Autowired
    private MqttMessageRepository repository; // Cửa ngõ lưu vào Database

    @Autowired
    private SimpMessagingTemplate messagingTemplate; // Gửi dữ liệu lên giao diện Web

    public void handleIncomingData(Double p, Double v, Double i) {
        // 1. Tạo đối tượng từ dữ liệu MQTT nhận được
        MqttMessage message = new MqttMessage(p, v, i, LocalDateTime.now());

        // 2. Lưu xuống Database (H2 sẽ tự sinh ID)
        MqttMessage savedData = repository.save(message);
        System.out.println("✅ Đã lưu vào H2 Database: ID " + savedData.getId());

        // 3. Đẩy dữ liệu sang Dashboard qua WebSocket
        messagingTemplate.convertAndSend("/topic/messages", savedData);
    }
}