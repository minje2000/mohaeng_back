package org.poolpool.mohaeng.notification.controller;

import jakarta.servlet.http.HttpServletResponse;

import org.poolpool.mohaeng.auth.token.jwt.JwtTokenProvider;
import org.poolpool.mohaeng.common.api.ApiResponse;
import org.poolpool.mohaeng.common.api.PageResponse;
import org.poolpool.mohaeng.notification.dto.NotificationItemDto;
import org.poolpool.mohaeng.notification.service.NotificationService;
import org.poolpool.mohaeng.notification.service.NotificationSseService;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;
    private final NotificationSseService notificationSseService;
    private final JwtTokenProvider jwtTokenProvider;

    @GetMapping(value = "/subscribe", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribe(
            @AuthenticationPrincipal String userId,
            @RequestParam(name = "accessToken", required = false) String accessToken,
            HttpServletResponse response
    ) {
        response.setHeader("Cache-Control", "no-cache");
        response.setHeader("X-Accel-Buffering", "no");

        Long loginUserId;

        if (userId != null && !userId.isBlank()) {
            loginUserId = Long.valueOf(userId);
        } else {
            String token = accessToken;

            if (token == null || token.isBlank()) {
                throw new IllegalArgumentException("accessToken이 필요합니다.");
            }

            if (token.startsWith("Bearer ")) {
                token = token.substring(7);
            }

            String subject = jwtTokenProvider.getUserId(token);
            loginUserId = Long.valueOf(subject);
        }

        return notificationSseService.subscribe(loginUserId);
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<NotificationItemDto>>> list(
            @AuthenticationPrincipal String userId,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "5") int size,
            @RequestParam(name = "all", defaultValue = "false") boolean all
    ) {
        int finalPage = all ? 0 : page;
        int finalSize = all ? 2000 : Math.min(size, 200);

        var data = notificationService.getList(Long.valueOf(userId), PageRequest.of(finalPage, finalSize));
        return ResponseEntity.ok(ApiResponse.ok("내 알림 목록 조회 성공", data));
    }

    @GetMapping("/count")
    public ResponseEntity<ApiResponse<Long>> count(
            @AuthenticationPrincipal String userId
    ) {
        long cnt = notificationService.count(Long.valueOf(userId));
        return ResponseEntity.ok(ApiResponse.ok("알림 개수 조회 성공", cnt));
    }

    @DeleteMapping("/{notificationId}")
    public ResponseEntity<ApiResponse<Void>> read(
            @AuthenticationPrincipal String userId,
            @PathVariable("notificationId") long notificationId
    ) {
        notificationService.read(Long.valueOf(userId), notificationId);
        return ResponseEntity.ok(ApiResponse.ok("알림 읽음 처리 성공", null));
    }

    @DeleteMapping
    public ResponseEntity<ApiResponse<Void>> readAll(
            @AuthenticationPrincipal String userId
    ) {
        notificationService.readAll(Long.valueOf(userId));
        return ResponseEntity.ok(ApiResponse.ok("전체 알림 읽음 처리 성공", null));
    }
}