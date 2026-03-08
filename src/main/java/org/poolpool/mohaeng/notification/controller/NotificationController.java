// src/main/java/org/poolpool/mohaeng/notification/controller/NotificationController.java
package org.poolpool.mohaeng.notification.controller;

import org.poolpool.mohaeng.common.api.ApiResponse;
import org.poolpool.mohaeng.common.api.PageResponse;
import org.poolpool.mohaeng.notification.dto.NotificationItemDto;
import org.poolpool.mohaeng.notification.service.NotificationService;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<NotificationItemDto>>> list(
            @AuthenticationPrincipal String userId,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "5") int size,
            @RequestParam(name = "all", defaultValue = "false") boolean all
    ) {
        int finalPage = all ? 0 : page;

        // 무한대로 주면 위험하니까 상한
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

    // 읽음 = 삭제
    @DeleteMapping("/{notificationId}")
    public ResponseEntity<ApiResponse<Void>> read(
            @AuthenticationPrincipal String userId,
            @PathVariable("notificationId") long notificationId
    ) {
        notificationService.read(Long.valueOf(userId), notificationId);
        return ResponseEntity.ok(ApiResponse.ok("알림 읽음 처리 성공", null));
    }

    // 전체읽음 = 전체삭제
    @DeleteMapping
    public ResponseEntity<ApiResponse<Void>> readAll(
            @AuthenticationPrincipal String userId
    ) {
        notificationService.readAll(Long.valueOf(userId));
        return ResponseEntity.ok(ApiResponse.ok("전체 알림 읽음 처리 성공", null));
    }
}