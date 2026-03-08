// src/main/java/org/poolpool/mohaeng/event/wishlist/controller/WishlistController.java
package org.poolpool.mohaeng.event.wishlist.controller;

import org.poolpool.mohaeng.common.api.ApiResponse;
import org.poolpool.mohaeng.common.api.PageResponse;
import org.poolpool.mohaeng.event.wishlist.dto.WishlistCreateRequestDto;
import org.poolpool.mohaeng.event.wishlist.dto.WishlistItemDto;
import org.poolpool.mohaeng.event.wishlist.dto.WishlistToggleRequestDto;
import org.poolpool.mohaeng.event.wishlist.service.EventWishlistService;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class WishlistController {

    private final EventWishlistService wishlistService;

    public WishlistController(EventWishlistService wishlistService) {
        this.wishlistService = wishlistService;
    }

    // =========================
    // GET /api/user/wishlist
    // =========================
    @GetMapping("/user/wishlist")
    public ResponseEntity<ApiResponse<PageResponse<WishlistItemDto>>> list(
            @AuthenticationPrincipal String userId,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "10") int size
    ) {
        Pageable pageable = PageRequest.of(Math.max(page, 0), size);
        PageResponse<WishlistItemDto> data = wishlistService.getList(Long.valueOf(userId), pageable);
        return ResponseEntity.ok(ApiResponse.ok("관심행사 목록 조회 성공", data));
    }

    // =========================
    // POST /api/user/wishlist  body: { eventId }
    // =========================
    @PostMapping("/user/wishlist")
    public ResponseEntity<ApiResponse<Long>> create(
            @AuthenticationPrincipal String userId,
            @RequestBody WishlistCreateRequestDto request
    ) {
        long wishId = wishlistService.add(Long.valueOf(userId), request);
        return ResponseEntity.ok(ApiResponse.ok("관심행사 등록 완료", wishId));
    }

    // =========================
    // DELETE /api/user/wishlist/{wishId}
    // =========================
    @DeleteMapping("/user/wishlist/{wishId}")
    public ResponseEntity<ApiResponse<Object>> delete(
            @AuthenticationPrincipal String userId,
            @PathVariable("wishId") long wishId
    ) {
        wishlistService.remove(Long.valueOf(userId), wishId);
        return ResponseEntity.ok(ApiResponse.ok("관심행사 해제 완료", null));
    }

    // =========================
    // PUT /api/user/wishlist/{wishId}/notification  body: { notificationEnabled }
    // =========================
    @PutMapping("/user/wishlist/{wishId}/notification")
    public ResponseEntity<ApiResponse<WishlistItemDto>> toggleNotification(
            @AuthenticationPrincipal String userId,
            @PathVariable("wishId") long wishId,
            @RequestBody WishlistToggleRequestDto request
    ) {
        WishlistItemDto changed = wishlistService.toggleNotification(Long.valueOf(userId), wishId, request);
        return ResponseEntity.ok(ApiResponse.ok("알림 설정 변경 완료", changed));
    }
}