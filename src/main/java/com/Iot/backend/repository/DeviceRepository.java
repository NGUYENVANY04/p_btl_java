package com.Iot.backend.repository;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Repository;
import org.springframework.web.client.RestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;

@Repository
public class DeviceRepository {

    private final RestTemplate restTemplate;

    private final String URL = "https://znfxhbrkabxenuzrcogd.supabase.co/rest/v1";
    private final String API_KEY = "sb_secret_NXFJy_AuCYhqJmmJadDKNA_I7N91qFu";

    public DeviceRepository(RestTemplate restTemplate) {
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

    public Map<String, Object> createDevice(Map<String, Object> device) {
        try {

            String url = URL + "/devices";
            System.out.println(url);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(device, createHeaders());

            ResponseEntity<List<Map<String, Object>>> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    entity,
                    new ParameterizedTypeReference<List<Map<String, Object>>>() {
                    });

            return response.getBody() != null && !response.getBody().isEmpty()
                    ? response.getBody().get(0)
                    : new HashMap<>();

        } catch (Exception e) {
            System.out.println("CREATE ERROR: " + e.getMessage());
            return new HashMap<>();
        }
    }

    public Map<String, Object> createLimitDevice(Long id, Map<String, Object> device) {
        try {

            String url = URL + "/device_limits?id=eq." + id;
            System.out.println(url);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(device, createHeaders());
            ResponseEntity<List<Map<String, Object>>> response = restTemplate.exchange(
                    url,
                    HttpMethod.PUT,
                    entity,
                    new ParameterizedTypeReference<List<Map<String, Object>>>() {
                    });

            return response.getBody() != null && !response.getBody().isEmpty()
                    ? response.getBody().get(0)
                    : new HashMap<>();

        } catch (Exception e) {
            System.out.println("CREATE ERROR: " + e.getMessage());
            return new HashMap<>();
        }
    }

    public List<Map<String, Object>> getAllLimitDevices() {
        try {
            String url = URL + "/device_limits?select=*";

            HttpEntity<String> entity = new HttpEntity<>(createHeaders());

            ResponseEntity<List<Map<String, Object>>> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    new ParameterizedTypeReference<List<Map<String, Object>>>() {
                    });

            return response.getBody() != null
                    ? response.getBody()
                    : new ArrayList<>();

        } catch (Exception e) {
            System.out.println("GET ERROR: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    public List<Map<String, Object>> getAllDevices() {
        try {
            String url = URL + "/devices?select=*";

            HttpEntity<String> entity = new HttpEntity<>(createHeaders());

            ResponseEntity<List<Map<String, Object>>> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    new ParameterizedTypeReference<List<Map<String, Object>>>() {
                    });

            return response.getBody() != null
                    ? response.getBody()
                    : new ArrayList<>();

        } catch (Exception e) {
            System.out.println("GET ERROR: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    public Map<String, Object> updateDevice(Long id, Map<String, Object> device) {
        try {

            String url = URL + "/devices?id=eq." + id;
            System.out.println(" SEND: " + url);
            Map<String, Object> body = new HashMap<>();
            body.put("name", device.get("name"));
            body.put("location", device.get("location"));
            body.put("status", true);

            System.out.println("BODY SEND: " + body);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, createHeaders());

            ResponseEntity<List<Map<String, Object>>> response = restTemplate.exchange(
                    url,
                    HttpMethod.PUT,
                    entity,
                    new ParameterizedTypeReference<List<Map<String, Object>>>() {
                    });

            return response.getBody() != null && !response.getBody().isEmpty()
                    ? response.getBody().get(0)
                    : new HashMap<>();

        } catch (Exception e) {
            System.out.println("UPDATE ERROR: " + e.getMessage());
            return new HashMap<>();
        }
    }

    public String deleteDevice(Long id) {
        try {
            String url = URL + "/devices?id=eq." + id;

            HttpEntity<String> entity = new HttpEntity<>(createHeaders());

            restTemplate.exchange(
                    url,
                    HttpMethod.DELETE,
                    entity,
                    String.class);

            return "Deleted successfully";

        } catch (Exception e) {
            System.out.println("DELETE ERROR: " + e.getMessage());
            return "Delete failed";
        }
    }
}