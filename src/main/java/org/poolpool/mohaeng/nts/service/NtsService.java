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

    // 국세청 사업자 상태 조회
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
            JsonNode dataNode = root.path("data").get(0);

            String statusCodeStr = dataNode.path("b_stt_cd").asText();

            if (statusCodeStr == null || statusCodeStr.isEmpty()) {
                return -99;
            }

            return Integer.parseInt(statusCodeStr);

        } catch (Exception e) {
            return -1;
        }
    }

    // 국세청 진위확인 API
    public boolean validateBiz(String businessNumber, String ownerName, String openDate) {

        String url = "https://api.odcloud.kr/api/nts-businessman/v1/validate"
                + "?serviceKey=" + serviceKey
                + "&returnType=JSON";

        Map<String, Object> biz = new HashMap<>();
        biz.put("b_no", businessNumber);
        biz.put("start_dt", openDate);
        biz.put("p_nm", ownerName);

        Map<String, Object> body = new HashMap<>();
        body.put("businesses", List.of(biz));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Map<String, Object>> entity =
                new HttpEntity<>(body, headers);

        ResponseEntity<String> response =
                restTemplate.exchange(url, HttpMethod.POST, entity, String.class);

        try {

            JsonNode root = objectMapper.readTree(response.getBody());
            JsonNode dataNode = root.path("data").get(0);

            String validCode = dataNode.path("valid").asText();

            return "01".equals(validCode);

        } catch (Exception e) {
            return false;
        }
    }
}