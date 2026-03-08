package org.poolpool.mohaeng.event.mypage.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.poolpool.mohaeng.event.list.dto.EventDto;

import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MyEventsResponse {
    private List<EventDto> items;
    private int page;          // 0-based
    private int size;
    private int totalPages;
    private long totalElements;
}
