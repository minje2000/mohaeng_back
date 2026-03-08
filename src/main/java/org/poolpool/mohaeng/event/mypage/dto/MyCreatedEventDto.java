package org.poolpool.mohaeng.event.mypage.dto;

import java.time.LocalDate;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 마이페이지 - 내가 등록(주최)한 행사 목록 아이템 DTO
 * - 프론트가 쓰는 필드명(eventId/title/thumbnail/startDate/endDate/eventStatus)을 유지한다.
 * - eventStatus는 DB값이 stale 할 수 있어 startDate/endDate로 보정한 "실제 상태"를 내려준다.
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
    private String eventStatus;   // ✅ 보정된 상태(표시/삭제 조건 기준)
    private boolean deletable;    // ✅ 삭제 버튼 노출 기준
}
