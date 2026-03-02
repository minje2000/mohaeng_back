package org.poolpool.mohaeng.nts.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class NtsService {

    @Value("${nts.service-key}")
    private String serviceKey;
    
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public int getStatus(String businessNumber) {

        String url = "https://api.odcloud.kr/api/nts-businessman/v1/status"
                + "?serviceKey=" + serviceKey
                + "&returnType=JSON";

        Map<String, Object> body = new HashMap<>();
        body.put("b_no", List.of(businessNumber));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Map<String, Object>> entity =
                new HttpEntity<>(body, headers);

        ResponseEntity<String> response =
                restTemplate.exchange(url, HttpMethod.POST, entity, String.class);

        try {
            JsonNode root = objectMapper.readTree(response.getBody());

            // API 정상 여부 체크
            String statusCode = root.path("status_code").asText();
            if (!"OK".equals(statusCode)) {
                return -1;  // API 오류
            }

            JsonNode dataNode = root.path("data").get(0);

            // 상태코드 추출(int)
            String statusCodeStr = dataNode.path("b_stt_cd").asText();

            if (statusCodeStr == null || statusCodeStr.isEmpty()) {
                return -99;  // 존재하지 않는 사업자
            }

            return Integer.parseInt(statusCodeStr);

        } catch (Exception e) {
            return -1;
        }
    }
}