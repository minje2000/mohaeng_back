package org.poolpool.mohaeng.ai.client;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.core.ParameterizedTypeReference;

import reactor.core.publisher.Mono;

@Service
public class AiAgentClient {

    private final WebClient webClient;

    public AiAgentClient(
            @Value("${ai.base-url}") String baseUrl,
            @Value("${ai.api-key}") String apiKey
    ) {
        this.webClient = WebClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader("X-API-KEY", apiKey)
                .build();
    }

    /**
     * FastAPI POST 호출 - 단일 객체 반환
     */
    public <T> Mono<T> post(String uri, Object body, Class<T> responseType) {
        return webClient.post()
                .uri(uri)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .retrieve()
                .bodyToMono(responseType);
    }

    /**
     * FastAPI POST 호출 - 리스트 반환 (타입 안전)
     */
    public Mono<List<Long>> postLongList(String uri, Object body) {
        return webClient.post()
                .uri(uri)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<List<Long>>() {});
    }

    /**
     * FastAPI GET 호출
     */
    public <T> Mono<T> get(String uri, Class<T> responseType) {
        return webClient.get()
                .uri(uri)
                .retrieve()
                .bodyToMono(responseType);
    }
}
