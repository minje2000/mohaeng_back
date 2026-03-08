package org.poolpool.mohaeng.event.host.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.poolpool.mohaeng.event.list.entity.EventEntity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HostEventSummaryDto {
    private Long eventId;
    private String title;
    private LocalDate startDate;
    private LocalDate endDate;
    private String eventStatus;
    private String thumbnail;
    private LocalDateTime createdAt;

    public static HostEventSummaryDto fromEntity(EventEntity e) {
        if (e == null) return null;
        return HostEventSummaryDto.builder()
                .eventId(e.getEventId())
                .title(e.getTitle())
                .startDate(e.getStartDate())
                .endDate(e.getEndDate())
                .eventStatus(e.getEventStatus())
                .thumbnail(e.getThumbnail())
                .createdAt(e.getCreatedAt())
                .build();
    }
}
