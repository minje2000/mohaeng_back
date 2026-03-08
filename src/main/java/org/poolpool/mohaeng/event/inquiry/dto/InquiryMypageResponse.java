package org.poolpool.mohaeng.event.inquiry.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InquiryMypageResponse {
    private List<EventInquiryDto> items;
    private int page;          // 0-based
    private int size;
    private int totalPages;
    private long totalElements;
}