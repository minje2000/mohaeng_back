package org.poolpool.mohaeng.event.wishlist.dto;

import org.poolpool.mohaeng.event.list.entity.EventEntity;
import org.poolpool.mohaeng.event.wishlist.entity.EventWishlistEntity;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class WishlistItemDto {
    private long wishId;
    private long eventId;
    private String eventTitle;
    private String eventThumbnail;
    private boolean notificationEnabled;
    private LocalDateTime createdAt;
    private String eventStatus; // ✅ 추가: DELETED / REPORTDELETED 판별용
    private String eventSimpleExplain;
    private LocalDate startDate;
    private LocalDate endDate;

    public static WishlistItemDto fromEntity(EventWishlistEntity w, EventEntity e) {
        WishlistItemDto dto = new WishlistItemDto();
        dto.wishId = w.getWishId();
        dto.eventId = w.getEventId();
        dto.notificationEnabled = w.isNotificationEnabled();
        dto.createdAt = w.getCreatedAt();
        dto.eventTitle = (e != null) ? e.getTitle() : "(삭제/미존재 이벤트)";
        dto.eventThumbnail = (e != null) ? e.getThumbnail() : null;
        dto.eventStatus = (e != null) ? e.getEventStatus() : null; // ✅ 추가
        dto.eventSimpleExplain = (e != null) ? e.getSimpleExplain() : null;
        dto.startDate = (e != null) ? e.getStartDate() : null;
        dto.endDate = (e != null) ? e.getEndDate() : null;
        return dto;
    }

    // getter
    public long getWishId() { return wishId; }
    public long getEventId() { return eventId; }
    public String getEventTitle() { return eventTitle; }
    public String getEventThumbnail() { return eventThumbnail; }
    public boolean isNotificationEnabled() { return notificationEnabled; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public String getEventStatus() { return eventStatus; } // ✅ 추가
    public String getEventSimpleExplain() { return eventSimpleExplain; }
    public LocalDate getStartDate() { return startDate; }
    public LocalDate getEndDate() { return endDate; }
}
