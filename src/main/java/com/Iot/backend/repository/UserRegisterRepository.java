package com.Iot.backend.repository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Repository;
import org.springframework.web.client.RestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;

@Repository
public class UserRegisterRepository {

    private final RestTemplate restTemplate;

    private final String URL = "https://znfxhbrkabxenuzrcogd.supabase.co/rest/v1";
    private final String API_KEY = "sb_secret_NXFJy_AuCYhqJmmJadDKNA_I7N91qFu";

    public UserRegisterRepository(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    private HttpHeaders createHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("apikey", API_KEY);
        headers.set("Authorization", "Bearer " + API_KEY);
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Prefer", "return=representation");
        return headers;
    }


    public Map<String, Object> registerAccount(Map<String, Object> info) {

        String url = URL + "/users";
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(info, createHeaders());

        ResponseEntity<List<Map<String, Object>>> response = restTemplate.exchange(
                url,
                HttpMethod.POST, // http
                entity,
                new ParameterizedTypeReference<List<Map<String, Object>>>() {
                });

        return response.getBody() != null && !response.getBody().isEmpty()
                ? response.getBody().get(0)
                : new HashMap<>();
    }
}