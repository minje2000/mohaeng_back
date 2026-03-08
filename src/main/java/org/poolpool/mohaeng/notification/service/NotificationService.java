package org.poolpool.mohaeng.notification.service;

import org.poolpool.mohaeng.common.api.PageResponse;
import org.poolpool.mohaeng.notification.dto.NotificationItemDto;
import org.springframework.data.domain.Pageable;

public interface NotificationService {

    PageResponse<NotificationItemDto> getList(long userId, Pageable pageable);

    long count(long userId);

    void read(long userId, long notificationId);   // 읽음 = 삭제

    void readAll(long userId);                     // 전체읽음 = 전체 삭제

    long create(long userId, long notiTypeId, Long eventId, Long reportId);

    // 폴링 스케줄러(부스/위시)에서 중복방지 키(status1)까지 넣고 싶을 때
    long createWithStatus(long userId, long notiTypeId, Long eventId, Long reportId, String status1, String status2);
}