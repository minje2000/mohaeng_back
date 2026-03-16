package org.poolpool.mohaeng.ai.chat.contact.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.poolpool.mohaeng.ai.chat.contact.dto.AdminContactRequestDto;
import org.poolpool.mohaeng.ai.chat.contact.dto.AdminContactResponseDto;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class AdminContactServiceImpl implements AdminContactService {

    private static final Path STORAGE_PATH = Paths.get("data", "chatbot", "admin-contacts.json");

    private final ObjectMapper objectMapper;

    public AdminContactServiceImpl(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public synchronized AdminContactResponseDto create(AdminContactRequestDto request) {
        ensureStorage();
        String content = request == null || request.getContent() == null ? "" : request.getContent().trim();
        if (content.isBlank()) {
            throw new IllegalArgumentException("문의 내용은 비어 있을 수 없습니다.");
        }

        List<Map<String, Object>> items = readItems();
        long nextId = items.stream()
                .map(item -> item.get("ticketId"))
                .filter(Number.class::isInstance)
                .map(Number.class::cast)
                .mapToLong(Number::longValue)
                .max()
                .orElse(0L) + 1L;

        Map<String, Object> saved = new HashMap<>();
        saved.put("ticketId", nextId);
        saved.put("sessionId", request.getSessionId());
        saved.put("content", content);
        saved.put("status", "RECEIVED");
        saved.put("createdAt", LocalDateTime.now().toString());
        items.add(saved);
        writeItems(items);
        return new AdminContactResponseDto(nextId, "RECEIVED");
    }

    private void ensureStorage() {
        try {
            Files.createDirectories(STORAGE_PATH.getParent());
            if (!Files.exists(STORAGE_PATH)) {
                objectMapper.writerWithDefaultPrettyPrinter().writeValue(STORAGE_PATH.toFile(), new ArrayList<>());
            }
        } catch (IOException e) {
            throw new RuntimeException("관리자 문의 저장소를 준비하는 중 문제가 생겼습니다.", e);
        }
    }

    private List<Map<String, Object>> readItems() {
        try {
            return objectMapper.readValue(STORAGE_PATH.toFile(), new TypeReference<List<Map<String, Object>>>() {});
        } catch (IOException e) {
            throw new RuntimeException("관리자 문의를 읽는 중 문제가 생겼습니다.", e);
        }
    }

    private void writeItems(List<Map<String, Object>> items) {
        try {
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(STORAGE_PATH.toFile(), items);
        } catch (IOException e) {
            throw new RuntimeException("관리자 문의를 저장하는 중 문제가 생겼습니다.", e);
        }
    }
}
