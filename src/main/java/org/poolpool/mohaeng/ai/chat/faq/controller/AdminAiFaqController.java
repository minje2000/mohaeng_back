package org.poolpool.mohaeng.ai.chat.faq.controller;

import java.util.List;

import org.poolpool.mohaeng.ai.chat.faq.dto.AdminAiFaqItemDto;
import org.poolpool.mohaeng.ai.chat.faq.service.AdminAiFaqService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/admin/ai/faqs")
@RequiredArgsConstructor
public class AdminAiFaqController {

    private final AdminAiFaqService adminAiFaqService;

    @GetMapping
    public List<AdminAiFaqItemDto> getFaqs() {
        return adminAiFaqService.getFaqs();
    }

    @PutMapping
    public List<AdminAiFaqItemDto> saveFaqs(@RequestBody List<AdminAiFaqItemDto> faqs) {
        return adminAiFaqService.saveFaqs(faqs);
    }
}
