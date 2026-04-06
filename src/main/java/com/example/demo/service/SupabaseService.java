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

    // POST alert
    public String insertAlert(Alert alert) {
        try {
            RestTemplate restTemplate = new RestTemplate();

            HttpHeaders headers = new HttpHeaders();
            headers.set("apikey", API_KEY);
            headers.set("Authorization", "Bearer " + API_KEY);
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Alert> entity = new HttpEntity<>(alert, headers);
            ResponseEntity<String> response = restTemplate.postForEntity(URL, entity, String.class);

            return response.getBody() != null ? response.getBody() : "{}";

        } catch (Exception e) {
            e.printStackTrace();
            return "{}";
        }
    }

    // GET tất cả alert
    public List<Alert> getAllAlerts() {
        try {
            RestTemplate restTemplate = new RestTemplate();

            HttpHeaders headers = new HttpHeaders();
            headers.set("apikey", API_KEY);
            headers.set("Authorization", "Bearer " + API_KEY);

            HttpEntity<String> entity = new HttpEntity<>(headers);
            ResponseEntity<Alert[]> response = restTemplate.exchange(
                    URL + "?select=*",
                    HttpMethod.GET,
                    entity,
                    Alert[].class);

            return Arrays.asList(response.getBody());

        } catch (Exception e) {
            e.printStackTrace();
            return List.of();
        }
    }
}