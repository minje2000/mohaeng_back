package org.poolpool.mohaeng.ai.chat.service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.poolpool.mohaeng.ai.chat.dto.AiChatRequest;
import org.poolpool.mohaeng.ai.chat.dto.AiChatResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Service
public class AiChatProxyService {

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${ai.base-url}")
    private String aiBaseUrl;

    @Value("${ai.api-key}")
    private String aiApiKey;

    public AiChatResponse chat(AiChatRequest request, String authorizationHeader) {
        String url = aiBaseUrl + "/ai/chat";

        HttpHeaders headers = createHeaders(authorizationHeader);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("message", request.getMessage());
        body.put("authorization", authorizationHeader);
        body.put("history", request.getHistory());
        body.put("sessionId", request.getSessionId());
        body.put("pageType", request.getPageType());
        body.put("contextPage", request.getContextPage());
        body.put("region", request.getRegion());
        body.put("locationKeywords", request.getLocationKeywords());
        body.put("filters", request.getFilters());

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<AiChatResponse> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    entity,
                    AiChatResponse.class
            );

            if (response.getBody() != null) {
                return response.getBody();
            }

        } catch (HttpStatusCodeException e) {
            System.out.println("[AI CHAT ERROR] status=" + e.getStatusCode());
            System.out.println("[AI CHAT ERROR] body=" + e.getResponseBodyAsString());
        } catch (Exception e) {
            System.out.println("[AI CHAT ERROR] " + e.getMessage());
        }

        AiChatResponse fallback = new AiChatResponse();
        fallback.setIntent("fallback");
        fallback.setAnswer("지금은 AI 서버와 연결하는 중 문제가 생겼어요. 잠시 후 다시 시도해 주세요.");
        return fallback;
    }

    public List<Map<String, Object>> getAdminContacts(int limit, String authorizationHeader) {
        String url = UriComponentsBuilder
                .fromHttpUrl(aiBaseUrl + "/ai/admin/contacts")
                .queryParam("limit", limit)
                .toUriString();
        return getItems(url, authorizationHeader);
    }

    public List<Map<String, Object>> getAdminLogs(int limit, String authorizationHeader) {
        String url = UriComponentsBuilder
                .fromHttpUrl(aiBaseUrl + "/ai/admin/logs")
                .queryParam("limit", limit)
                .toUriString();
        return getItems(url, authorizationHeader);
    }

    private List<Map<String, Object>> getItems(String url, String authorizationHeader) {
        HttpEntity<Void> entity = new HttpEntity<>(createHeaders(authorizationHeader));
        try {
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    new ParameterizedTypeReference<Map<String, Object>>() {}
            );
            Object items = response.getBody() == null ? null : response.getBody().get("items");
            if (items instanceof List<?> list) {
                return list.stream()
                        .filter(Map.class::isInstance)
                        .map(item -> (Map<String, Object>) item)
                        .toList();
            }
        } catch (HttpStatusCodeException e) {
            System.out.println("[AI ADMIN ERROR] status=" + e.getStatusCode());
            System.out.println("[AI ADMIN ERROR] body=" + e.getResponseBodyAsString());
        } catch (Exception e) {
            System.out.println("[AI ADMIN ERROR] " + e.getMessage());
        }
        return List.of();
    }

    private HttpHeaders createHeaders(String authorizationHeader) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-API-Key", aiApiKey);
        if (authorizationHeader != null && !authorizationHeader.isBlank()) {
            headers.set(HttpHeaders.AUTHORIZATION, authorizationHeader);
        }
        return headers;
    }
}
