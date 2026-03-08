package org.poolpool.mohaeng.event.inquiry.service;

import org.poolpool.mohaeng.event.inquiry.dto.EventInquiryDto;
import org.poolpool.mohaeng.event.inquiry.dto.InquiryMypageResponse;

import java.util.List;

public interface EventInquiryService {

    List<EventInquiryDto> getInquiryList(Long eventId);

    Long createInquiry(Long currentUserId, Long eventId, EventInquiryDto dto);

    void updateInquiry(Long currentUserId, Long inqId, EventInquiryDto dto);

    void deleteInquiry(Long currentUserId, Long inqId);

    void createReply(Long currentUserId, Long inqId, EventInquiryDto dto);

    void updateReply(Long currentUserId, Long inqId, EventInquiryDto dto);

    void deleteReply(Long currentUserId, Long inqId);

    InquiryMypageResponse mypage(Long currentUserId, String tab, int page, int size);
}