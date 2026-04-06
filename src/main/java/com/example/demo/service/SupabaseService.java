// src/main/java/com/example/demo/service/SupabaseService.java
package com.example.demo.service;

import com.example.demo.model.Alert;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;
import java.util.List;

@Service
public class SupabaseService {

    private final String URL = "https://znfxhbrkabxenuzrcogd.supabase.co/rest/v1/alerts";
    private final String API_KEY = "sb_secret_NXFJy_AuCYhqJmmJadDKNA_I7N91qFu";

    // GET tất cả alerts
    public List<Alert> getAllData() {
        RestTemplate restTemplate = new RestTemplate();

        HttpHeaders headers = new HttpHeaders();
        headers.set("apikey", API_KEY);
        headers.set("Authorization", "Bearer " + API_KEY);

        HttpEntity<String> entity = new HttpEntity<>(headers);

        ResponseEntity<Alert[]> response = restTemplate.exchange(
                URL,
                HttpMethod.GET,
                entity,
                Alert[].class);

        return Arrays.asList(response.getBody());
    }

    // POST insert alert
    public String insertAlert(Alert alert) {
        RestTemplate restTemplate = new RestTemplate();

        HttpHeaders headers = new HttpHeaders();
        headers.set("apikey", API_KEY);
        headers.set("Authorization", "Bearer " + API_KEY);
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Alert> entity = new HttpEntity<>(alert, headers);

        ResponseEntity<String> response = restTemplate.postForEntity(
                URL,
                entity,
                String.class);

        return response.getBody();
    }
}