package com.example.demo.controller;

import com.example.demo.model.Alert;
import com.example.demo.service.SupabaseService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class SupabaseController {

    private final SupabaseService service;

    public SupabaseController(SupabaseService service) {
        this.service = service;
    }

    // POST alert
    @PostMapping("/alerts")
    public ResponseEntity<?> createAlert(@RequestBody Alert alert) {
        String result = service.insertAlert(alert);

        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Alert created successfully",
                "data", result));
    }

    // GET tất cả alert
    @GetMapping("/alerts")

    public ResponseEntity<?> getAlerts() {
        List<Alert> list = service.getAllAlerts();
        return ResponseEntity.ok(Map.of(
                "success", true,
                "data", list));
    }
}