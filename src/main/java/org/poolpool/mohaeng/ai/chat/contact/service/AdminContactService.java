package org.poolpool.mohaeng.ai.chat.contact.service;

import org.poolpool.mohaeng.ai.chat.contact.dto.AdminContactRequestDto;
import org.poolpool.mohaeng.ai.chat.contact.dto.AdminContactResponseDto;

import java.util.List;
import java.util.Map;

public interface AdminContactService {
    AdminContactResponseDto create(AdminContactRequestDto request, String userId);
    List<AdminContactResponseDto> listForAdmin(int limit);
    AdminContactResponseDto update(String itemId, Map<String, Object> payload);
    AdminContactResponseDto delete(String itemId);
    List<AdminContactResponseDto> listMine(String userId, int limit);
}
