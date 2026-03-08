package org.poolpool.mohaeng.event.host.service;

import java.util.List;

import org.poolpool.mohaeng.event.host.dto.EventCreateDto;
import org.poolpool.mohaeng.event.host.dto.HostEventMypageResponse;
import org.springframework.web.multipart.MultipartFile;

public interface EventHostService {
	Long createEventWithDetails(EventCreateDto createDto, Long hostId, MultipartFile thumbnail, List<MultipartFile> detailFiles, List<MultipartFile> boothFiles);
	void deleteEvent(Long eventId, Long currentUserId);
	HostEventMypageResponse myEvents(Long hostId, int page, int size);
	// 회원 탈퇴 시 주최 행사 중 행사종료/삭제되지 않은 행사 존재 유무 조회
	boolean hasActiveEvent(Long hostId);
}