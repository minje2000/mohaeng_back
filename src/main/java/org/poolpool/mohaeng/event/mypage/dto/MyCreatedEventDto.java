package org.poolpool.mohaeng.event.mypage.dto;

import java.time.LocalDate;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 마이페이지 - 내가 등록(주최)한 행사 목록 아이템 DTO
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MyCreatedEventDto {
    private Long eventId;
    private String title;
    private String thumbnail;
    private LocalDate startDate;
    private LocalDate endDate;
    private String eventStatus;
    private String moderationStatus;   // 추가
    private boolean deletable;
}