package org.poolpool.mohaeng.event.host.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HostEventMypageResponse {
    private List<HostEventSummaryDto> items;
    private int page;
    private int size;
    private int totalPages;
    private long totalElements;
}
