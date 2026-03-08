package org.poolpool.mohaeng.event.host.controller;

import java.util.List;

import org.poolpool.mohaeng.event.host.dto.EventCreateDto;
import org.poolpool.mohaeng.event.host.dto.HostEventMypageResponse;
import org.poolpool.mohaeng.event.host.service.EventHostService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal; // 💡 중요!
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/events")
@RequiredArgsConstructor
public class EventHostController {

    private final EventHostService eventHostService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Long> createEvent(
            @RequestPart("eventData") EventCreateDto createDto,
            // 💡 CustomUserPrincipal 대신 String으로 직접 받습니다!
            // 현재 필터가 String(userId)을 넣어주고 있기 때문에 이렇게 하면 null이 안 나옵니다.
            @AuthenticationPrincipal String userId, 
            @RequestPart(value = "thumbnail", required = false) MultipartFile thumbnail,
            @RequestPart(value = "detailFiles", required = false) List<MultipartFile> detailFiles,
            @RequestPart(value = "boothFiles", required = false) List<MultipartFile> boothFiles
    ) {
        // 💡 이미 userId가 "1" 같은 문자열로 들어왔으니 바로 Long으로 변환만 하면 됩니다.
        if (userId == null) {
            throw new RuntimeException("로그인 정보가 없습니다. (토큰 확인 필요)");
        }
        
        Long hostId = Long.parseLong(userId); 
        
        Long newEventId = eventHostService.createEventWithDetails(createDto, hostId, thumbnail, detailFiles, boothFiles);
        
        return ResponseEntity.ok(newEventId);
    }
    
    @PutMapping("/{eventId}")
    public ResponseEntity<String> deleteEvent(
            @PathVariable("eventId") Long eventId,
            @AuthenticationPrincipal String userId // 💡 토큰에서 추출한 ID
    ) {
        if (userId == null) {
            throw new RuntimeException("로그인이 필요합니다.");
        }
        
        Long currentUserId = Long.parseLong(userId);
        
        // 서비스 호출 시 현재 유저 ID를 같이 넘깁니다.
        eventHostService.deleteEvent(eventId, currentUserId);
        
        return ResponseEntity.ok("행사가 성공적으로 삭제(상태 변경)되었습니다.");
    }

    // ✅ 마이페이지 - 내가 등록한 행사 목록(토큰 기반)
    @GetMapping("/mine")
    public ResponseEntity<HostEventMypageResponse> myEvents(
            @AuthenticationPrincipal String userId,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "5") int size
    ) {
        if (userId == null) {
            throw new RuntimeException("로그인이 필요합니다.");
        }
        Long hostId = Long.parseLong(userId);
        return ResponseEntity.ok(eventHostService.myEvents(hostId, page, size));
    }
}