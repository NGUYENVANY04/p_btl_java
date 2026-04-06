package com.example.demo.controller;

import com.example.demo.model.Alert;
import com.example.demo.service.SupabaseService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api")
public class SupabaseController {

    private final SupabaseService service;

    public SupabaseController(SupabaseService service) {
        this.service = service;
    }

    // POST insert alert
    @PostMapping("/alerts")
    public ResponseEntity<?> createAlert(@RequestBody Alert alert) {
        try {
            String result = service.insertAlert(alert);

            // Trả về JSON chuẩn
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "data", result));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "success", false,
                            "error", e.getMessage()));
        }
    }

}