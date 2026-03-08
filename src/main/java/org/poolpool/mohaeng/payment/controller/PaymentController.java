package org.poolpool.mohaeng.payment.controller;

import lombok.RequiredArgsConstructor;
import org.poolpool.mohaeng.payment.dto.PaymentDto;
import org.poolpool.mohaeng.payment.service.PaymentService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payment")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    // 결제 준비 - orderId 발급
    @PostMapping("/prepare")
    public ResponseEntity<PaymentDto.PrepareResponse> prepare(
            @RequestBody PaymentDto.PrepareRequest request) {
        Long userId = getCurrentUserId();
        return ResponseEntity.ok(paymentService.prepare(userId, request));
    }

    // 결제 승인 - 토스 콜백 후 최종 승인
    @PostMapping("/confirm")
    public ResponseEntity<PaymentDto.ConfirmResponse> confirm(
            @RequestBody PaymentDto.ConfirmRequest request) {
        Long userId = getCurrentUserId();
        return ResponseEntity.ok(paymentService.confirm(userId, request));
    }

    private Long getCurrentUserId() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof String) {
            return Long.parseLong((String) principal);
        }
        throw new IllegalStateException("인증 정보를 찾을 수 없습니다.");
    }
}
