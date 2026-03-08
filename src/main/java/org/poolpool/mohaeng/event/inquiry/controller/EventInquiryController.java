package org.poolpool.mohaeng.event.inquiry.controller;

import lombok.RequiredArgsConstructor;
import org.poolpool.mohaeng.auth.security.principal.CustomUserPrincipal;
import org.poolpool.mohaeng.event.inquiry.dto.EventInquiryDto;
import org.poolpool.mohaeng.event.inquiry.dto.InquiryMypageResponse;
import org.poolpool.mohaeng.event.inquiry.service.EventInquiryService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/eventInquiry")
public class EventInquiryController {

    private final EventInquiryService service;

    /**
     * ✅ 현재 로그인 사용자 ID 추출
     * - @AuthenticationPrincipal이 null로 들어오는 환경에서도 동작하도록 SecurityContext에서 직접 꺼냄
     * - principal 타입이 프로젝트 설정마다 다를 수 있어서 여러 케이스를 방어
     */
    private Long currentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth == null || !auth.isAuthenticated()) {
            throw new IllegalStateException("로그인이 필요합니다.");
        }

        Object principal = auth.getPrincipal();
        if (principal == null) {
            throw new IllegalStateException("인증 principal이 없습니다.");
        }

        // 1) 너희가 의도한 타입
        if (principal instanceof CustomUserPrincipal cup) {
            if (cup.getUserId() == null) {
                throw new IllegalStateException("principal userId가 null 입니다.");
            }
            return Long.valueOf(cup.getUserId());
        }

        // 2) Spring UserDetails로 들어오는 경우 (username에 userId 또는 email이 들어갈 수 있음)
        if (principal instanceof UserDetails ud) {
            String username = ud.getUsername();
            // username이 숫자(userId)로 들어오는 케이스 대응
            try {
                return Long.parseLong(username);
            } catch (NumberFormatException ignored) {
                // username이 email이면 여기서 userId로 변환해야 함(Repository 필요)
                throw new IllegalStateException("principal이 UserDetails(username=" + username + ") 입니다. email->userId 변환 로직이 필요합니다.");
            }
        }

        // 3) principal이 String으로 들어오는 경우 (userId 문자열 or email)
        if (principal instanceof String s) {
            try {
                return Long.parseLong(s);
            } catch (NumberFormatException ignored) {
                throw new IllegalStateException("principal이 String(" + s + ") 입니다. email->userId 변환 로직이 필요합니다.");
            }
        }

        throw new IllegalStateException("지원하지 않는 principal 타입: " + principal.getClass().getName());
    }

    // ✅ 문의 목록 조회 (모든 유저 공개)
    @GetMapping("/list")
    public ResponseEntity<List<EventInquiryDto>> getInquiryList(@RequestParam("eventId") Long eventId) {
        return ResponseEntity.ok(service.getInquiryList(eventId));
    }

    // ✅ 문의 등록 (로그인 필요)
    @PostMapping("/createInquiry")
    public ResponseEntity<Long> createInquiry(
            @RequestParam("eventId") Long eventId,
            @RequestBody EventInquiryDto dto
    ) {
        Long uid = currentUserId();
        return ResponseEntity.ok(service.createInquiry(uid, eventId, dto));
    }

    // ✅ 문의 수정 (본인만)
    @PutMapping("/updateInquiry")
    public ResponseEntity<Void> updateInquiry(
            @RequestParam("inqId") Long inqId,
            @RequestBody EventInquiryDto dto
    ) {
        Long uid = currentUserId();
        service.updateInquiry(uid, inqId, dto);
        return ResponseEntity.ok().build();
    }

    // ✅ 문의 삭제 (본인만)
    @DeleteMapping("/deleteInquiry")
    public ResponseEntity<Void> deleteInquiry(@RequestParam("inqId") Long inqId) {
        Long uid = currentUserId();
        service.deleteInquiry(uid, inqId);
        return ResponseEntity.ok().build();
    }

    // ✅ 답변 등록 (주최자만)
    @PostMapping("/createReply")
    public ResponseEntity<Void> createReply(
            @RequestParam("inqId") Long inqId,
            @RequestBody EventInquiryDto dto
    ) {
        Long uid = currentUserId();
        service.createReply(uid, inqId, dto);
        return ResponseEntity.ok().build();
    }

    // ✅ 답변 수정 (주최자만)
    @PutMapping("/updateReply")
    public ResponseEntity<Void> updateReply(
            @RequestParam("inqId") Long inqId,
            @RequestBody EventInquiryDto dto
    ) {
        Long uid = currentUserId();
        service.updateReply(uid, inqId, dto);
        return ResponseEntity.ok().build();
    }

    // ✅ 답변 삭제 (주최자만)
    @DeleteMapping("/deleteReply")
    public ResponseEntity<Void> deleteReply(@RequestParam("inqId") Long inqId) {
        Long uid = currentUserId();
        service.deleteReply(uid, inqId);
        return ResponseEntity.ok().build();
    }

    // ✅ 마이페이지 문의 목록(전체/작성/받은) - 로그인 필요
    @GetMapping("/mypage")
    public ResponseEntity<InquiryMypageResponse> mypage(
            @RequestParam(value = "tab", defaultValue = "ALL") String tab,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "5") int size
    ) {
        Long uid = currentUserId();
        return ResponseEntity.ok(service.mypage(uid, tab, page, size));
    }
}