package org.poolpool.mohaeng.ai.chat.faq.service;

import java.util.List;

import org.poolpool.mohaeng.ai.chat.faq.dto.AdminAiFaqItemDto;

public interface AdminAiFaqService {
    List<AdminAiFaqItemDto> getFaqs();
    List<AdminAiFaqItemDto> saveFaqs(List<AdminAiFaqItemDto> faqs);
}
