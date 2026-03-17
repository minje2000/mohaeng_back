package org.poolpool.mohaeng.ai.chat.contact.controller;

import lombok.RequiredArgsConstructor;
import org.poolpool.mohaeng.ai.chat.contact.dto.AdminContactRequestDto;
import org.poolpool.mohaeng.ai.chat.contact.dto.AdminContactResponseDto;
import org.poolpool.mohaeng.ai.chat.contact.service.AdminContactService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/ai/admin-contacts")
public class AdminContactController {

    private final AdminContactService adminContactService;

    @PostMapping
    public ResponseEntity<AdminContactResponseDto> create(
            @AuthenticationPrincipal String userId,
            @RequestBody AdminContactRequestDto request
    ) {
        return ResponseEntity.ok(adminContactService.create(request, userId));
    }

    @GetMapping("/me")
    public ResponseEntity<List<AdminContactResponseDto>> getMyContacts(
            @AuthenticationPrincipal String userId,
            @RequestParam(name = "limit", defaultValue = "100") Integer limit
    ) {
        return ResponseEntity.ok(adminContactService.listMine(userId, limit == null ? 100 : limit));
    }
}
