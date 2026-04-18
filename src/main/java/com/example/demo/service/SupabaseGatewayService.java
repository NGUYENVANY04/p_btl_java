package com.example.demo.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.Arrays;
import java.util.List;

import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;

@Service
public class SupabaseGatewayService {

    private final RestTemplate restTemplate;
    private final String baseUrl;
    private final String apiKey;

    public SupabaseGatewayService(
            RestTemplate restTemplate,
            @Value("${supabase.url}") String baseUrl,
            @Value("${supabase.api-key:}") String apiKey) {
        this.restTemplate = restTemplate;
        this.baseUrl = baseUrl;
        this.apiKey = apiKey;
    }

    public <T> List<T> getList(String table, MultiValueMap<String, String> queryParams, Class<T[]> responseType) {
        ensureConfigured();

        URI uri = buildUri(table, queryParams);
        HttpEntity<Void> entity = new HttpEntity<>(buildHeaders());
        ResponseEntity<T[]> response = restTemplate.exchange(uri, HttpMethod.GET, entity, responseType);
        T[] body = response.getBody();
        return body == null ? List.of() : Arrays.asList(body);
    }

    public void post(String table, Object payload) {
        ensureConfigured();

        HttpHeaders headers = buildHeaders();
        headers.set("Prefer", "return=minimal");
        HttpEntity<Object> entity = new HttpEntity<>(payload, headers);
        restTemplate.exchange(buildUri(table, null), HttpMethod.POST, entity, String.class);
    }

    private URI buildUri(String table, MultiValueMap<String, String> queryParams) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(baseUrl).pathSegment(table);
        if (queryParams != null) {
            queryParams.forEach((key, values) -> values.forEach(value -> builder.queryParam(key, value)));
        }
        return builder.build().encode().toUri();
    }

    private HttpHeaders buildHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("apikey", apiKey);
        headers.setBearerAuth(apiKey);
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        return headers;
    }

    private void ensureConfigured() {
        if (apiKey == null || apiKey.isBlank()) {
            throw new ResponseStatusException(
                    INTERNAL_SERVER_ERROR,
                    "Thiếu SUPABASE_API_KEY. Hãy cấu hình biến môi trường trước khi chạy backend.");
        }
    }
}
