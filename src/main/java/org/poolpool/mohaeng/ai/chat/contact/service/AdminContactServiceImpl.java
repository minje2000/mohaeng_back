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
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class AdminContactServiceImpl implements AdminContactService {

    private static final Path STORAGE_PATH = Paths.get("data", "chatbot", "admin-contacts.json");

    private final ObjectMapper objectMapper;

    public AdminContactServiceImpl(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public synchronized AdminContactResponseDto create(AdminContactRequestDto request, String userId) {
        ensureStorage();
        String content = request == null || request.getContent() == null ? "" : request.getContent().trim();
        if (content.isBlank()) {
            throw new IllegalArgumentException("문의 내용은 비어 있을 수 없습니다.");
        }

        List<Map<String, Object>> items = readItems();
        String now = now();
        String sessionId = request == null || request.getSessionId() == null || request.getSessionId().trim().isBlank()
                ? "auto_" + UUID.randomUUID().toString().replace("-", "")
                : request.getSessionId().trim();
        String id = UUID.randomUUID().toString();
        String ticketId = String.valueOf(items.size() + 1L);

        Map<String, Object> saved = new HashMap<>();
        saved.put("id", id);
        saved.put("ticketId", ticketId);
        saved.put("userId", blankToNull(userId));
        saved.put("sessionId", sessionId);
        saved.put("content", content);
        saved.put("status", "대기");
        saved.put("createdAt", now);
        saved.put("updatedAt", now);
        saved.put("answeredAt", "");
        saved.put("answer", "");
        saved.put("assignee", "");
        saved.put("category", blankToNull(userId) != null ? "회원 문의" : "일반");
        saved.put("priority", "보통");
        saved.put("source", request != null && request.getSource() != null && !request.getSource().isBlank() ? request.getSource().trim() : "chatbot");
        saved.put("hasAuthorization", blankToNull(userId) != null);
        saved.put("history", new ArrayList<>());
        appendHistory(saved, "접수", blankToNull(userId) != null ? "회원" : "AI 챗봇", Map.of("status", "대기"));
        items.add(0, saved);
        writeItems(items);
        return toDto(saved);
    }

    @Override
    public synchronized List<AdminContactResponseDto> listForAdmin(int limit) {
        ensureStorage();
        return readItems().stream()
                .map(this::normalizeItem)
                .sorted(Comparator.comparing((Map<String, Object> row) -> String.valueOf(row.getOrDefault("createdAt", ""))).reversed())
                .limit(Math.max(1, Math.min(limit, 500)))
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public synchronized AdminContactResponseDto update(String itemId, Map<String, Object> payload) {
        ensureStorage();
        List<Map<String, Object>> items = readItems();
        for (int i = 0; i < items.size(); i++) {
            Map<String, Object> current = normalizeItem(items.get(i));
            if (!Objects.equals(String.valueOf(current.get("id")), String.valueOf(itemId))) {
                continue;
            }
            String answer = text(payload.get("answer"));
            String assignee = text(payload.get("assignee"));
            String status = normalizeStatus(text(payload.get("status")), answer);
            String category = text(payload.get("category")).isBlank() ? text(current.get("category")) : text(payload.get("category"));
            String priority = text(payload.get("priority")).isBlank() ? text(current.get("priority")) : text(payload.get("priority"));
            String memo = text(payload.get("memo"));

            Map<String, Object> changes = new HashMap<>();
            updateField(current, changes, "answer", answer);
            updateField(current, changes, "assignee", assignee);
            updateField(current, changes, "status", status);
            updateField(current, changes, "category", category);
            updateField(current, changes, "priority", priority);
            if (!memo.isBlank()) {
                changes.put("memo", memo);
            }
            current.put("updatedAt", now());
            if (!answer.isBlank()) {
                current.put("answeredAt", now());
            }
            appendHistory(current, !answer.isBlank() ? "답변" : "수정", assignee.isBlank() ? "관리자" : assignee, changes);
            items.set(i, current);
            writeItems(items);
            return toDto(current);
        }
        return null;
    }

    @Override
    public synchronized AdminContactResponseDto delete(String itemId) {
        ensureStorage();
        List<Map<String, Object>> items = readItems();
        List<Map<String, Object>> remaining = new ArrayList<>();
        Map<String, Object> deleted = null;
        for (Map<String, Object> item : items) {
            Map<String, Object> current = normalizeItem(item);
            if (Objects.equals(String.valueOf(current.get("id")), String.valueOf(itemId))) {
                appendHistory(current, "삭제", "관리자", Map.of());
                deleted = current;
                continue;
            }
            remaining.add(current);
        }
        if (deleted != null) {
            writeItems(remaining);
            return toDto(deleted);
        }
        return null;
    }

    @Override
    public synchronized List<AdminContactResponseDto> listMine(String userId, int limit) {
        ensureStorage();
        String normalizedUserId = blankToNull(userId);
        if (normalizedUserId == null) {
            return List.of();
        }
        return readItems().stream()
                .map(this::normalizeItem)
                .filter(item -> Objects.equals(text(item.get("userId")), normalizedUserId))
                .sorted(Comparator.comparing((Map<String, Object> row) -> String.valueOf(row.getOrDefault("createdAt", ""))).reversed())
                .limit(Math.max(1, Math.min(limit, 200)))
                .map(this::toDto)
                .collect(Collectors.toList());
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

    private Map<String, Object> normalizeItem(Map<String, Object> source) {
        Map<String, Object> item = new HashMap<>();
        String id = text(source.get("id"));
        if (id.isBlank()) id = UUID.randomUUID().toString();
        item.put("id", id);
        item.put("ticketId", text(source.get("ticketId")).isBlank() ? id : text(source.get("ticketId")));
        item.put("userId", text(source.get("userId")));
        item.put("sessionId", text(source.get("sessionId")).isBlank() ? "auto_" + UUID.randomUUID().toString().replace("-", "") : text(source.get("sessionId")));
        item.put("content", !text(source.get("content")).isBlank() ? text(source.get("content")) : text(source.get("message")));
        item.put("answer", text(source.get("answer")));
        item.put("status", normalizeStatus(text(source.get("status")), text(source.get("answer"))));
        item.put("assignee", text(source.get("assignee")));
        item.put("category", text(source.get("category")).isBlank() ? (text(source.get("userId")).isBlank() ? "일반" : "회원 문의") : text(source.get("category")));
        item.put("priority", text(source.get("priority")).isBlank() ? "보통" : text(source.get("priority")));
        item.put("source", text(source.get("source")).isBlank() ? "chatbot" : text(source.get("source")));
        item.put("createdAt", text(source.get("createdAt")).isBlank() ? now() : text(source.get("createdAt")));
        item.put("updatedAt", text(source.get("updatedAt")).isBlank() ? text(source.get("createdAt")) : text(source.get("updatedAt")));
        item.put("answeredAt", text(source.get("answeredAt")));
        item.put("hasAuthorization", !text(source.get("userId")).isBlank() || Boolean.TRUE.equals(source.get("hasAuthorization")));
        item.put("history", normalizeHistory(source.get("history")));
        return item;
    }

    private List<Map<String, Object>> normalizeHistory(Object raw) {
        List<Map<String, Object>> result = new ArrayList<>();
        if (raw instanceof List<?> list) {
            for (Object entry : list) {
                if (entry instanceof Map<?, ?> map) {
                    Map<String, Object> row = new HashMap<>();
                    row.put("id", text(map.get("id")).isBlank() ? UUID.randomUUID().toString() : text(map.get("id")));
                    row.put("action", text(map.get("action")).isBlank() ? "수정" : text(map.get("action")));
                    row.put("actor", text(map.get("actor")).isBlank() ? "관리자" : text(map.get("actor")));
                    row.put("createdAt", text(map.get("createdAt")).isBlank() ? now() : text(map.get("createdAt")));
                    Object changes = map.get("changes");
                    row.put("changes", copyChanges(changes));
                    result.add(row);
                }
            }
        }
        return result;
    }

    private Map<String, Object> copyChanges(Object raw) {
        Map<String, Object> copied = new HashMap<>();
        if (raw instanceof Map<?, ?> map) {
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                copied.put(String.valueOf(entry.getKey()), entry.getValue());
            }
        }
        return copied;
    }

    private void appendHistory(Map<String, Object> item, String action, String actor, Map<String, Object> changes) {
        List<Map<String, Object>> history = normalizeHistory(item.get("history"));
        Map<String, Object> row = new HashMap<>();
        row.put("id", UUID.randomUUID().toString());
        row.put("action", action);
        row.put("actor", actor);
        row.put("createdAt", now());
        row.put("changes", changes == null ? new HashMap<>() : copyChanges(changes));
        history.add(0, row);
        item.put("history", history.stream().limit(50).collect(Collectors.toList()));
    }

    private void updateField(Map<String, Object> current, Map<String, Object> changes, String key, String value) {
        String prev = text(current.get(key));
        String next = value == null ? "" : value;
        current.put(key, next);
        if (!Objects.equals(prev, next)) {
            changes.put(key, next);
        }
    }

    private String normalizeStatus(String raw, String answer) {
        String value = text(raw);
        if (value.equals("RECEIVED") || value.equals("WAITING") || value.equals("PENDING")) return "대기";
        if (value.equals("IN_PROGRESS") || value.equals("PROCESSING")) return "처리중";
        if (value.equals("ANSWERED") || value.equals("DONE")) return "답변완료";
        if (value.equals("CLOSED")) return "종결";
        if (List.of("대기", "처리중", "답변완료", "종결").contains(value)) return value;
        return answer != null && !answer.isBlank() ? "답변완료" : "대기";
    }

    private AdminContactResponseDto toDto(Map<String, Object> item) {
        Map<String, Object> current = normalizeItem(item);
        return AdminContactResponseDto.builder()
                .id(text(current.get("id")))
                .ticketId(text(current.get("ticketId")))
                .status(text(current.get("status")))
                .content(text(current.get("content")))
                .answer(text(current.get("answer")))
                .sessionId(text(current.get("sessionId")))
                .source(text(current.get("source")))
                .assignee(text(current.get("assignee")))
                .category(text(current.get("category")))
                .priority(text(current.get("priority")))
                .createdAt(text(current.get("createdAt")))
                .updatedAt(text(current.get("updatedAt")))
                .answeredAt(text(current.get("answeredAt")))
                .userId(text(current.get("userId")))
                .hasAuthorization(Boolean.TRUE.equals(current.get("hasAuthorization")))
                .history(normalizeHistory(current.get("history")))
                .build();
    }

    private String now() {
        return LocalDateTime.now().toString();
    }

    private String text(Object value) {
        return value == null ? "" : String.valueOf(value).trim();
    }

    private String blankToNull(String value) {
        return value == null || value.trim().isBlank() ? null : value.trim();
    }
}
