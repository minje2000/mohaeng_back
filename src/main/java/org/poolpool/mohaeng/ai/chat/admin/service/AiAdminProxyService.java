package org.poolpool.mohaeng.ai.chat.admin.service;

import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class AiAdminProxyService {

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${ai.base-url}")
    private String aiBaseUrl;

    @Value("${ai.api-key}")
    private String aiApiKey;

    private HttpHeaders headers() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-API-Key", aiApiKey);
        return headers;
    }

    public Object getContacts() {
        ResponseEntity<Object> response = restTemplate.exchange(
                aiBaseUrl + "/ai/admin/contacts",
                HttpMethod.GET,
                new HttpEntity<>(headers()),
                Object.class
        );
        return response.getBody();
    }

    public Object answerContact(Long itemId, Map<String, Object> payload) {
        ResponseEntity<Object> response = restTemplate.exchange(
                aiBaseUrl + "/ai/admin/contacts/" + itemId,
                HttpMethod.PUT,
                new HttpEntity<>(payload, headers()),
                Object.class
        );
        return response.getBody();
    }

    public Object getLogs(Integer limit) {
        String url = aiBaseUrl + "/ai/admin/logs?limit=" + (limit == null ? 200 : limit);
        ResponseEntity<Object> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                new HttpEntity<>(headers()),
                Object.class
        );
        return response.getBody();
    }
}
