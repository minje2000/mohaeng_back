package org.poolpool.mohaeng.event.mypage.controller;

import lombok.RequiredArgsConstructor;
import org.poolpool.mohaeng.auth.security.principal.CustomUserPrincipal;
import org.poolpool.mohaeng.event.mypage.dto.MyEventsResponse;
import org.poolpool.mohaeng.event.mypage.service.MypageEventService;
import org.poolpool.mohaeng.event.participation.dto.EventParticipationDto;
import org.poolpool.mohaeng.event.participation.service.EventParticipationService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/mypage/events")
public class MypageEventController {

    private final MypageEventService mypageEventService;
    private final EventParticipationService participationService;

    /**
     * ✅ 현재 로그인 사용자 ID 추출
     * - 문의 컨트롤러와 동일하게 SecurityContext에서 직접 꺼내서 principal null 이슈를 방어
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

        if (principal instanceof CustomUserPrincipal cup) {
            if (cup.getUserId() == null) {
                throw new IllegalStateException("principal userId가 null 입니다.");
            }
            return Long.valueOf(cup.getUserId());
        }

        if (principal instanceof UserDetails ud) {
            String username = ud.getUsername();
            try {
                return Long.parseLong(username);
            } catch (NumberFormatException ignored) {
                throw new IllegalStateException(
                        "principal이 UserDetails(username=" + username + ") 입니다. email->userId 변환 로직이 필요합니다.");
            }
        }

        if (principal instanceof String s) {
            try {
                return Long.parseLong(s);
            } catch (NumberFormatException ignored) {
                throw new IllegalStateException("principal이 String(" + s + ") 입니다. email->userId 변환 로직이 필요합니다.");
            }
        }

        throw new IllegalStateException("지원하지 않는 principal 타입: " + principal.getClass().getName());
    }

    /**
     * ✅ 마이페이지 - 행사 등록(주최) 내역
     */
    @GetMapping("/created")
    public ResponseEntity<MyEventsResponse> myCreatedEvents(
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "5") int size
    ) {
        Long uid = currentUserId();
        return ResponseEntity.ok(mypageEventService.getMyCreatedEvents(uid, page, size));
    }

    
    /**
     * ✅ 마이페이지 - 행사 등록 내역 소프트 삭제
     * - 행사예정/행사종료(날짜 기준)만 삭제 허용
     * - 삭제 시 DB eventStatus = 'DELETED'
     */
    @PutMapping("/created/{eventId}/delete")
    public ResponseEntity<Void> deleteMyCreatedEvent(@PathVariable("eventId") Long eventId) {
        Long uid = currentUserId();
        mypageEventService.deleteMyCreatedEvent(uid, eventId);
        return ResponseEntity.ok().build();
    }
/**
     * ✅ 마이페이지 - 행사 참여 내역(부스 제외)
     * - 부스는 별도 탭(부스 관리)에서 노출한다고 해서 여기서는 제외
     */
    @GetMapping("/participations")
    public ResponseEntity<List<EventParticipationDto>> myParticipations() {
        Long uid = currentUserId();
        return ResponseEntity.ok(participationService.getParticipationList(uid));
    }
}
