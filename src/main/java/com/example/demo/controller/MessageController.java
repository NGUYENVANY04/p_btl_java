package com.example.demo.controller;

import com.example.demo.repository.MqttMessageRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class MessageController {

    private final MqttMessageRepository repository;

    // Sử dụng Constructor Injection (Chuẩn khuyến nghị của Spring)
    public MessageController(MqttMessageRepository repository) {
        this.repository = repository;
    }

    @GetMapping("/messages")
    public String index(Model model) { // Đổi tên hàm thành index cho đồng bộ
        model.addAttribute("dsTinNhan", repository.findAll());
        return "index";
    }
}