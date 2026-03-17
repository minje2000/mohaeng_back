package org.poolpool.mohaeng.ai.chat.faq.service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import org.poolpool.mohaeng.ai.chat.faq.dto.AdminAiFaqItemDto;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class AdminAiFaqServiceImpl implements AdminAiFaqService {

    private static final Path STORAGE_PATH = Paths.get("data", "chatbot", "faqs.json");

    private final ObjectMapper objectMapper;

    public AdminAiFaqServiceImpl(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public synchronized List<AdminAiFaqItemDto> getFaqs() {
        ensureStorage();
        try {
            List<AdminAiFaqItemDto> items = objectMapper.readValue(
                    STORAGE_PATH.toFile(),
                    new TypeReference<List<AdminAiFaqItemDto>>() {}
            );
            items.sort(Comparator.comparingInt(AdminAiFaqItemDto::getSortOrder).thenComparing(AdminAiFaqItemDto::getId));
            return items;
        } catch (IOException e) {
            throw new RuntimeException("AI FAQ 파일을 읽는 중 문제가 생겼습니다.", e);
        }
    }

    @Override
    public synchronized List<AdminAiFaqItemDto> saveFaqs(List<AdminAiFaqItemDto> faqs) {
        ensureStorage();
        List<AdminAiFaqItemDto> sanitized = new ArrayList<>();
        AtomicLong sequence = new AtomicLong(1L);
        for (int i = 0; i < (faqs == null ? 0 : faqs.size()); i++) {
            AdminAiFaqItemDto item = faqs.get(i);
            if (item == null) {
                continue;
            }
            AdminAiFaqItemDto copy = new AdminAiFaqItemDto();
            copy.setId(item.getId() != null ? item.getId() : sequence.getAndIncrement());
            copy.setTitle(trim(item.getTitle()));
            copy.setQuestion(trim(item.getQuestion()));
            copy.setAnswer(trim(item.getAnswer()));
            copy.setKeywords(item.getKeywords() != null ? item.getKeywords() : new ArrayList<>());
            copy.setEnabled(item.isEnabled());
            copy.setSortOrder(i);
            if (copy.getQuestion() == null || copy.getQuestion().isBlank() || copy.getAnswer() == null || copy.getAnswer().isBlank()) {
                continue;
            }
            sanitized.add(copy);
            sequence.set(Math.max(sequence.get(), copy.getId() + 1));
        }

        try {
            Files.createDirectories(STORAGE_PATH.getParent());
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(STORAGE_PATH.toFile(), sanitized);
            return sanitized;
        } catch (IOException e) {
            throw new RuntimeException("AI FAQ 파일을 저장하는 중 문제가 생겼습니다.", e);
        }
    }

    private void ensureStorage() {
        try {
            if (Files.exists(STORAGE_PATH)) {
                return;
            }
            Files.createDirectories(STORAGE_PATH.getParent());
            ClassPathResource resource = new ClassPathResource("chatbot/default-faqs.json");
            if (resource.exists()) {
                try (InputStream is = resource.getInputStream()) {
                    Files.copy(is, STORAGE_PATH);
                    return;
                }
            }
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(STORAGE_PATH.toFile(), defaultFaqs());
        } catch (IOException e) {
            throw new RuntimeException("AI FAQ 저장소를 초기화하는 중 문제가 생겼습니다.", e);
        }
    }

    private List<AdminAiFaqItemDto> defaultFaqs() {
        List<AdminAiFaqItemDto> items = new ArrayList<>();
        items.add(create(1L, "행사 신청 안내", "행사는 어떻게 신청하나요?", "행사 상세 페이지에서 행사 상태가 '행사참여모집중'일 때 신청할 수 있어요.", List.of("신청", "모집", "참여"), 0));
        items.add(create(2L, "문의 확인 안내", "문의 내역은 어디서 보나요?", "행사 상세 페이지에서 문의를 남길 수 있고, 마이페이지에서 작성 문의와 받은 문의를 확인할 수 있어요.", List.of("문의", "내역", "마이페이지"), 1));
        items.add(create(3L, "관심 행사 안내", "관심 행사 목록은 어디서 확인하나요?", "관심 행사는 찜 기능으로 저장할 수 있고, 마이페이지에서 다시 확인할 수 있어요.", List.of("관심", "찜", "위시리스트"), 2));
        return items;
    }

    private AdminAiFaqItemDto create(Long id, String title, String question, String answer, List<String> keywords, int sortOrder) {
        AdminAiFaqItemDto item = new AdminAiFaqItemDto();
        item.setId(id);
        item.setTitle(title);
        item.setQuestion(question);
        item.setAnswer(answer);
        item.setKeywords(new ArrayList<>(keywords));
        item.setEnabled(true);
        item.setSortOrder(sortOrder);
        return item;
    }

    private String trim(String value) {
        return value == null ? null : value.trim();
    }
}
