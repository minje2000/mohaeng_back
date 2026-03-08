package org.poolpool.mohaeng.event.review.controller;

import jakarta.validation.Valid;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Optional;

import org.poolpool.mohaeng.common.api.ApiResponse;
import org.poolpool.mohaeng.common.api.PageResponse;
import org.poolpool.mohaeng.event.review.dto.EventReviewTabItemDto;
import org.poolpool.mohaeng.event.review.dto.MyPageReviewItemDto;
import org.poolpool.mohaeng.event.review.dto.ReviewCreateRequestDto;
import org.poolpool.mohaeng.event.review.dto.ReviewEditRequestDto;
import org.poolpool.mohaeng.event.review.service.ReviewService;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@RestController
@RequestMapping("/api")
public class ReviewController {

    private final ReviewService reviewService;

    public ReviewController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    //  URL 유지: /api/users/{userId}/reviews
    //  userId는 헤더로 통일
    //  path userId는 검증용으로만 사용 (불일치 시 403을 "직접" 반환 -> GlobalExceptionHandler에 의해 500으로 안 바뀜)
    @GetMapping("/users/reviews")
    public ResponseEntity<ApiResponse<PageResponse<MyPageReviewItemDto>>> myList(
    		@AuthenticationPrincipal String userId,  // 토큰에서 추출된 ID(문자열)
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "10") int size
    ) {
    	long uid = Long.parseLong(userId);

        var pageable = PageRequest.of(page, size);
        var data = reviewService.selectMyList(uid, pageable);

        return ResponseEntity.ok(ApiResponse.ok("내 리뷰 목록 조회 성공", data));
    }

    @GetMapping("/events/{eventId}/reviews")
    public ResponseEntity<ApiResponse<Object>> eventList(
            Authentication auth,
            @PathVariable("eventId") long eventId,
            @RequestParam(name="page", defaultValue="0") int page,
            @RequestParam(name="size", defaultValue="10") int size
    ) {
        long uid = 0L; //  비로그인 기본값

        if (auth != null && auth.isAuthenticated() && !(auth instanceof AnonymousAuthenticationToken)) {
            Object principal = auth.getPrincipal();
            if (principal instanceof String s) {
                // principal이 "123" 같이 숫자일 때만 파싱
                if (s.matches("\\d+")) uid = Long.parseLong(s);
            }
            // principal이 커스텀 유저객체라면 여기서 uid 꺼내는 방식으로 확장 가능
        }

        var pageable = PageRequest.of(page, size);
        var data = reviewService.selectEventReviews(uid, eventId, pageable); //  서비스 시그니처 그대로 사용
        return ResponseEntity.ok(ApiResponse.ok("이벤트 리뷰 목록 조회 성공", data));
    }

    @GetMapping("/events/{eventId}/reviews/my")
    public ResponseEntity<ApiResponse<Object>> myReview(
            Authentication auth,
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable("eventId") long eventId
    ) {
        Long userId = extractUserId(auth, authorization);

        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.fail("로그인이 필요합니다.", null));
        }

        var data = reviewService.selectMyReviewForEvent(userId, eventId);
        return ResponseEntity.ok(ApiResponse.ok("내 리뷰 조회 성공", data));
    }

    private Long extractUserId(Authentication auth, String authorization) {
        // 1) Authentication에서 먼저 시도
        if (auth != null && auth.isAuthenticated() && !(auth instanceof AnonymousAuthenticationToken)) {
            Object principal = auth.getPrincipal();
            if (principal instanceof String s && s.matches("\\d+")) {
                return Long.parseLong(s);
            }
            // principal이 username/email이면 여기서는 못 뽑음 → 2)로 넘어감
        }

        // 2) Authorization: Bearer 토큰에서 payload 디코딩해서 userId/sub 시도 (컨트롤러만 수정하는 임시 해법)
        try {
            if (authorization == null || !authorization.startsWith("Bearer ")) return null;

            String token = authorization.substring(7).trim();
            String[] parts = token.split("\\.");
            if (parts.length < 2) return null;

            String payloadJson = new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8);
            JsonNode payload = new ObjectMapper().readTree(payloadJson);

            // 토큰에 userId 클레임이 있으면 그걸 쓰고, 없으면 sub 시도
            String uidStr = payload.has("userId") ? payload.get("userId").asText() : null;
            if (uidStr == null || uidStr.isBlank()) uidStr = payload.has("sub") ? payload.get("sub").asText() : null;

            if (uidStr != null && uidStr.matches("\\d+")) return Long.parseLong(uidStr);
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    @PostMapping("/reviews")
    public ResponseEntity<ApiResponse<Void>> create(
            Authentication auth,
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @Valid @RequestBody ReviewCreateRequestDto request
    ) {
        Long uid = extractUserId(auth, authorization);
        if (uid == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.fail("로그인이 필요합니다.", (Void) null));
        }

        reviewService.create(uid, request);
        return ResponseEntity.ok(ApiResponse.ok("리뷰 작성 성공", (Void) null));
    }

    @PutMapping("/reviews/{reviewId}")
    public ResponseEntity<ApiResponse<Void>> update(
            Authentication auth,
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable("reviewId") long reviewId,
            @Valid @RequestBody ReviewEditRequestDto request
    ) {
        Long uid = extractUserId(auth, authorization);
        if (uid == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.fail("로그인이 필요합니다.", (Void) null));
        }

        reviewService.edit(uid, reviewId, request);
        return ResponseEntity.ok(ApiResponse.ok("리뷰 수정 성공", (Void) null));
    }

    @DeleteMapping("/reviews/{reviewId}")
    public ResponseEntity<ApiResponse<Void>> delete(
            Authentication auth,
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable("reviewId") long reviewId
    ) {
        Long uid = extractUserId(auth, authorization);
        if (uid == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.fail("로그인이 필요합니다.", (Void) null));
        }

        reviewService.delete(uid, reviewId);
        return ResponseEntity.ok(ApiResponse.ok("리뷰 삭제 성공", (Void) null));
    }
}