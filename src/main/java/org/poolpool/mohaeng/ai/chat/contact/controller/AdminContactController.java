package org.poolpool.mohaeng.ai.chat.contact.controller;

import lombok.RequiredArgsConstructor;
import org.poolpool.mohaeng.ai.chat.contact.dto.AdminContactRequestDto;
import org.poolpool.mohaeng.ai.chat.contact.dto.AdminContactResponseDto;
import org.poolpool.mohaeng.ai.chat.contact.service.AdminContactService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/ai/admin-contacts")
public class AdminContactController {

    private final AdminContactService adminContactService;

    @PostMapping
    public ResponseEntity<AdminContactResponseDto> create(@RequestBody AdminContactRequestDto request) {
        return ResponseEntity.ok(adminContactService.create(request));
    }
}
