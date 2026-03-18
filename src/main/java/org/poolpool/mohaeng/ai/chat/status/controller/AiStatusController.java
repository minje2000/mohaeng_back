package org.poolpool.mohaeng.ai.chat.status.controller;

import java.util.List;

import org.poolpool.mohaeng.ai.chat.status.dto.AiStatusItemDto;
import org.poolpool.mohaeng.ai.chat.status.service.AiStatusService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/ai/status")
@RequiredArgsConstructor
public class AiStatusController {

    private final AiStatusService aiStatusService;

    private Long currentUserId(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            throw new IllegalStateException("로그인이 필요합니다.");
        }
        return Long.valueOf(authentication.getName());
    }

    @GetMapping("/payments")
    public ResponseEntity<List<AiStatusItemDto>> getPayments(Authentication authentication) {
        return ResponseEntity.ok(aiStatusService.getPaymentStatuses(currentUserId(authentication)));
    }

    @GetMapping("/refunds")
    public ResponseEntity<List<AiStatusItemDto>> getRefunds(Authentication authentication) {
        return ResponseEntity.ok(aiStatusService.getRefundStatuses(currentUserId(authentication)));
    }

    @GetMapping("/booths")
    public ResponseEntity<List<AiStatusItemDto>> getBooths(Authentication authentication) {
        return ResponseEntity.ok(aiStatusService.getBoothStatuses(currentUserId(authentication)));
    }
}
