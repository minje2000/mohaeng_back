package org.poolpool.mohaeng.ai.chat.faq.controller;

import java.util.List;

import org.poolpool.mohaeng.ai.chat.faq.dto.AdminAiFaqItemDto;
import org.poolpool.mohaeng.ai.chat.faq.service.AdminAiFaqService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/ai/faqs")
@RequiredArgsConstructor
public class PublicAiFaqController {

    private final AdminAiFaqService adminAiFaqService;

    @GetMapping("/public")
    public List<AdminAiFaqItemDto> getPublicFaqs() {
        return adminAiFaqService.getFaqs().stream()
                .filter(AdminAiFaqItemDto::isEnabled)
                .collect(Collectors.toList());
    }
}
