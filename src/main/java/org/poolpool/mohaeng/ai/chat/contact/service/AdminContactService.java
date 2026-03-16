package org.poolpool.mohaeng.ai.chat.contact.service;

import org.poolpool.mohaeng.ai.chat.contact.dto.AdminContactRequestDto;
import org.poolpool.mohaeng.ai.chat.contact.dto.AdminContactResponseDto;

public interface AdminContactService {
    AdminContactResponseDto create(AdminContactRequestDto request);
}
